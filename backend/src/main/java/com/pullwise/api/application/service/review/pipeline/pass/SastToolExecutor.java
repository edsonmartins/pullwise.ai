package com.pullwise.api.application.service.review.pipeline.pass;

import com.pullwise.api.application.service.integration.SonarQubeService;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.enums.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Executor de ferramentas SAST.
 *
 * <p>Executa ferramentas de análise estática via CLI ou API e parse os resultados.
 *
 * <p>Nota: Em produção, muitas dessas ferramentas seriam executadas em containers
 * ou em um cluster separado para não sobrecarregar o servidor principal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SastToolExecutor {

    private final SonarQubeService sonarQubeService;

    /**
     * Executa uma ferramenta SAST específica.
     *
     * @param tool        A ferramenta a ser executada
     * @param pullRequest O PR sendo analisado
     * @param review      O review associado
     * @return Lista de issues encontrados
     */
    public List<SastAggregatorPass.ToolIssue> execute(SastAggregatorPass.SastTool tool,
                                                       PullRequest pullRequest,
                                                       Review review) {
        return switch (tool) {
            case SONARQUBE -> executeSonarQube(pullRequest, review);
            case CHECKSTYLE -> executeCheckstyle(pullRequest, review);
            case PMD -> executePMD(pullRequest, review);
            case SPOTBUGS -> executeSpotBugs(pullRequest, review);
            case ESLINT -> executeEslint(pullRequest, review);
            case BIOME -> executeBiome(pullRequest, review);
            case RUFF -> executeRuff(pullRequest, review);
            case PYLINT -> executePylint(pullRequest, review);
            default -> {
                log.debug("Tool {} not implemented, returning empty results", tool.getName());
                yield List.of();
            }
        };
    }

    /**
     * Executa SonarQube via API.
     */
    private List<SastAggregatorPass.ToolIssue> executeSonarQube(PullRequest pullRequest, Review review) {
        if (!sonarQubeService.isConfigured()) {
            return List.of();
        }

        try {
            // TODO: Implementar integração completa com SonarQube
            // Por ora, retorna resultado vazio
            log.debug("SonarQube analysis for PR {} (not yet fully implemented)", pullRequest.getPrNumber());
            return List.of();
        } catch (Exception e) {
            log.warn("SonarQube analysis failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Executa Checkstyle (Java).
     */
    private List<SastAggregatorPass.ToolIssue> executeCheckstyle(PullRequest pullRequest, Review review) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();

        try {
            // Em produção, executaria: java -jar checkstyle.jar -c config.xml files/
            // Por ora, retorna resultado simulado
            log.debug("Checkstyle analysis for PR {}", pullRequest.getPrNumber());

            // TODO: Implementar execução real do Checkstyle
            // String[] cmd = {"java", "-jar", "checkstyle.jar", "-c", "google_checks.xml", path};

        } catch (Exception e) {
            log.warn("Checkstyle execution failed: {}", e.getMessage());
        }

        return issues;
    }

    /**
     * Executa PMD (Java).
     */
    private List<SastAggregatorPass.ToolIssue> executePMD(PullRequest pullRequest, Review review) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();

        try {
            log.debug("PMD analysis for PR {}", pullRequest.getPrNumber());
            // TODO: Implementar execução real do PMD

        } catch (Exception e) {
            log.warn("PMD execution failed: {}", e.getMessage());
        }

        return issues;
    }

    /**
     * Executa SpotBugs (Java).
     */
    private List<SastAggregatorPass.ToolIssue> executeSpotBugs(PullRequest pullRequest, Review review) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();

        try {
            log.debug("SpotBugs analysis for PR {}", pullRequest.getPrNumber());
            // TODO: Implementar execução real do SpotBugs

        } catch (Exception e) {
            log.warn("SpotBugs execution failed: {}", e.getMessage());
        }

        return issues;
    }

    /**
     * Executa ESLint (JavaScript/TypeScript).
     */
    private List<SastAggregatorPass.ToolIssue> executeEslint(PullRequest pullRequest, Review review) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();

        try {
            log.debug("ESLint analysis for PR {}", pullRequest.getPrNumber());
            // TODO: Implementar execução real do ESLint
            // String[] cmd = {"eslint", "--format", "json", path};

        } catch (Exception e) {
            log.warn("ESLint execution failed: {}", e.getMessage());
        }

        return issues;
    }

    /**
     * Executa Biome (JavaScript/TypeScript) - ferramenta Rust, muito rápida.
     */
    private List<SastAggregatorPass.ToolIssue> executeBiome(PullRequest pullRequest, Review review) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();

        try {
            log.debug("Biome analysis for PR {}", pullRequest.getPrNumber());
            // TODO: Implementar execução real do Biome
            // String[] cmd = {"biome", "lint", "--json", path};

        } catch (Exception e) {
            log.warn("Biome execution failed: {}", e.getMessage());
        }

        return issues;
    }

    /**
     * Executa Ruff (Python) - ferramenta Rust, 100x mais rápido que Pylint.
     */
    private List<SastAggregatorPass.ToolIssue> executeRuff(PullRequest pullRequest, Review review) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();

        try {
            log.debug("Ruff analysis for PR {}", pullRequest.getPrNumber());
            // TODO: Implementar execução real do Ruff
            // String[] cmd = {"ruff", "check", "--output-format", "json", path};

        } catch (Exception e) {
            log.warn("Ruff execution failed: {}", e.getMessage());
        }

        return issues;
    }

    /**
     * Executa Pylint (Python).
     */
    private List<SastAggregatorPass.ToolIssue> executePylint(PullRequest pullRequest, Review review) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();

        try {
            log.debug("Pylint analysis for PR {}", pullRequest.getPrNumber());
            // TODO: Implementar execução real do Pylint
            // String[] cmd = {"pylint", "--output-format", "json", path};

        } catch (Exception e) {
            log.warn("Pylint execution failed: {}", e.getMessage());
        }

        return issues;
    }

    /**
     * Executa um comando CLI e retorna o output.
     */
    private String executeCommand(String[] cmd, File workingDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0 && exitCode != 1) {  // 1 pode ser normal para linters (issues found)
            throw new RuntimeException("Command exited with code " + exitCode);
        }

        return output.toString();
    }
}
