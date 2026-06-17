package com.pullwise.api.application.service.review.pipeline.synthesis;

import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.domain.model.Issue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Corrige o número de linha de cada achado de forma determinística, casando o
 * trecho de código reportado ({@link Issue#getCodeSnippet()}) contra o diff do
 * arquivo. Resolve o problema clássico de <em>line drift</em> dos reviews via
 * LLM: o modelo erra com frequência o número exato da linha, mas é confiável
 * ao reproduzir o trecho de código com problema.
 *
 * <p>Estratégia (inspirada no Open Code Review da Alibaba):
 * <ol>
 *   <li>Tenta casar o trecho contra o <b>lado novo</b> do diff (contexto +
 *       linhas adicionadas) → número de linha no arquivo pós-mudança.</li>
 *   <li>Fallback para o <b>lado antigo</b> (contexto + linhas removidas).</li>
 * </ol>
 * O casamento ignora indentação e espaços em branco redundantes para tolerar
 * pequenas reformatações do snippet pelo LLM.
 */
@Slf4j
@Component
public class CommentPositioner {

    /**
     * Reposiciona, in-place, os issues que possuem {@code codeSnippet} e cujo
     * arquivo está presente nos diffs.
     *
     * @return número de issues efetivamente reposicionados
     */
    public int reposition(List<Issue> issues, List<GitHubService.FileDiff> diffs) {
        if (issues == null || issues.isEmpty() || diffs == null || diffs.isEmpty()) {
            return 0;
        }

        Map<String, String> patchByPath = new HashMap<>();
        for (GitHubService.FileDiff d : diffs) {
            if (d.patch() != null && !d.patch().isBlank()) {
                patchByPath.put(d.filename(), d.patch());
            }
        }

        int repositioned = 0;
        for (Issue issue : issues) {
            if (issue.getCodeSnippet() == null || issue.getCodeSnippet().isBlank()) {
                continue;
            }
            String patch = patchByPath.get(issue.getFilePath());
            if (patch == null) {
                continue;
            }

            int[] range = resolve(issue.getCodeSnippet(), patch);
            if (range != null) {
                issue.setLineStart(range[0]);
                issue.setLineEnd(range[1]);
                repositioned++;
            }
        }

        if (repositioned > 0) {
            log.debug("CommentPositioner repositioned {}/{} issue(s) via snippet matching",
                    repositioned, issues.size());
        }
        return repositioned;
    }

    /** Tenta resolver no lado novo do diff; se falhar, no lado antigo. */
    private int[] resolve(String snippet, String patch) {
        List<String> target = normalizeBlock(snippet);
        if (target.isEmpty()) {
            return null;
        }
        int[] r = matchAgainst(UnifiedDiffParser.parseNewSide(patch), target);
        if (r != null) {
            return r;
        }
        return matchAgainst(UnifiedDiffParser.parseOldSide(patch), target);
    }

    /**
     * Procura a primeira ocorrência consecutiva de {@code target} dentro das
     * linhas do diff (já normalizadas, ignorando linhas vazias).
     */
    private int[] matchAgainst(List<UnifiedDiffParser.LocatedLine> lines, List<String> target) {
        List<UnifiedDiffParser.LocatedLine> norm = new ArrayList<>(lines.size());
        for (UnifiedDiffParser.LocatedLine l : lines) {
            String n = normalizeLine(l.content());
            if (!n.isEmpty()) {
                norm.add(new UnifiedDiffParser.LocatedLine(l.lineNumber(), n));
            }
        }
        if (norm.size() < target.size()) {
            return null;
        }
        for (int i = 0; i + target.size() <= norm.size(); i++) {
            boolean ok = true;
            for (int j = 0; j < target.size(); j++) {
                if (!norm.get(i + j).content().equals(target.get(j))) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                int a = norm.get(i).lineNumber();
                int b = norm.get(i + target.size() - 1).lineNumber();
                return new int[]{Math.min(a, b), Math.max(a, b)};
            }
        }
        return null;
    }

    /** Normaliza um bloco em linhas não-vazias, ignorando espaços redundantes. */
    private List<String> normalizeBlock(String block) {
        List<String> out = new ArrayList<>();
        for (String line : block.split("\n")) {
            String n = normalizeLine(line);
            if (!n.isEmpty()) {
                out.add(n);
            }
        }
        return out;
    }

    /** Remove indentação/colapsa espaços para tolerar reformatação do snippet. */
    private String normalizeLine(String line) {
        return line.strip().replaceAll("\\s+", " ");
    }
}
