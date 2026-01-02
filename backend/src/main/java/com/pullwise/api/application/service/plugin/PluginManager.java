package com.pullwise.api.application.service.plugin;

import com.pullwise.api.application.service.plugin.api.AnalysisRequest;
import com.pullwise.api.application.service.plugin.api.AnalysisResult;
import com.pullwise.api.application.service.plugin.api.CodeReviewPlugin;
import com.pullwise.api.application.service.plugin.api.PluginContext;
import com.pullwise.api.application.service.plugin.api.PluginException;
import com.pullwise.api.application.service.plugin.api.PluginLanguage;
import com.pullwise.api.application.service.plugin.api.PluginMetadata;
import com.pullwise.api.application.service.plugin.api.PluginType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Gerenciador central de plugins.
 *
 * <p>Responsabilidades:
 * - Descobrir plugins (Java via SPI, externos via diretório)
 * - Inicializar plugins com contexto apropriado
 * - Executar plugins com timeout
 * - Gerenciar ciclo de vida dos plugins
 * - Fornecer métricas de execução
 */
@Slf4j
@Service
public class PluginManager {

    /**
     * Plugins carregados e prontos para uso.
     * Key: plugin ID
     */
    private final Map<String, LoadedPlugin> loadedPlugins = new ConcurrentHashMap<>();

    /**
     * Plugins por tipo.
     */
    private final Map<PluginType, Set<String>> pluginsByType = new ConcurrentHashMap<>();

    /**
     * Plugins por linguagem.
     */
    private final Map<PluginLanguage, Set<String>> pluginsByLanguage = new ConcurrentHashMap<>();

    /**
     * Executor para execução paralela de plugins.
     */
    private final ExecutorService executor;

    /**
     * Diretório base de plugins externos.
     */
    private final Path pluginsDirectory;

