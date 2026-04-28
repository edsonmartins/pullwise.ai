package com.pullwise.api.application.service.graph.blast;

import com.pullwise.api.domain.enums.NodeKind;
import com.pullwise.api.domain.enums.ProgrammingLanguage;
import com.pullwise.api.domain.model.CodeGraphNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RiskScoringServiceTest {

    private final SecurityKeywordMatcher securityKeywordMatcher = new SecurityKeywordMatcher();
    private final RiskScoringService service = new RiskScoringService(securityKeywordMatcher);

    @Test
    void nullNode_returnsZero() {
        var risk = service.score(null, 1, 1.0);
        assertThat(risk.total()).isZero();
    }

    @Test
    void shallowSecurityHotspotUntested_yieldsHighRisk() {
        CodeGraphNode node = node("AuthService", "com.x.AuthService", 1.0, false);

        var risk = service.score(node, 0, 1.0);

        // depth_inverse=1.0*0.30=0.30, hotspot=1.0*0.25=0.25,
        // security>=0.5*0.25=0.125, test_gap=1.0*0.20=0.20 → 0.875 baseline
        assertThat(risk.total()).isGreaterThanOrEqualTo(0.7);
        assertThat(risk.breakdown()).containsKeys("depth_inverse", "hotspot", "security", "test_gap", "confidence");
    }

    @Test
    void deepNonSensitiveTested_yieldsLowRisk() {
        CodeGraphNode node = node("MathUtils", "com.x.MathUtils", 0.0, true);

        var risk = service.score(node, 5, 1.0);

        // depth_inverse=1/6 ≈ 0.166, hotspot=0, security=0, test_gap=0 → ~0.05
        assertThat(risk.total()).isLessThan(0.10);
    }

    @Test
    void confidenceMultiplierAttenuatesRisk() {
        CodeGraphNode node = node("AuthService", "com.x.AuthService", 1.0, false);

        var fullConf = service.score(node, 0, 1.0);
        var ambiguous = service.score(node, 0, 0.4); // confidence típico AMBIGUOUS

        assertThat(ambiguous.total()).isLessThan(fullConf.total());
        // Diferença proporcional ao confidence
        assertThat(ambiguous.total()).isCloseTo(fullConf.total() * 0.4, within(0.01));
    }

    @Test
    void breakdownExposesEachDimension() {
        // "encrypt" + "password" → 2 keywords → security score = 1.0
        CodeGraphNode node = node("EncryptedPasswordStore", "com.x.EncryptedPasswordStore", 0.5, false);

        var risk = service.score(node, 1, 0.7);

        assertThat(risk.breakdown().get("depth_inverse")).isEqualTo(0.5);
        assertThat(risk.breakdown().get("hotspot")).isEqualTo(0.5);
        assertThat(risk.breakdown().get("security")).isEqualTo(1.0);
        assertThat(risk.breakdown().get("test_gap")).isEqualTo(1.0);
        assertThat(risk.breakdown().get("confidence")).isEqualTo(0.7);
    }

    @Test
    void totalIsClampedToOne() {
        // Mesmo com inputs absurdos via builder, score nunca passa de 1
        CodeGraphNode node = node("AuthCryptoTokenSecret", "x.AuthCryptoTokenSecret", 99.0, false);

        var risk = service.score(node, 0, 1.0);

        assertThat(risk.total()).isLessThanOrEqualTo(1.0);
    }

    private CodeGraphNode node(String simple, String qn, double hotspot, boolean tested) {
        return CodeGraphNode.builder()
                .simpleName(simple)
                .qualifiedName(qn)
                .kind(NodeKind.CLASS)
                .filePath("/src/" + simple + ".java")
                .language(ProgrammingLanguage.JAVA)
                .hotspotScore(hotspot)
                .hasTestCoverage(tested)
                .build();
    }
}
