package com.pullwise.api.application.service.review.pipeline.synthesis;

import com.pullwise.api.domain.model.Issue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detector de duplicação de issues.
 *
 * <p>Remove issues duplicados que podem ser reportados por múltiplas
 * ferramentas ou passadas do pipeline.
 *
 * <p>Estratégias de deduplicação:
 * - Mesmo arquivo e linha similar
 * - Mesma categoria e descrição similar
 * - Mesma regra/regra de lint
 */
@Slf4j
@Component
public class IssueDuplicationDetector {

    private static final double SIMILARITY_THRESHOLD = 0.8;

    /**
     * Remove issues duplicados da lista.
     *
     * @param issues Lista de todos os issues
     * @return Lista sem duplicatas
     */
    public List<Issue> deduplicate(List<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }

        int originalSize = issues.size();
        List<Issue> deduplicated = new ArrayList<>();

        // Agrupa issues por arquivo
        Map<String, List<Issue>> byFile = issues.stream()
                .collect(Collectors.groupingBy(Issue::getFilePath));

        // Para cada arquivo, deduplica issues
        for (var entry : byFile.entrySet()) {
            List<Issue> fileIssues = deduplicateInFile(entry.getValue());
            deduplicated.addAll(fileIssues);
        }

        int removed = originalSize - deduplicated.size();
        if (removed > 0) {
            log.debug("Deduplication: removed {} duplicate issues ({} -> {})",
                    removed, originalSize, deduplicated.size());
        }

        return deduplicated;
    }

    /**
     * Deduplica issues dentro do mesmo arquivo.
     */
    private List<Issue> deduplicateInFile(List<Issue> fileIssues) {
        List<Issue> unique = new ArrayList<>();
        Set<String> seenSignatures = new HashSet<>();

        for (Issue issue : fileIssues) {
            String signature = generateSignature(issue);

            // Verifica se já existe issue similar
            boolean isDuplicate = unique.stream()
                    .anyMatch(existing -> isSimilar(existing, issue));

            if (!isDuplicate) {
                unique.add(issue);
                seenSignatures.add(signature);
            } else {
                log.debug("Skipping duplicate issue: {} at line {}",
                        issue.getTitle(), issue.getLineStart());
            }
        }

        return unique;
    }

    /**
     * Gera uma assinatura única para o issue.
     */
    private String generateSignature(Issue issue) {
        return String.format("%s:%d:%s",
                issue.getFilePath(),
                issue.getLineStart(),
                issue.getRuleId() != null ? issue.getRuleId() : issue.getType());
    }

    /**
     * Verifica se dois issues são similares (possíveis duplicatas).
     */
    private boolean isSimilar(Issue a, Issue b) {
        // Mesma regra = duplicata
        if (a.getRuleId() != null && a.getRuleId().equals(b.getRuleId())) {
            return true;
        }

        // Mesmo tipo e linha próxima
        if (a.getType() == b.getType()) {
            int lineDiff = Math.abs(
                    (a.getLineStart() != null ? a.getLineStart() : 0) -
                    (b.getLineStart() != null ? b.getLineStart() : 0)
                );

            if (lineDiff <= 2) {
                // Mesmo tipo e linha muito próxima = provável duplicata
                return true;
            }
        }

        // Similaridade de texto
        double textSimilarity = calculateTextSimilarity(
                a.getTitle() + " " + a.getDescription(),
                b.getTitle() + " " + b.getDescription()
        );

        return textSimilarity >= SIMILARITY_THRESHOLD;
    }

    /**
     * Calcula similaridade entre dois textos (simplificado).
     * Usa algoritmo de intersecção de palavras.
     */
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        Set<String> words1 = tokenize(text1);
        Set<String> words2 = tokenize(text2);

        if (words1.isEmpty() && words2.isEmpty()) {
            return 1.0;
        }

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * Tokeniza texto em palavras (normalizadas).
     */
    private Set<String> tokenize(String text) {
        if (text == null) {
            return Set.of();
        }

        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", " ")
                        .split("\\s+"))
                .filter(w -> w.length() > 3)  // Ignora palavras muito curtas
                .collect(Collectors.toSet());
    }

    /**
     * Mescla issues duplicados, mantendo o mais severo.
     *
     * @param issues Lista de issues possivelmente duplicados
     * @return Lista mesclada
     */
    public List<Issue> mergeDuplicates(List<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }

        Map<String, Issue> merged = new LinkedHashMap<>();

        for (Issue issue : issues) {
            String key = generateSignature(issue);

            Issue existing = merged.get(key);
            if (existing == null) {
                merged.put(key, issue);
            } else {
                // Mantém o mais severo
                if (compareSeverity(issue.getSeverity(), existing.getSeverity()) > 0) {
                    merged.put(key, issue);
                }
            }
        }

        return new ArrayList<>(merged.values());
    }

    /**
     * Compara duas severidades.
     *
     * @return positivo se a > b, negativo se a < b, zero se iguais
     */
    private int compareSeverity(com.pullwise.api.domain.enums.Severity a,
                               com.pullwise.api.domain.enums.Severity b) {
        return Integer.compare(a.ordinal(), b.ordinal());
    }
}