    public PluginManager() {
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "plugin-executor");
            t.setDaemon(true);
            return t;
        });
        this.pluginsDirectory = Path.of("/opt/pullwise/plugins");

        // Inicializar diretório de plugins
        try {
            if (!Files.exists(pluginsDirectory)) {
                Files.createDirectories(pluginsDirectory);
            }
        } catch (Exception e) {
            log.warn("Failed to create plugins directory: {}", e.getMessage());
        }

        // Carregar plugins via SPI
        loadJavaPlugins();
    }

    // ========== Discovery ==========

    /**
     * Carrega plugins Java via SPI (ServiceLoader).
     */
    private void loadJavaPlugins() {
        log.info("Loading Java plugins via SPI...");

        ServiceLoader<CodeReviewPlugin> loader = ServiceLoader.load(CodeReviewPlugin.class);
        int loaded = 0;

        for (CodeReviewPlugin plugin : loader) {
            try {
                registerPlugin(plugin, false);
                loaded++;
            } catch (Exception e) {
                log.error("Failed to register plugin: {}", plugin.getId(), e);
            }
        }

        log.info("Loaded {} Java plugins", loaded);
    }

    /**
     * Registra um plugin no gerenciador.
     *
     * @param plugin     Plugin a registrar
     * @param external   Se é um plugin externo
     */
    public void registerPlugin(CodeReviewPlugin plugin, boolean external) {
        String pluginId = plugin.getId();

        if (loadedPlugins.containsKey(pluginId)) {
            log.warn("Plugin {} already registered, skipping", pluginId);
            return;
        }

        log.info("Registering plugin: {} v{} by {}",
                plugin.getName(), plugin.getVersion(), plugin.getAuthor());

        // Criar contexto do plugin
        PluginContext context = createContext(plugin, external);

        LoadedPlugin loaded = new LoadedPlugin(plugin, context);
        loadedPlugins.put(pluginId, loaded);

        // Indexar por tipo
        PluginType type = plugin.getType();
        pluginsByType.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add(pluginId);

        // Indexar por linguagem
        for (PluginLanguage lang : plugin.getSupportedLanguages()) {
            pluginsByLanguage.computeIfAbsent(lang, k -> ConcurrentHashMap.newKeySet()).add(pluginId);
        }

        // Inicializar plugin
        try {
            plugin.initialize(context);
            log.info("Plugin {} initialized successfully", pluginId);
        } catch (PluginException e) {
            log.error("Failed to initialize plugin {}: {}", pluginId, e.getMessage(), e);
            loaded.setInitializationError(e);
        }
    }

    /**
     * Cria o contexto para um plugin.
     */
    private PluginContext createContext(CodeReviewPlugin plugin, boolean external) {
        Path pluginDir = external
                ? pluginsDirectory.resolve(plugin.getId())
                : pluginsDirectory.resolve("bundled").resolve(plugin.getId());

        try {
            Files.createDirectories(pluginDir);
        } catch (Exception e) {
            log.warn("Failed to create plugin directory: {}", e.getMessage());
        }

        return PluginContext.builder()
                .dataDirectory(pluginDir.resolve("data"))
                .workingDirectory(pluginDir)
                .configuration(new HashMap<>())
                .developmentMode(false)
                .build();
    }

    // ========== Execution ==========

    /**
     * Executa todos os plugins habilitados de um tipo específico.
     *
     * @param type    Tipo de plugin
     * @param request Request de análise
     * @return Lista de resultados de todos os plugins
     */
    public List<AnalysisResult> executeByType(PluginType type, AnalysisRequest request) {
        Set<String> pluginIds = pluginsByType.getOrDefault(type, Set.of());
        return executePlugins(pluginIds, request);
    }

    /**
     * Executa todos os plugins que suportam uma linguagem.
     *
     * @param language Linguagem do código
     * @param request  Request de análise
     * @return Lista de resultados de todos os plugins
     */
    public List<AnalysisResult> executeByLanguage(PluginLanguage language, AnalysisRequest request) {
        // Incluir plugins que suportam ALL
        Set<String> pluginIds = new HashSet<>();

        for (Map.Entry<PluginLanguage, Set<String>> entry : pluginsByLanguage.entrySet()) {
            if (entry.getKey() == PluginLanguage.ALL || entry.getKey() == language) {
                pluginIds.addAll(entry.getValue());
            }
        }

        return executePlugins(pluginIds, request);
    }

    /**
     * Executa uma lista específica de plugins.
     *
     * @param pluginIds IDs dos plugins a executar
     * @param request   Request de análise
     * @return Lista de resultados
     */
    public List<AnalysisResult> executePlugins(Set<String> pluginIds, AnalysisRequest request) {
        if (pluginIds.isEmpty()) {
            return List.of();
        }

        log.debug("Executing {} plugins for request", pluginIds.size());

        // Executar plugins em paralelo
        List<CompletableFuture<AnalysisResult>> futures = new ArrayList<>();

        for (String pluginId : pluginIds) {
            LoadedPlugin loaded = loadedPlugins.get(pluginId);
            if (loaded == null || !loaded.isReady()) {
                log.debug("Skipping plugin {} (not loaded or failed to initialize)", pluginId);
                continue;
            }

            if (!loaded.plugin().isEnabled()) {
                log.debug("Skipping disabled plugin {}", pluginId);
                continue;
            }

            CompletableFuture<AnalysisResult> future = CompletableFuture.supplyAsync(
                    () -> executePlugin(loaded, request),
                    executor
            ).exceptionally(ex -> {
                log.error("Plugin {} execution failed: {}", pluginId, ex.getMessage());
                return AnalysisResult.error(pluginId, ex.getMessage());
            });

            futures.add(future);
        }

        // Aguardar todos os resultados
        List<AnalysisResult> results = futures.stream()
                .map(CompletableFuture::join)
                .filter(r -> r != null)
                .collect(Collectors.toList());

        log.debug("Plugin execution completed: {} results", results.size());

        return results;
    }

    /**
     * Executa um único plugin com timeout.
     */
    private AnalysisResult executePlugin(LoadedPlugin loaded, AnalysisRequest request) {
        String pluginId = loaded.plugin().getId();
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Executing plugin: {}", pluginId);

            // Executar com timeout
            int timeout = request.getTimeoutSeconds() > 0 ? request.getTimeoutSeconds() : 60;

            AnalysisResult result = CompletableFuture.supplyAsync(() -> {
                try {
                    return loaded.plugin().analyze(request);
                } catch (PluginException e) {
                    throw new RuntimeException(e);
                }
            }, executor).get(timeout, TimeUnit.SECONDS);

            // Adicionar pluginId se não estiver presente
            if (result.getPluginId() == null) {
                result.setPluginId(pluginId);
            }

            // Calcular tempo de execução
            long duration = System.currentTimeMillis() - startTime;
            result.setExecutionTime(java.time.Duration.ofMillis(duration));

            log.debug("Plugin {} completed in {}ms with {} issues",
                    pluginId, duration, result.getIssues() != null ? result.getIssues().size() : 0);

            return result;

        } catch (TimeoutException e) {
            log.warn("Plugin {} execution timed out after {}s", pluginId, request.getTimeoutSeconds());
            return AnalysisResult.error(pluginId, "Execution timeout");

        } catch (Exception e) {
            log.error("Plugin {} execution failed", pluginId, e);
            return AnalysisResult.error(pluginId, e.getMessage());
        }
    }

    /**
     * Executa um único plugin específico.
     *
     * @param pluginId ID do plugin
     * @param request  Request de análise
     * @return Resultado da análise
     */
    public AnalysisResult executePlugin(String pluginId, AnalysisRequest request) {
        LoadedPlugin loaded = loadedPlugins.get(pluginId);
        if (loaded == null) {
            return AnalysisResult.error(pluginId, "Plugin not found");
        }
        if (!loaded.isReady()) {
            return AnalysisResult.error(pluginId, "Plugin not initialized");
        }
        return executePlugin(loaded, request);
    }

    // ========== Query ==========

    /**
     * Retorna todos os plugins carregados.
     *
     * @return Mapa de plugins por ID
     */
    public Map<String, PluginMetadata> getAllPlugins() {
        Map<String, PluginMetadata> metadata = new HashMap<>();
        for (Map.Entry<String, LoadedPlugin> entry : loadedPlugins.entrySet()) {
            metadata.put(entry.getKey(), entry.getValue().plugin().getMetadata());
        }
        return metadata;
    }

    /**
     * Retorna plugins por tipo.
     *
     * @param type Tipo de plugin
     * @return Lista de metadados
     */
    public List<PluginMetadata> getPluginsByType(PluginType type) {
        Set<String> pluginIds = pluginsByType.getOrDefault(type, Set.of());
        return pluginIds.stream()
                .map(loadedPlugins::get)
                .filter(Objects::nonNull)
                .map(lp -> lp.plugin().getMetadata())
                .collect(Collectors.toList());
    }

    /**
     * Retorna um plugin específico por ID.
     *
     * @param pluginId ID do plugin
     * @return Plugin ou null se não encontrado
     */
    public CodeReviewPlugin getPlugin(String pluginId) {
        LoadedPlugin loaded = loadedPlugins.get(pluginId);
        return loaded != null ? loaded.plugin() : null;
    }

    /**
     * Retorna metadados de um plugin específico.
     *
     * @param pluginId ID do plugin
     * @return Metadados ou null se não encontrado
     */
    public PluginMetadata getPluginMetadata(String pluginId) {
        CodeReviewPlugin plugin = getPlugin(pluginId);
        return plugin != null ? plugin.getMetadata() : null;
    }

    /**
     * Verifica se um plugin existe e está pronto.
     *
     * @param pluginId ID do plugin
     * @return true se o plugin existe e está pronto
     */
    public boolean isPluginReady(String pluginId) {
        LoadedPlugin loaded = loadedPlugins.get(pluginId);
        return loaded != null && loaded.isReady();
    }

    /**
     * Retorna o número de plugins carregados.
     *
     * @return Número de plugins
     */
    public int getPluginCount() {
        return loadedPlugins.size();
    }

    /**
     * Retorna estatísticas dos plugins.
     *
     * @return Mapa com estatísticas
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlugins", loadedPlugins.size());
        stats.put("enabledPlugins", (int) loadedPlugins.values().stream()
                .filter(lp -> lp.plugin().isEnabled()).count());
        stats.put("byType", pluginsByType.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> e.getValue().size()
                )));
        return stats;
    }

    // ========== Lifecycle ==========

    /**
     * Desliga todos os plugins e libera recursos.
     */
    public void shutdown() {
        log.info("Shutting down PluginManager...");

        for (LoadedPlugin loaded : loadedPlugins.values()) {
            try {
                loaded.plugin().shutdown();
            } catch (Exception e) {
                log.error("Error shutting down plugin {}", loaded.plugin().getId(), e);
            }
        }

        loadedPlugins.clear();
        pluginsByType.clear();
        pluginsByLanguage.clear();

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("PluginManager shutdown complete");
    }

    // ========== Inner Classes ==========

    /**
     * Representa um plugin carregado.
     */
    private static class LoadedPlugin {
        private final CodeReviewPlugin plugin;
        private final PluginContext context;
        private PluginException initializationError;

        public LoadedPlugin(CodeReviewPlugin plugin, PluginContext context) {
            this.plugin = plugin;
            this.context = context;
        }

        public CodeReviewPlugin plugin() {
            return plugin;
        }

        public PluginContext context() {
            return context;
        }

        public boolean isReady() {
            return initializationError == null;
        }

        public void setInitializationError(PluginException error) {
            this.initializationError = error;
        }
    }
}
