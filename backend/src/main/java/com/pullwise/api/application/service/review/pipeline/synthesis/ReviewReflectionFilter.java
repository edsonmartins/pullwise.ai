package com.pullwise.api.application.service.review.pipeline.synthesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.ReviewTaskType;
import com.pullwise.api.domain.model.Issue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Passo de reflexão ("review filter"): remove achados gerados por LLM que o
 * próprio diff <em>prova</em> estarem errados, reduzindo falsos-positivos.
 *
 * <p>Inspirado no módulo {@code REVIEW_FILTER_TASK} do Open Code Review
 * (Alibaba). O princípio central é <b>falsificar, não verificar</b>:
 * <ul>
 *   <li>Só remove um comentário quando o diff contém contra-evidência direta
 *       de que a afirmação central está errada.</li>
 *   <li>Na dúvida, mantém — o achado pode depender de contexto fora do diff
 *       (lógica em outros arquivos, semântica de negócio, runtime).</li>
 * </ul>
 *
 * <p>Apenas issues de origem {@link IssueSource#LLM} são avaliados; achados de
 * ferramentas SAST são determinísticos e passam intactos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewReflectionFilter {

    private final MultiModelLLMRouter llmRouter;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a fact-checker for code review comments.

            The comments below were produced by another reviewer agent that had
            full code context (it could open files and search the codebase). You
            can see ONLY the diff provided.

            Your task is NOT to verify whether the comments are correct — it is
            to remove ONLY the comments that the diff itself PROVES wrong.

            Core principle: falsify, do not verify.
            - Flag a comment ONLY when the diff contains direct counter-evidence
              that its key claim is wrong (e.g. it flags code that the diff shows
              is plainly correct, or describes behavior the diff contradicts).
            - Do NOT flag a comment merely because you cannot verify it. If the
              claim depends on information not visible in the diff (logic in other
              files, business semantics, runtime behavior), let it pass.

            Output ONLY a JSON array with the integer ids of the comments to
            REMOVE. Return [] if none should be removed. No prose, no explanation.
            """;

    /**
     * Filtra os issues, marcando como falso-positivo e removendo do resultado
     * os achados de LLM que o diff prova estarem errados.
     *
     * @return nova lista sem os achados rejeitados (a ordem dos demais é mantida)
     */
    public List<Issue> filter(List<Issue> issues, List<GitHubService.FileDiff> diffs) {
        if (issues == null || issues.isEmpty()) {
            return issues == null ? List.of() : issues;
        }
        if (diffs == null || diffs.isEmpty()) {
            return issues;
        }

        Map<String, String> patchByPath = new HashMap<>();
        for (GitHubService.FileDiff d : diffs) {
            if (d.patch() != null && !d.patch().isBlank()) {
                patchByPath.put(d.filename(), d.patch());
            }
        }

        Map<String, List<Issue>> byFile = issues.stream()
                .filter(i -> i.getSource() == IssueSource.LLM)
                .filter(i -> i.getFilePath() != null && patchByPath.containsKey(i.getFilePath()))
                .collect(Collectors.groupingBy(Issue::getFilePath));

        if (byFile.isEmpty()) {
            return issues;
        }

        Set<Issue> rejected = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Map.Entry<String, List<Issue>> entry : byFile.entrySet()) {
            String path = entry.getKey();
            List<Issue> fileIssues = entry.getValue();
            try {
                Set<Integer> toRemove = askLlm(path, patchByPath.get(path), fileIssues);
                for (Integer idx : toRemove) {
                    if (idx != null && idx >= 0 && idx < fileIssues.size()) {
                        Issue bad = fileIssues.get(idx);
                        bad.setIsFalsePositive(true);
                        rejected.add(bad);
                    }
                }
            } catch (Exception e) {
                // Resiliente: na falha, não descarta nada para aquele arquivo.
                log.warn("Reflection filter failed for {}: {}", path, e.getMessage());
            }
        }

        if (rejected.isEmpty()) {
            return issues;
        }

        List<Issue> kept = issues.stream()
                .filter(i -> !rejected.contains(i))
                .collect(Collectors.toList());
        log.debug("Reflection filter removed {} provably-wrong finding(s) ({} -> {})",
                rejected.size(), issues.size(), kept.size());
        return kept;
    }

    private Set<Integer> askLlm(String path, String patch, List<Issue> fileIssues) {
        StringBuilder up = new StringBuilder();
        up.append("### Code diff for `").append(path).append("`\n");
        up.append("```diff\n").append(patch).append("\n```\n\n");
        up.append("### Review comments\n");
        for (int i = 0; i < fileIssues.size(); i++) {
            Issue is = fileIssues.get(i);
            up.append("[id=").append(i).append("] ")
                    .append(safe(is.getTitle()))
                    .append(" — ").append(truncate(is.getDescription(), 400));
            if (is.getCodeSnippet() != null && !is.getCodeSnippet().isBlank()) {
                up.append(" (code: ").append(truncate(is.getCodeSnippet(), 200)).append(")");
            }
            up.append("\n");
        }
        up.append("\nReturn the JSON array of ids to remove.");

        var response = llmRouter.execute(ReviewTaskType.QA, SYSTEM_PROMPT, up.toString());
        return parseIds(response.content());
    }

    private Set<Integer> parseIds(String response) {
        Set<Integer> ids = new HashSet<>();
        if (response == null) {
            return ids;
        }
        String json = extractJsonArray(response);
        if (json == null) {
            return ids;
        }
        try {
            Integer[] arr = objectMapper.readValue(json, Integer[].class);
            Collections.addAll(ids, arr);
        } catch (Exception e) {
            log.debug("Could not parse reflection filter response: {}", e.getMessage());
        }
        return ids;
    }

    private String extractJsonArray(String s) {
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return null;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
