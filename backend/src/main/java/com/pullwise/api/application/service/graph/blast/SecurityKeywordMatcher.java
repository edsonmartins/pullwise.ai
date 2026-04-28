package com.pullwise.api.application.service.graph.blast;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Detecta presença de keywords sensíveis (segurança/autenticação/cripto) no
 * nome ou qualified name de um nó. Usado pelo {@link RiskScoringService} para
 * elevar o risco de mudanças em código sensível.
 *
 * <p>Lista alinhada com {@code code_review_graph/constants.py:7} (projeto
 * code-review-graph). Match case-insensitive, substring.
 */
@Component
public class SecurityKeywordMatcher {

    private static final List<String> KEYWORDS = List.of(
            "auth", "crypto", "password", "passwd", "secret", "token",
            "permission", "admin", "sudo", "privileg",
            "validate", "sanitize", "escape",
            "decrypt", "encrypt", "sign", "verify",
            "csrf", "xss", "session", "cookie",
            "credential", "apikey", "api_key", "bearer",
            "oauth", "jwt", "ssrf"
    );

    /**
     * Retorna o score de match: 0.0 se nenhum, 1.0 se matches >= 2,
     * intermediário (0.5) para um único match.
     */
    public double score(String simpleName, String qualifiedName) {
        Set<String> matched = matches(simpleName, qualifiedName);
        if (matched.isEmpty()) return 0.0;
        if (matched.size() == 1) return 0.5;
        return 1.0;
    }

    /** Conjunto (na ordem de descoberta) de keywords que casaram. */
    public Set<String> matches(String simpleName, String qualifiedName) {
        String lowerSimple = simpleName == null ? "" : simpleName.toLowerCase();
        String lowerQn = qualifiedName == null ? "" : qualifiedName.toLowerCase();
        Set<String> hits = new LinkedHashSet<>();
        for (String kw : KEYWORDS) {
            if (lowerSimple.contains(kw) || lowerQn.contains(kw)) {
                hits.add(kw);
            }
        }
        return hits;
    }

    public boolean hasMatch(String simpleName, String qualifiedName) {
        String lowerSimple = simpleName == null ? "" : simpleName.toLowerCase();
        String lowerQn = qualifiedName == null ? "" : qualifiedName.toLowerCase();
        for (String kw : KEYWORDS) {
            if (lowerSimple.contains(kw) || lowerQn.contains(kw)) return true;
        }
        return false;
    }
}
