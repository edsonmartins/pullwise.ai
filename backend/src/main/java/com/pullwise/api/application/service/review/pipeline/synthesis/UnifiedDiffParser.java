package com.pullwise.api.application.service.review.pipeline.synthesis;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser de unified diff que mapeia cada linha do patch para o seu número de
 * linha real no arquivo novo (new-side) ou antigo (old-side).
 *
 * <p>Usado pelo {@link CommentPositioner} para resolver de forma determinística
 * o número de linha de um achado a partir do trecho de código ({@code codeSnippet})
 * reportado pelo LLM — eliminando o "drift" de linha típico de reviews via LLM.
 *
 * <p>Inspirado no resolver determinístico do Open Code Review (Alibaba):
 * o LLM informa <em>qual</em> trecho está com problema; a engenharia decide
 * <em>onde</em> ele está.
 */
public final class UnifiedDiffParser {

    /** Cabeçalho de hunk: {@code @@ -oldStart,oldCount +newStart,newCount @@}. */
    private static final Pattern HUNK_HEADER =
            Pattern.compile("^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@");

    private UnifiedDiffParser() {
        // Utility class — prevent instantiation
    }

    /** Uma linha do diff com o seu número de linha absoluto no arquivo. */
    public record LocatedLine(int lineNumber, String content) {}

    /**
     * Linhas presentes no lado novo do diff (contexto + adicionadas), com os
     * números de linha do arquivo novo (pós-mudança). É o lado usado para
     * comentários inline em PRs.
     */
    public static List<LocatedLine> parseNewSide(String patch) {
        return parse(patch, true);
    }

    /**
     * Linhas presentes no lado antigo do diff (contexto + removidas), com os
     * números de linha do arquivo antigo (pré-mudança).
     */
    public static List<LocatedLine> parseOldSide(String patch) {
        return parse(patch, false);
    }

    private static List<LocatedLine> parse(String patch, boolean newSide) {
        List<LocatedLine> result = new ArrayList<>();
        if (patch == null || patch.isBlank()) {
            return result;
        }

        int oldLine = 0;
        int newLine = 0;
        boolean inHunk = false;

        for (String raw : patch.split("\n", -1)) {
            Matcher m = HUNK_HEADER.matcher(raw);
            if (m.find()) {
                oldLine = Integer.parseInt(m.group(1));
                newLine = Integer.parseInt(m.group(3));
                inHunk = true;
                continue;
            }
            if (!inHunk || raw.isEmpty()) {
                continue;
            }

            char marker = raw.charAt(0);
            String content = raw.substring(1);
            switch (marker) {
                case '+' -> {
                    if (newSide) result.add(new LocatedLine(newLine, content));
                    newLine++;
                }
                case '-' -> {
                    if (!newSide) result.add(new LocatedLine(oldLine, content));
                    oldLine++;
                }
                case ' ' -> {
                    result.add(new LocatedLine(newSide ? newLine : oldLine, content));
                    newLine++;
                    oldLine++;
                }
                // "\ No newline at end of file" e marcadores desconhecidos: ignorar
                // sem mexer nos contadores para não corromper o mapeamento.
                default -> { /* skip */ }
            }
        }
        return result;
    }
}
