package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.request.ConformanceReviewRequest;
import com.pullwise.api.application.dto.request.ConformanceReviewRequest.CriterionDTO;
import com.pullwise.api.application.dto.response.ConformanceVerdictDTO;
import com.pullwise.api.application.service.ConformanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConformanceControllerTest {

    private ConformanceService service;
    private ConformanceController controller;

    @BeforeEach
    void setUp() {
        service = mock(ConformanceService.class);
        controller = new ConformanceController(service);
        ReflectionTestUtils.setField(controller, "apiKey", "secret");
    }

    private ConformanceReviewRequest request() {
        return new ConformanceReviewRequest("diff", List.of(new CriterionDTO("C1", "w", "t")));
    }

    @Test
    void rejectsWrongApiKey() {
        ResponseEntity<ConformanceVerdictDTO> response = controller.review("wrong", request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(service, never()).review(any());
    }

    @Test
    void delegatesWithValidApiKey() {
        ConformanceVerdictDTO verdict = new ConformanceVerdictDTO(false, "ok", List.of());
        when(service.review(any())).thenReturn(verdict);

        ResponseEntity<ConformanceVerdictDTO> response = controller.review("secret", request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(verdict);
    }
}
