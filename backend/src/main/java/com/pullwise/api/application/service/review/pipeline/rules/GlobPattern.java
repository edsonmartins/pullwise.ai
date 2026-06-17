package com.pullwise.api.application.service.review.pipeline.rules;

import java.util.regex.Pattern;

/**
 * Converte um glob (estilo {@code doublestar}) em {@link Pattern} regex para
 * casar caminhos de arquivo. Suporta:
 * <ul>
 *   <li>{@code *} — qualquer sequência exceto {@code /}</li>
 *   <li>{@code **} — qualquer sequência incluindo {@code /}</li>
 *   <li>{@code **&#47;} — zero ou mais diretórios (também casa na raiz)</li>
 *   <li>{@code ?} — um caractere exceto {@code /}</li>
 *   <li>{@code {a,b,c}} — alternativas</li>
 * </ul>
 * O casamento é case-insensitive, espelhando o resolver do Open Code Review.
 */
public final class GlobPattern {

    private GlobPattern() {
        // Utility class — prevent instantiation
    }

    public static Pattern compile(String glob) {
        StringBuilder sb = new StringBuilder("^");
        int i = 0;
        int n = glob.length();
        boolean inBrace = false;

        while (i < n) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> {
                    boolean doubleStar = i + 1 < n && glob.charAt(i + 1) == '*';
                    if (doubleStar) {
                        if (i + 2 < n && glob.charAt(i + 2) == '/') {
                            sb.append("(?:.*/)?"); // **/ → zero ou mais diretórios
                            i += 3;
                        } else {
                            sb.append(".*");
                            i += 2;
                        }
                    } else {
                        sb.append("[^/]*");
                        i++;
                    }
                }
                case '?' -> {
                    sb.append("[^/]");
                    i++;
                }
                case '{' -> {
                    sb.append("(?:");
                    inBrace = true;
                    i++;
                }
                case '}' -> {
                    sb.append(')');
                    inBrace = false;
                    i++;
                }
                case ',' -> {
                    sb.append(inBrace ? "|" : "\\,");
                    i++;
                }
                // caracteres com significado em regex que devem ser literais no glob
                case '.', '(', ')', '+', '^', '$', '|', '\\', '[', ']' -> {
                    sb.append('\\').append(c);
                    i++;
                }
                default -> {
                    sb.append(c);
                    i++;
                }
            }
        }
        sb.append('$');
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }
}
