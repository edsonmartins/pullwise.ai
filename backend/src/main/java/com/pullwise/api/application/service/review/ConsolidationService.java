package com.pullwise.api.application.service.review;

import com.pullwise.api.domain.model.Issue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de consolidação de issues de diferentes fontes.
 * Remove duplicatas e agrupa issues similares.
 */
@Slf4j
@Service
public class ConsolidationService {

    /**
     * Consolida issues de SAST e LLM, removendo duplicatas.
     */
    public List<Issue> consolidateIssues(List<Issue> sastIssues, List<Issue> llmIssues) {
        List<Issue> allIssues = new ArrayList<>();
        allIssues.addAll(sastIssues);
        allIssues.addAll(llmIssues);

        // Remover duplicatas baseado em: filePath + type + título similar
        Map<String, Issue> uniqueIssues = new LinkedHashMap<>();

        for (Issue issue : allIssues) {
            String key = generateIssueKey(issue);

            if (uniqueIssues.containsKey(key)) {
                // Já existe issue similar, manter a de maior severidade
                Issue existing = uniqueIssues.get(key);
                if (issue.getSeverity().isHigherThan(existing.getSeverity())) {
                    uniqueIssues.put(key, issue);
                }
            } else {
                uniqueIssues.put(key, issue);
            }
        }

        List<Issue> consolidated = new ArrayList<>(uniqueIssues.values());

        // Ordenar por severidade
        consolidated.sort((a, b) -> a.getSeverity().getLevel() - b.getSeverity().getLevel());

        log.debug("Consolidated {} issues (SAST: {}, LLM: {}) into {} unique issues",
                sastIssues.size() + llmIssues.size(),
                sastIssues.size(),
                llmIssues.size(),
                consolidated.size());

        return consolidated;
    }

    /**
     * Gera uma chave única para identificar issues duplicatas.
     */
    private String generateIssueKey(Issue issue) {
        String file = issue.getFilePath() != null ? issue.getFilePath() : "general";
        String type = issue.getType().getCode();
        String title = issue.getTitle().toLowerCase().replaceAll("[^a-z0-9]", "");

        // Criar hash simplificado do título (primeiras 3 palavras)
        String[] words = title.split("\\s+");
        String titleKey = String.join("_", Arrays.stream(words)
                .limit(3)
                .toList());

        return file + ":" + type + ":" + titleKey;
    }

    /**
     * Agrupa issues por severidade.
     */
    public Map<com.pullwise.api.domain.enums.Severity, List<Issue>> groupBySeverity(List<Issue> issues) {
        return issues.stream()
                .collect(Collectors.groupingBy(Issue::getSeverity));
    }

    /**
     * Agrupa issues por arquivo.
     */
    public Map<String, List<Issue>> groupByFile(List<Issue> issues) {
        return issues.stream()
                .filter(Issue::hasLocation)
                .collect(Collectors.groupingBy(i ->
                        i.getFilePath() != null ? i.getFilePath() : "unknown"));
    }

    /**
     * Filtra issues por severidade mínima.
     */
    public List<Issue> filterByMinSeverity(List<Issue> issues, com.pullwise.api.domain.enums.Severity minSeverity) {
        return issues.stream()
                .filter(issue -> !issue.getSeverity().isHigherThan(minSeverity) ||
                        issue.getSeverity().getLevel() == minSeverity.getLevel())
                .toList();
    }
}
