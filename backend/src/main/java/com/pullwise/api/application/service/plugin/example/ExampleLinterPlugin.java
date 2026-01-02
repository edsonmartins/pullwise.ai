package com.pullwise.api.application.service.plugin.example;

import com.pullwise.api.application.service.plugin.api.AnalysisRequest;
import com.pullwise.api.application.service.plugin.api.AnalysisResult;
import com.pullwise.api.application.service.plugin.api.PluginException;
import com.pullwise.api.application.service.plugin.api.PluginLanguage;
import com.pullwise.api.application.service.plugin.api.PluginType;
import com.pullwise.api.application.service.plugin.base.AbstractCodeReviewPlugin;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plugin de exemplo que detecta problemas comuns de código.
 *
 * <p>Este plugin demonstra:
 * - Uso da classe base AbstractCodeReviewPlugin
 * - Parse de diffs
 * - Detecção de issues
 * - Uso de configuração
 *
 * <p>Regras implementadas:
 * 1. Detecta TODOs sem tickets
 * 2. Detecta System.out.println
 * 3. Detecta métodos muito longos
 */
@Slf4j
public class ExampleLinterPlugin extends AbstractCodeReviewPlugin {

    private static final Pattern TODO_PATTERN = Pattern.compile("// TODO(?::|\\s)(?!#\\d+).*");
    private static final Pattern PRINTLN_PATTERN = Pattern.compile("System\\.out\\.println");

    private boolean enableTodoCheck;
    private boolean enablePrintlnCheck;
    private int maxMethodLength;

    @Override
    protected void doInitialize() throws PluginException {
        // Ler configurações
        this.enableTodoCheck = getConfigBoolean("enableTodoCheck", true);
        this.enablePrintlnCheck = getConfigBoolean("enablePrintlnCheck", true);
        this.maxMethodLength = getConfigInt("maxMethodLength", 50);

        log.info("ExampleLinterPlugin initialized with: todoCheck={}, printlnCheck={}, maxMethodLength={}",
                enableTodoCheck, enablePrintlnCheck, maxMethodLength);
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) throws PluginException {
        List<Issue> issues = new ArrayList<>();

        String diff = request.getDiff();
        if (diff == null || diff.isEmpty()) {
            return AnalysisResult.empty(getId());
        }

        // Analisar diff linha por linha
        String[] lines = diff.split("\n");
        String currentFile = null;
        int lineNumber = 0;

        for (String line : lines) {
            // Detectar arquivo (diff header)
            if (line.startsWith("+++ ") || line.startsWith("--- ")) {
                String[] parts = line.split("\\s+", 3);
                if (parts.length >= 3) {
                    currentFile = extractFilePath(parts[2]);
                }
                continue;
            }

            // Detectar linha adicionada
            if (line.startsWith("+") && !line.startsWith("+++")) {
                lineNumber++;

                String codeLine = line.substring(1);

                // Checar por TODOs
                if (enableTodoCheck) {
                    checkForTodos(currentFile, codeLine, lineNumber, issues);
                }

                // Checar por System.out.println
                if (enablePrintlnCheck) {
                    checkForPrintln(currentFile, codeLine, lineNumber, issues);
                }
            } else if (line.startsWith("@@")) {
                // Extrair número da linha do hunk
                lineNumber = extractLineNumber(line);
            }
        }

        // Checar tamanho dos métodos
        if (request.getFileContents() != null) {
            checkMethodLengths(request, issues);
        }

        return AnalysisResult.builder()
                .pluginId(getId())
                .issues(issues)
                .success(true)
                .build();
    }

    /**
     * Extrai o caminho do arquivo de um diff header.
     */
    private String extractFilePath(String path) {
        // Remove prefixos comuns de diff
        if (path.startsWith("a/")) {
            path = path.substring(2);
        } else if (path.startsWith("b/")) {
            path = path.substring(2);
        }
        return path;
    }

