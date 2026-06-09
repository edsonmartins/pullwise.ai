package com.pullwise.api.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.dto.request.ConformanceReviewRequest;
import com.pullwise.api.application.dto.response.ConformanceVerdictDTO;
import com.pullwise.api.application.dto.response.ConformanceVerdictDTO.CriterionResultDTO;
import com.pullwise.api.application.service.integration.OpenRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stateless conformance review: asks the LLM whether a diff satisfies each acceptance criterion
 * (WHEN/THEN) and returns a structured verdict. Fail-open: if the LLM is unavailable or its output
 * cannot be parsed, returns {@code diverges=false} (the caller's scenario↔test coverage is the hard
 * gate; the semantic review must not block delivery on an infra hiccup).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConformanceService {

    private final OpenRouterService openRouterService;
    private final ObjectMapper objectMapper;

    public ConformanceVerdictDTO review(ConformanceReviewRequest request) {
        String raw = openRouterService.analyzeWithOpenRouter(buildPrompt(request));
        if (raw == null || raw.isBlank()) {
            return failOpen(request, "conformance reviewer unavailable (no LLM response)");
        }
        String json = extractJson(raw);
        if (json == null) {
            return failOpen(request, "could not parse reviewer output");
        }
        try {
            Parsed parsed = objectMapper.readValue(json, Parsed.class);
            List<CriterionResultDTO> results = parsed.criteria() == null ? List.of()
                    : parsed.criteria().stream()
                    .map(c -> new CriterionResultDTO(c.name(), c.ok(), c.note()))
                    .toList();
            return new ConformanceVerdictDTO(parsed.diverges(), parsed.summary(), results);
        } catch (Exception e) {
            log.warn("Conformance review parse failed: {}", e.getMessage());
            return failOpen(request, "could not parse reviewer output");
        }
    }

    private String buildPrompt(ConformanceReviewRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a strict QA conformance reviewer. Decide whether the code change (unified ")
                .append("diff) satisfies EACH acceptance criterion, written as WHEN/THEN.\n")
                .append("Respond with ONLY a JSON object, no prose, in this exact shape:\n")
                .append("{\"diverges\": <true if ANY criterion is not satisfied>, ")
                .append("\"summary\": \"<one short sentence>\", ")
                .append("\"criteria\": [{\"name\": \"<name>\", \"ok\": <bool>, \"note\": \"<why>\"}]}\n\n")
                .append("Acceptance criteria:\n");
        int i = 1;
        for (ConformanceReviewRequest.CriterionDTO c : request.criteria()) {
            sb.append(i++).append(". ").append(c.name())
                    .append("\n   WHEN ").append(c.when())
                    .append("\n   THEN ").append(c.then()).append("\n");
        }
        sb.append("\nUnified diff under review:\n```diff\n").append(request.gitDiff()).append("\n```\n");
        return sb.toString();
    }

    /** Extract the outermost JSON object from a possibly-noisy LLM response. */
    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        return (start >= 0 && end > start) ? response.substring(start, end + 1) : null;
    }

    private ConformanceVerdictDTO failOpen(ConformanceReviewRequest request, String reason) {
        List<CriterionResultDTO> results = request.criteria().stream()
                .map(c -> new CriterionResultDTO(c.name(), true, reason))
                .toList();
        return new ConformanceVerdictDTO(false, reason, results);
    }

    private record Parsed(boolean diverges, String summary, List<Criterion> criteria) {
        private record Criterion(String name, boolean ok, String note) {}
    }
}
