package com.pullwise.api.application.service.review.pipeline.synthesis;

import com.pullwise.api.application.service.graph.blast.BlastRadiusResult;
import com.pullwise.api.application.service.graph.blast.ImpactedNode;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IssuePrioritizerTest {

    private final IssuePrioritizer prioritizer = new IssuePrioritizer();

    @Test
    void emptyInput_returnsEmpty() {
        assertThat(prioritizer.prioritize(null, null)).isEmpty();
        assertThat(prioritizer.prioritize(List.of(), null)).isEmpty();
    }

    @Test
    void higherSeverityWins_whenNoBlastRadius() {
        Issue critical = issue("/a.java", Severity.CRITICAL);
        Issue medium = issue("/b.java", Severity.MEDIUM);
        Issue low = issue("/c.java", Severity.LOW);

        List<Issue> sorted = prioritizer.prioritize(List.of(low, medium, critical), null);
        assertThat(sorted).containsExactly(critical, medium, low);
    }

    @Test
    void blastRadiusBoostsLowSeverityOnImpactedFile() {
        // Issue HIGH em arquivo SEM blast radius
        Issue highOnSafeFile = issue("/safe.java", Severity.HIGH);
        // Issue MEDIUM em arquivo COM alto blast radius
        Issue mediumOnImpacted = issue("/impacted.java", Severity.MEDIUM);

        BlastRadiusResult blast = blast(impacted("/impacted.java", 1.0));

        List<Issue> sorted = prioritizer.prioritize(List.of(highOnSafeFile, mediumOnImpacted), blast);

        // priority(HIGH only) = 0.75*0.6 = 0.45
        // priority(MEDIUM + risk 1.0) = 0.5*0.6 + 1.0*0.4 = 0.70
        // → mediumOnImpacted vem primeiro
        assertThat(sorted).containsExactly(mediumOnImpacted, highOnSafeFile);
    }

    @Test
    void stableOrderForTies() {
        Issue first = issue("/a.java", Severity.HIGH);
        Issue second = issue("/b.java", Severity.HIGH);
        Issue third = issue("/c.java", Severity.HIGH);

        List<Issue> sorted = prioritizer.prioritize(List.of(first, second, third), null);
        // Sem blast e severidades iguais → ordem original preservada
        assertThat(sorted).containsExactly(first, second, third);
    }

    @Test
    void infoIssuesGoLast() {
        Issue info = issue("/a.java", Severity.INFO);
        Issue low = issue("/b.java", Severity.LOW);

        List<Issue> sorted = prioritizer.prioritize(List.of(info, low), null);
        assertThat(sorted).containsExactly(low, info);
    }

    @Test
    void doesNotMutateInputList() {
        Issue critical = issue("/a.java", Severity.CRITICAL);
        Issue low = issue("/b.java", Severity.LOW);
        List<Issue> input = new ArrayList<>(List.of(low, critical));

        List<Issue> sorted = prioritizer.prioritize(input, null);

        assertThat(input).containsExactly(low, critical); // input intocado
        assertThat(sorted).containsExactly(critical, low);
    }

    private Issue issue(String filePath, Severity severity) {
        return Issue.builder()
                .filePath(filePath)
                .severity(severity)
                .type(IssueType.BUG)
                .title("t")
                .source(IssueSource.LLM)
                .build();
    }

    private ImpactedNode impacted(String filePath, double risk) {
        return new ImpactedNode("qn", filePath, 1, 1.0, risk, Map.of());
    }

    private BlastRadiusResult blast(ImpactedNode... nodes) {
        return new BlastRadiusResult(List.of(nodes),
                java.util.Arrays.stream(nodes).map(ImpactedNode::filePath)
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)),
                false, 1);
    }
}
