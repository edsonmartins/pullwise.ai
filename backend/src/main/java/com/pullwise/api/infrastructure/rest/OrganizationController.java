package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.response.OrganizationDTO;
import com.pullwise.api.application.dto.response.ProjectDTO;
import com.pullwise.api.application.dto.response.UsageStatsDTO;
import com.pullwise.api.application.service.auth.AuthorizationService;
import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.UsageRecord;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para gerenciamento de Organizações.
 */
@Slf4j
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final AuthorizationService authorizationService;

    /**
     * Lista organizações do usuário autenticado.
     */
    @GetMapping
    public ResponseEntity<List<OrganizationDTO>> listOrganizations(Principal principal) {
        Long userId = authorizationService.getUserId(principal);
        List<Long> orgIds = authorizationService.getUserOrganizationIds(userId);

        // Batch queries to avoid N+1 (single query for repo counts + review counts)
        String period = LocalDate.now().format(PERIOD_FORMATTER);

        Map<Long, Long> repoCounts = new HashMap<>();
        for (Object[] row : projectRepository.countActiveByOrganizationIds(orgIds)) {
            repoCounts.put((Long) row[0], (Long) row[1]);
        }

        Map<Long, Long> reviewCounts = new HashMap<>();
        for (Object[] row : usageRecordRepository.sumByOrganizationIdsAndPeriodAndMetricType(orgIds, period, UsageRecord.METRIC_REVIEWS)) {
            reviewCounts.put((Long) row[0], (Long) row[1]);
        }

        List<OrganizationDTO> dtos = organizationRepository.findAllById(orgIds).stream()
                .map(org -> OrganizationDTO.from(org,
                        repoCounts.getOrDefault(org.getId(), 0L).intValue(),
                        reviewCounts.getOrDefault(org.getId(), 0L).intValue()))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Busca uma organização por ID (verifica membership).
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDTO> getOrganization(@PathVariable Long id, Principal principal) {
        Long userId = authorizationService.getUserId(principal);
        authorizationService.requireOrganizationMember(userId, id);

        return organizationRepository.findById(id)
                .map(org -> {
                    long repoCount = projectRepository.countActiveByOrganizationId(org.getId());
                    String period = LocalDate.now().format(PERIOD_FORMATTER);
                    long reviewCount = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                            org.getId(), period, UsageRecord.METRIC_REVIEWS
                    );
                    return ResponseEntity.ok(OrganizationDTO.from(org, (int) repoCount, (int) reviewCount));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lista projetos de uma organização (verifica membership).
     */
    @GetMapping("/{id}/projects")
    public ResponseEntity<List<ProjectDTO>> getOrganizationProjects(@PathVariable Long id, Principal principal) {
        Long userId = authorizationService.getUserId(principal);
        authorizationService.requireOrganizationMember(userId, id);

        List<ProjectDTO> projects = projectRepository.findActiveByOrganizationId(id).stream()
                .map(ProjectDTO::from)
                .toList();
        return ResponseEntity.ok(projects);
    }

    /**
     * Retorna estatísticas de uso de uma organização (verifica membership).
     */
    @GetMapping("/{id}/usage")
    public ResponseEntity<UsageStatsDTO> getOrganizationUsage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "current") String period,
            Principal principal) {

        Long userId = authorizationService.getUserId(principal);
        authorizationService.requireOrganizationMember(userId, id);

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        String periodStr = period.equals("current")
                ? LocalDate.now().format(PERIOD_FORMATTER)
                : period;

        long totalReviews = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                id, periodStr, UsageRecord.METRIC_REVIEWS);
        long llmTokens = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                id, periodStr, UsageRecord.METRIC_LLM_TOKENS);
        long filesScanned = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                id, periodStr, UsageRecord.METRIC_FILES_SCANNED);
        long linesAnalyzed = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                id, periodStr, UsageRecord.METRIC_LINES_ANALYZED);

        boolean withinLimits = org.hasExceededReviewLimit((int) totalReviews);

        UsageStatsDTO stats = new UsageStatsDTO(
                id,
                periodStr,
                totalReviews,
                llmTokens,
                filesScanned,
                linesAnalyzed,
                null,
                withinLimits
        );

        return ResponseEntity.ok(stats);
    }
}