    /**
     * Extrai o número da linha de um hunk header.
     */
    private int extractLineNumber(String hunk) {
        // Ex: @@ -52,7 +52,9 @@
        Pattern pattern = Pattern.compile("\\+\\s*(\\d+)");
        Matcher matcher = pattern.matcher(hunk);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Checa por TODOs sem tickets.
     */
    private void checkForTodos(String file, String line, int lineNumber, List<Issue> issues) {
        Matcher matcher = TODO_PATTERN.matcher(line);
        if (matcher.find()) {
            Issue issue = Issue.builder()
                    .type(IssueType.CODE_SMELL)
                    .severity(Severity.LOW)
                    .title("TODO without ticket reference")
                    .description("TODO comments should reference a ticket (e.g., TODO: #123)")
                    .filePath(file != null ? file : "unknown")
                    .lineStart(lineNumber)
                    .lineEnd(lineNumber)
                    .ruleId("EXAMPLE_TODO_WITHOUT_TICKET")
                    .source(com.pullwise.api.domain.enums.IssueSource.LLM)
                    .createdAt(LocalDateTime.now())
                    .build();

            issues.add(issue);
        }
    }

    /**
     * Checa por System.out.println.
     */
    private void checkForPrintln(String file, String line, int lineNumber, List<Issue> issues) {
        Matcher matcher = PRINTLN_PATTERN.matcher(line);
        if (matcher.find()) {
            Issue issue = Issue.builder()
                    .type(IssueType.CODE_SMELL)
                    .severity(Severity.LOW)
                    .title("System.out.println found")
                    .description("Use a proper logging framework instead of System.out.println")
                    .filePath(file != null ? file : "unknown")
                    .lineStart(lineNumber)
                    .lineEnd(lineNumber)
                    .ruleId("EXAMPLE_SYSTEM_OUT")
                    .source(com.pullwise.api.domain.enums.IssueSource.LLM)
                    .createdAt(LocalDateTime.now())
                    .build();

            issues.add(issue);
        }
    }

    /**
     * Checa métodos muito longos.
     */
    private void checkMethodLengths(AnalysisRequest request, List<Issue> issues) {
        for (Map.Entry<String, String> entry : request.getFileContents().entrySet()) {
            String file = entry.getKey();
            String content = entry.getValue();

            // Verifica se é arquivo Java
            if (!file.endsWith(".java")) {
                continue;
            }

            // Contar linhas entre { e } de cada método
            String[] lines = content.split("\n");
            int methodStart = 0;
            boolean inMethod = false;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();

                // Detectar início de método (linha com { no final)
                if (!inMethod && (line.contains("public ") || line.contains("private ") ||
                        line.contains("protected ")) && line.endsWith("{")) {
                    inMethod = true;
                    methodStart = i;
                }

                // Detectar fim de método
                if (inMethod && line.equals("}")) {
                    int methodLength = i - methodStart;
                    if (methodLength > maxMethodLength) {
                        Issue issue = Issue.builder()
                                .type(IssueType.CODE_SMELL)
                                .severity(Severity.MEDIUM)
                                .title("Method too long")
                                .description(String.format("Method is %d lines, consider refactoring to smaller methods (max: %d)",
                                        methodLength, maxMethodLength))
                                .filePath(file)
                                .lineStart(methodStart + 1)
                                .lineEnd(i + 1)
                                .ruleId("EXAMPLE_LONG_METHOD")
                                .source(com.pullwise.api.domain.enums.IssueSource.LLM)
                                .createdAt(LocalDateTime.now())
                                .build();

                        issues.add(issue);
                    }
                    inMethod = false;
                }
            }
        }
    }

    // ========== CodeReviewPlugin ==========

    @Override
    public String getId() {
        return "com.pullwise.example.linter";
    }

    @Override
    public String getName() {
        return "Example Linter Plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "Pullwise.ai";
    }

    @Override
    public String getDescription() {
        return "Example plugin demonstrating common code checks";
    }

    @Override
    public PluginType getType() {
        return PluginType.LINTER;
    }

    @Override
    public Set<PluginLanguage> getSupportedLanguages() {
        return Set.of(PluginLanguage.JAVA, PluginLanguage.JAVASCRIPT, PluginLanguage.PYTHON);
    }

    @Override
    public int getPriority() {
        return 200; // Executar antes dos outros plugins
    }
}
