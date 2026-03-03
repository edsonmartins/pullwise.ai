package com.pullwise.api.application.service.review;

import com.pullwise.api.application.service.integration.BitBucketService;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.GitLabService;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Platform;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.PullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostingServiceTest {

    private PostingService postingService;

    @Mock private GitHubService gitHubService;
    @Mock private BitBucketService bitBucketService;
    @Mock private GitLabService gitLabService;

    @BeforeEach
    void setUp() {
        postingService = new PostingService(gitHubService, bitBucketService, gitLabService);
        ReflectionTestUtils.setField(postingService, "postAsComment", true);
        ReflectionTestUtils.setField(postingService, "includeSummary", true);
    }

    @Test
    void postReviewComment_github_shouldUseGitHubService() {
        Project project = new Project();
        project.setId(1L);

        PullRequest pr = new PullRequest();
        pr.setId(1L);
        pr.setPrNumber(42);
        pr.setPlatform(Platform.GITHUB);
        pr.setProject(project);

        Issue issue = Issue.builder()
                .severity(Severity.HIGH)
                .type(IssueType.BUG)
                .source(IssueSource.PMD)
                .title("Null pointer risk")
                .description("Potential NPE at line 10")
                .filePath("src/Main.java")
                .lineStart(10)
                .build();

        when(gitHubService.postPullRequestComment(any(), eq(42), anyString()))
                .thenReturn("comment-123");

        String commentId = postingService.postReviewComment(pr, List.of(issue));

        assertThat(commentId).isEqualTo("comment-123");
        verify(gitHubService).postPullRequestComment(eq(project), eq(42), anyString());
        verifyNoInteractions(bitBucketService);
    }

    @Test
    void postReviewComment_bitbucket_shouldUseBitBucketService() {
        Project project = new Project();
        project.setId(1L);

        PullRequest pr = new PullRequest();
        pr.setId(1L);
        pr.setPrNumber(7);
        pr.setPlatform(Platform.BITBUCKET);
        pr.setProject(project);

        when(bitBucketService.postReviewComment(any(), eq(7L), anyString()))
                .thenReturn("bb-comment-456");

        String commentId = postingService.postReviewComment(pr, List.of());

        assertThat(commentId).isEqualTo("bb-comment-456");
        verify(bitBucketService).postReviewComment(eq(project), eq(7L), anyString());
        verifyNoInteractions(gitHubService);
    }

    @Test
    void postReviewComment_postingDisabled_shouldReturnNull() {
        ReflectionTestUtils.setField(postingService, "postAsComment", false);

        PullRequest pr = new PullRequest();
        pr.setPrNumber(1);

        String commentId = postingService.postReviewComment(pr, List.of());

        assertThat(commentId).isNull();
        verifyNoInteractions(gitHubService, bitBucketService);
    }

    @Test
    void postReviewComment_commentShouldContainSummaryTable() {
        Project project = new Project();
        PullRequest pr = new PullRequest();
        pr.setPrNumber(1);
        pr.setPlatform(Platform.GITHUB);
        pr.setProject(project);

        Issue critical = Issue.builder()
                .severity(Severity.CRITICAL).type(IssueType.SECURITY)
                .source(IssueSource.LLM).title("SQL Injection").description("Unsafe query").build();
        Issue low = Issue.builder()
                .severity(Severity.LOW).type(IssueType.CODE_SMELL)
                .source(IssueSource.CHECKSTYLE).title("Missing javadoc").description("Add javadoc").build();

        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
        when(gitHubService.postPullRequestComment(any(), anyInt(), commentCaptor.capture()))
                .thenReturn("id");

        postingService.postReviewComment(pr, List.of(critical, low));

        String comment = commentCaptor.getValue();
        assertThat(comment).contains("Pullwise Review Report");
        assertThat(comment).contains("Critical");
        assertThat(comment).contains("SQL Injection");
        assertThat(comment).contains("pullwise.ai");
    }
}
