package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.request.ConformanceReviewRequest;
import com.pullwise.api.application.dto.response.ConformanceVerdictDTO;
import com.pullwise.api.application.service.ConformanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Stateless conformance review for external orchestrators (e.g. SquadX Pass 5). Authenticated by a
 * shared service key in the {@code X-API-Key} header (path is permitAll; the key check lives here).
 */
@Slf4j
@RestController
@RequestMapping("/api/conformance")
@RequiredArgsConstructor
public class ConformanceController {

    private final ConformanceService conformanceService;

    @Value("${conformance.api-key:}")
    private String apiKey;

    @PostMapping("/review")
    public ResponseEntity<ConformanceVerdictDTO> review(
            @RequestHeader(value = "X-API-Key", required = false) String key,
            @Valid @RequestBody ConformanceReviewRequest request
    ) {
        if (apiKey != null && !apiKey.isBlank() && !apiKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(conformanceService.review(request));
    }
}
