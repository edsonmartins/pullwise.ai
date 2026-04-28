package com.pullwise.api.application.service.review.pipeline.synthesis;

import com.pullwise.api.application.service.graph.blast.BlastRadiusResult;
import com.pullwise.api.application.service.graph.blast.ImpactedNode;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BlastRadiusConsolidatorTest {

    private final BlastRadiusConsolidator consolidator = new BlastRadiusConsolidator();

    @Test
    void emptyInputs_returnsZero() {
        assertThat(consolidator.consolidate(null, null)).isZero();
        assertThat(consolidator.consolidate(List.of(), null)).isZero();
        assertThat(consolidator.consolidate(List.of(issue("/x.java", Severity.LOW)), null)).isZero();
        assertThat(consolidator.consolidate(List.of(issue("/x.java", Severity.LOW)),
                BlastRadiusResult.empty())).isZero();
    }

    @Test
    void promotesSeverityForFilesInBlastRadius() {
        Issue lowOnImpacted = issue("/src/B.java", Severity.LOW);
        Issue mediumOnImpacted = issue("/src/B.java", Severity.MEDIUM);
        Issue highOnImpacted = issue("/src/B.java", Severity.HIGH);
        Issue lowOutOfBlast = issue("/src/Z.java", Severity.LOW);
        Issue criticalOnImpacted = issue("/src/B.java", Severity.CRITICAL);

        BlastRadiusResult blast = blast(
                impactedNode("com.x.B", "/src/B.java", 0.8),
                impactedNode("com.x.C", "/src/C.java", 0.6)
        );

        int promoted = consolidator.consolidate(
                List.of(lowOnImpacted, mediumOnImpacted, highOnImpacted, lowOutOfBlast, criticalOnImpacted),
                blast);

        assertThat(promoted).isEqualTo(3); // low, medium, high foram promovidos
        assertThat(lowOnImpacted.getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(mediumOnImpacted.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(highOnImpacted.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(lowOutOfBlast.getSeverity()).isEqualTo(Severity.LOW); // intacto
        assertThat(criticalOnImpacted.getSeverity()).isEqualTo(Severity.CRITICAL); // teto
    }

    @Test
    void doesNotPromoteWhenMaxRiskBelowThreshold() {
        Issue issue = issue("/src/B.java", Severity.LOW);
        // riskScore=0.20 está abaixo do threshold 0.35 do consolidator
        BlastRadiusResult blast = blast(impactedNode("com.x.B", "/src/B.java", 0.20));

        int promoted = consolidator.consolidate(List.of(issue), blast);

        assertThat(promoted).isZero();
        assertThat(issue.getSeverity()).isEqualTo(Severity.LOW);
    }

    @Test
    void infoIsNotPromoted() {
        Issue info = issue("/src/B.java", Severity.INFO);
        BlastRadiusResult blast = blast(impactedNode("com.x.B", "/src/B.java", 0.9));

        int promoted = consolidator.consolidate(List.of(info), blast);

        assertThat(promoted).isZero();
        assertThat(info.getSeverity()).isEqualTo(Severity.INFO);
    }

    private Issue issue(String filePath, Severity severity) {
        return Issue.builder()
                .filePath(filePath)
                .severity(severity)
                .type(IssueType.BUG)
                .title("test")
                .source(IssueSource.LLM)
                .build();
    }

    private ImpactedNode impactedNode(String qn, String filePath, double risk) {
        return new ImpactedNode(qn, filePath, 1, 1.0, risk, Map.of());
    }

    private BlastRadiusResult blast(ImpactedNode... nodes) {
        Set<String> files = new java.util.LinkedHashSet<>();
        List<ImpactedNode> list = new java.util.ArrayList<>();
        for (ImpactedNode n : nodes) {
            list.add(n);
            if (n.filePath() != null) files.add(n.filePath());
        }
        return new BlastRadiusResult(list, files, false, 1);
    }
}
