package com.pullwise.api.application.service.graph.blast;

import com.pullwise.api.domain.enums.ConfidenceTier;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Classifica a confiança de uma aresta do code graph com base no nome
 * original e no nome resolvido pelo {@link com.pullwise.api.application.service.graph.GraphPersistenceService}.
 *
 * <p>Regras (em ordem):
 * <ol>
 *   <li>Tipo genérico, wildcard, array, primitivo, ou nulo → {@link ConfidenceTier#AMBIGUOUS}.</li>
 *   <li>Nome em pacote padrão JDK / framework comum (java.*, javax.*, org.springframework.*, ...) → ignorado upstream;
 *       se chegar aqui é {@link ConfidenceTier#AMBIGUOUS}.</li>
 *   <li>Nome original já qualificado (contém {@code "."}) → {@link ConfidenceTier#EXTRACTED}.</li>
 *   <li>Resolvido a partir de simple-name dentro do mesmo pacote → {@link ConfidenceTier#INFERRED}.</li>
 *   <li>Demais casos → {@link ConfidenceTier#AMBIGUOUS}.</li>
 * </ol>
 */
@Component
public class EdgeConfidenceClassifier {

    private static final Set<String> EXTERNAL_PACKAGE_PREFIXES = Set.of(
            "java.", "javax.", "jakarta.",
            "org.springframework.", "org.springdoc.",
            "com.fasterxml.", "com.google.",
            "lombok.", "org.slf4j.", "org.apache.",
            "io.micrometer.", "io.swagger."
    );

    private static final Set<String> JAVA_PRIMITIVES = Set.of(
            "int", "long", "double", "float", "boolean", "char", "byte", "short", "void"
    );

    /**
     * Classifica a aresta. {@code originalName} é o nome como aparecia no AST
     * (e.g. {@code "UserRepository"} ou {@code "com.example.UserRepository"});
     * {@code resolvedQualifiedName} é o que foi gravado como {@code target_qualified}.
     */
    public ConfidenceTier classify(String originalName, String resolvedQualifiedName) {
        if (originalName == null || originalName.isBlank()) {
            return ConfidenceTier.AMBIGUOUS;
        }

        // Genéricos, wildcards, arrays
        if (originalName.contains("<") || originalName.contains("?") || originalName.contains("[")) {
            return ConfidenceTier.AMBIGUOUS;
        }

        // Primitivos
        if (JAVA_PRIMITIVES.contains(originalName)) {
            return ConfidenceTier.AMBIGUOUS;
        }

        // Nome qualificado em pacote externo: AMBIGUOUS — não temos visibilidade de internals
        if (originalName.contains(".") && isExternalPackage(originalName)) {
            return ConfidenceTier.AMBIGUOUS;
        }

        // Nome qualificado interno
        if (originalName.contains(".")) {
            return ConfidenceTier.EXTRACTED;
        }

        // Resolvido por heurística (simple-name dentro do mesmo pacote)
        if (resolvedQualifiedName != null && resolvedQualifiedName.contains(".")) {
            return ConfidenceTier.INFERRED;
        }

        return ConfidenceTier.AMBIGUOUS;
    }

    /**
     * Útil para chamadas de método cross-instance: {@code obj.foo()} onde
     * {@code obj} é um campo cuja resolução de tipo é ambígua.
     */
    public ConfidenceTier classifyMethodCall(String calledClassExpression, String resolvedQualifiedName) {
        if (calledClassExpression == null || calledClassExpression.isBlank()) {
            return ConfidenceTier.AMBIGUOUS;
        }
        // expressões com ponto no meio sugerem field/method chaining → cross-binding
        if (calledClassExpression.contains(".") && Character.isLowerCase(calledClassExpression.charAt(0))) {
            return ConfidenceTier.AMBIGUOUS;
        }
        return classify(calledClassExpression, resolvedQualifiedName);
    }

    private boolean isExternalPackage(String qualifiedName) {
        for (String prefix : EXTERNAL_PACKAGE_PREFIXES) {
            if (qualifiedName.startsWith(prefix)) return true;
        }
        return false;
    }
}
