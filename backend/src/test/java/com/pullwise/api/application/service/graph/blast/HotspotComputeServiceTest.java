package com.pullwise.api.application.service.graph.blast;

import com.pullwise.api.domain.repository.CodeGraphNodeRepository;
import com.pullwise.api.domain.repository.IssueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotspotComputeServiceTest {

    private static final Long PROJECT_ID = 99L;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private CodeGraphNodeRepository nodeRepository;

    @InjectMocks
    private HotspotComputeService service;

    @Test
    void noIssues_doesNothing() {
        when(issueRepository.countByProjectIdGroupedByFilePathSince(eq(PROJECT_ID), any(LocalDateTime.class)))
                .thenReturn(List.of());

        var stats = service.recompute(PROJECT_ID);

        assertThat(stats.filesUpdated()).isZero();
        verify(nodeRepository, never()).updateHotspotScoreByFile(anyLong(), anyString(), anyDouble());
    }

    @Test
    void normalizesScoresToZeroOneRange() {
        when(issueRepository.countByProjectIdGroupedByFilePathSince(eq(PROJECT_ID), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        new Object[]{"/src/Hot.java", 50L},
                        new Object[]{"/src/Mid.java", 7L},
                        new Object[]{"/src/Cold.java", 1L}
                ));
        when(nodeRepository.updateHotspotScoreByFile(eq(PROJECT_ID), anyString(), anyDouble()))
                .thenReturn(3); // 3 nodes per file affected

        var stats = service.recompute(PROJECT_ID);

        ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<String> fileCaptor = ArgumentCaptor.forClass(String.class);
        verify(nodeRepository, times(3)).updateHotspotScoreByFile(eq(PROJECT_ID),
                fileCaptor.capture(), scoreCaptor.capture());

        // Maior count → score 1.0; menores count → fração entre 0 e 1
        var scores = scoreCaptor.getAllValues();
        assertThat(scores).allMatch(s -> s >= 0.0 && s <= 1.0);
        // Hot.java tem max count → score deve ser 1.0
        int hotIdx = fileCaptor.getAllValues().indexOf("/src/Hot.java");
        assertThat(scores.get(hotIdx)).isEqualTo(1.0);

        assertThat(stats.filesUpdated()).isEqualTo(3);
        assertThat(stats.nodesAffected()).isEqualTo(9);
        assertThat(stats.maxIssuesPerFile()).isEqualTo(50L);
    }

    @Test
    void invalidArgs_returnsEmpty() {
        assertThat(service.recompute(null).filesUpdated()).isZero();
        assertThat(service.recompute(PROJECT_ID, 0).filesUpdated()).isZero();
        assertThat(service.recompute(PROJECT_ID, -1).filesUpdated()).isZero();
    }
}
