package com.pullwise.api.application.service.graph.blast;

import com.pullwise.api.domain.model.CodeGraphNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calcula o risk score (0..1) de um nó alcançado pela BFS de blast-radius.
 *
 * <p>Soma ponderada das dimensões:
 * <pre>
 *   depth_inverse  (0.30) — quanto mais próximo do seed, mais arriscado
 *   hotspot        (0.25) — código historicamente problemático
 *   security       (0.25) — palavras-chave de auth/cripto/etc
 *   test_gap       (0.20) — falta de cobertura de testes
 * </pre>
 * O total é multiplicado por {@code propagatedConfidence}, atenuando o sinal
 * em caminhos via arestas AMBIGUOUS/INFERRED.
 */
@Component
@RequiredArgsConstructor
public class RiskScoringService {

    public static final double WEIGHT_DEPTH = 0.30;
    public static final double WEIGHT_HOTSPOT = 0.25;
    public static final double WEIGHT_SECURITY = 0.25;
    public static final double WEIGHT_TEST_GAP = 0.20;

    private final SecurityKeywordMatcher securityKeywordMatcher;

    public RiskScore score(CodeGraphNode node, int depth, double propagatedConfidence) {
        if (node == null) return RiskScore.zero();

        double depthInverse = 1.0 / (1 + Math.max(0, depth));
        double hotspot = clamp01(node.getHotspotScore() == null ? 0.0 : node.getHotspotScore());
        double security = securityKeywordMatcher.score(node.getSimpleName(), node.getQualifiedName());
        double testGap = Boolean.TRUE.equals(node.getHasTestCoverage()) ? 0.0 : 1.0;
        double confidence = clamp01(propagatedConfidence);

        double base = depthInverse * WEIGHT_DEPTH
                + hotspot * WEIGHT_HOTSPOT
                + security * WEIGHT_SECURITY
                + testGap * WEIGHT_TEST_GAP;

        double total = clamp01(base * confidence);

        Map<String, Double> breakdown = new LinkedHashMap<>();
        breakdown.put("depth_inverse", round(depthInverse));
        breakdown.put("hotspot", round(hotspot));
        breakdown.put("security", round(security));
        breakdown.put("test_gap", round(testGap));
        breakdown.put("confidence", round(confidence));

        return new RiskScore(round(total), breakdown);
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    public record RiskScore(double total, Map<String, Double> breakdown) {
        public static RiskScore zero() {
            return new RiskScore(0.0, Map.of());
        }
    }
}
