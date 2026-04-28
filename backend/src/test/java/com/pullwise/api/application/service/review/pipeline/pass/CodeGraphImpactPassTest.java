package com.pullwise.api.application.service.review.pipeline.pass;

import com.pullwise.api.application.service.graph.blast.BlastRadiusOptions;
import com.pullwise.api.application.service.graph.blast.BlastRadiusResult;
import com.pullwise.api.application.service.graph.blast.BlastRadiusService;
import com.pullwise.api.application.service.graph.blast.ImpactedNode;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.review.pipeline.MultiPassReviewOrchestrator.PassResult;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeGraphImpactPassTest {

    @Mock
    private BlastRadiusService blastRadiusService;

    @InjectMocks
    private CodeGraphImpactPass pass;

    private PullRequest pullRequest;
    private Review review;

    @BeforeEach
    void setUp() {
        Project project = Project.builder().id(7L).name("repo").build();
        pullRequest = PullRequest.builder().id(1L).prNumber(42).project(project).build();
        review = Review.builder().id(1L).build();
    }

    @Test
    void noProject_returnsEmptyResult() {
        pullRequest.setProject(null);
        PassResult result = pass.execute(pullRequest, review, null, null,
                List.of(diff("/src/A.java")));
        assertThat(result.getIssues()).isEmpty();
        verify(blastRadiusService, never()).computeBlastRadius(any(), anyList(), any());
    }

    @Test
    void noChangedFiles_returnsEmpty() {
        PassResult result = pass.execute(pullRequest, review, null, null, List.of());
        assertThat(result.getIssues()).isEmpty();
        verify(blastRadiusService, never()).computeBlastRadius(any(), anyList(), any());
    }

    @Test
    void noHighRiskNodes_doesNotCreateIssue() {
        when(blastRadiusService.computeBlastRadius(eq(7L), anyList(), any(BlastRadiusOptions.class)))
                .thenReturn(blast(0.30, "/src/B.java"));

        PassResult result = pass.execute(pullRequest, review, null, null,
                List.of(diff("/src/A.java")));

        assertThat(result.getIssues()).isEmpty();
        assertThat(result.getMetadata()).containsKey(CodeGraphImpactPass.METADATA_BLAST_RADIUS);
    }

    @Test
    void highRiskNodes_createsAggregatedIssue() {
        when(blastRadiusService.computeBlastRadius(eq(7L), anyList(), any(BlastRadiusOptions.class)))
                .thenReturn(blast(0.85, "/src/B.java"));

        PassResult result = pass.execute(pullRequest, review, null, null,
                List.of(diff("/src/A.java")));

        assertThat(result.getIssues()).hasSize(1);
        var issue = result.getIssues().get(0);
        assertThat(issue.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(issue.getRuleId()).isEqualTo("CODE_GRAPH_IMPACT_V2");
        assertThat(issue.getFilePath()).isEqualTo("/src/A.java");
        assertThat(issue.getDescription()).contains("Top high-risk impacted symbols");
    }

    @Test
    void lowConfidenceDowngradesCriticalToHigh() {
        // riskScore alto mas propagatedConfidence baixo (caminho AMBIGUOUS)
        ImpactedNode node = new ImpactedNode("com.x.B", "/src/B.java", 1, 0.4, 0.85, Map.of());
        BlastRadiusResult b = new BlastRadiusResult(List.of(node), Set.of("/src/B.java"), false, 1);
        when(blastRadiusService.computeBlastRadius(eq(7L), anyList(), any(BlastRadiusOptions.class)))
                .thenReturn(b);

        PassResult result = pass.execute(pullRequest, review, null, null,
                List.of(diff("/src/A.java")));

        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).getSeverity()).isEqualTo(Severity.HIGH);
    }

    private GitHubService.FileDiff diff(String filename) {
        return new GitHubService.FileDiff(filename, "modified", 10, 5, "@@ ... @@");
    }

    private BlastRadiusResult blast(double riskScore, String filePath) {
        ImpactedNode node = new ImpactedNode("com.x.B", filePath, 1, 1.0, riskScore, Map.of());
        return new BlastRadiusResult(List.of(node), Set.of(filePath), false, 1);
    }
}
