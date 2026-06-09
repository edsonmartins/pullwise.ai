package com.pullwise.api.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.dto.request.ConformanceReviewRequest;
import com.pullwise.api.application.dto.request.ConformanceReviewRequest.CriterionDTO;
import com.pullwise.api.application.dto.response.ConformanceVerdictDTO;
import com.pullwise.api.application.service.integration.OpenRouterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConformanceServiceTest {

    private OpenRouterService openRouterService;
    private ConformanceService service;

    @BeforeEach
    void setUp() {
        openRouterService = mock(OpenRouterService.class);
        service = new ConformanceService(openRouterService, new ObjectMapper());
    }

    private ConformanceReviewRequest request() {
        return new ConformanceReviewRequest("diff --git a b", List.of(new CriterionDTO("C1", "when", "then")));
    }

    @Test
    void mapsDivergesFromLlmJson() {
        when(openRouterService.analyzeWithOpenRouter(anyString())).thenReturn(
                "{\"diverges\":true,\"summary\":\"missing redirect\"," +
                        "\"criteria\":[{\"name\":\"C1\",\"ok\":false,\"note\":\"not handled\"}]}");

        ConformanceVerdictDTO verdict = service.review(request());

        assertThat(verdict.diverges()).isTrue();
        assertThat(verdict.summary()).contains("missing redirect");
        assertThat(verdict.criteria()).hasSize(1);
        assertThat(verdict.criteria().get(0).ok()).isFalse();
    }

    @Test
    void mapsConformantFromLlmJson() {
        when(openRouterService.analyzeWithOpenRouter(anyString())).thenReturn(
                "Sure! {\"diverges\":false,\"summary\":\"ok\",\"criteria\":[{\"name\":\"C1\",\"ok\":true,\"note\":\"\"}]}");

        ConformanceVerdictDTO verdict = service.review(request());

        assertThat(verdict.diverges()).isFalse();
    }

    @Test
    void failsOpenWhenLlmUnavailable() {
        when(openRouterService.analyzeWithOpenRouter(anyString())).thenReturn("");

        ConformanceVerdictDTO verdict = service.review(request());

        assertThat(verdict.diverges()).isFalse();
        assertThat(verdict.summary()).contains("unavailable");
        assertThat(verdict.criteria().get(0).ok()).isTrue();
    }

    @Test
    void failsOpenWhenOutputUnparseable() {
        when(openRouterService.analyzeWithOpenRouter(anyString())).thenReturn("not json at all");

        ConformanceVerdictDTO verdict = service.review(request());

        assertThat(verdict.diverges()).isFalse();
        assertThat(verdict.summary()).contains("parse");
    }
}
