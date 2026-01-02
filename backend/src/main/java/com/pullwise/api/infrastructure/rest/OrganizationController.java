package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.response.OrganizationDTO;
import com.pullwise.api.application.dto.response.ProjectDTO;
import com.pullwise.api.application.dto.response.UsageStatsDTO;
import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.UsageRecord;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller REST para gerenciamento de Organizações.
 */
@Slf4j
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final UsageRecordRepository usageRecordRepository;

    /**
     * Lista organizações do usuário.
     */
    @GetMapping
    public ResponseEntity<List<OrganizationDTO>> listOrganizations() {
        List<OrganizationDTO> dtos = organizationRepository.findAll().stream()
                .map(org -> {
                    long repoCount = projectRepository.countActiveByOrganizationId(org.getId());
                    String period = LocalDate.now().toString().substring(0, 7);
                    long reviewCount = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                            org.getId(), period, UsageRecord.METRIC_REVIEWS
                    );
                    return OrganizationDTO.from(org, (int) repoCount, (int) reviewCount);
                })
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Busca uma organização por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDTO> getOrganization(@PathVariable Long id) {
        return organizationRepository.findById(id)
                .map(org -> {
                    long repoCount = projectRepository.countActiveByOrganizationId(org.getId());
                    String period = LocalDate.now().toString().substring(0, 7);
                    long reviewCount = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                            org.getId(), period, UsageRecord.METRIC_REVIEWS
                    );
                    return ResponseEntity.ok(OrganizationDTO.from(org, (int) repoCount, (int) reviewCount));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lista projetos de uma organização.
     */
    @GetMapping("/{id}/projects")
    public ResponseEntity<List<ProjectDTO>> getOrganizationProjects(@PathVariable Long id) {
        List<ProjectDTO> projects = projectRepository.findActiveByOrganizationId(id).stream()
                .map(ProjectDTO::from)
                .toList();
        return ResponseEntity.ok(projects);
    }

    /**
     * Retorna estatísticas de uso de uma organização.
     */
    @GetMapping("/{id}/usage")
    public ResponseEntity<UsageStatsDTO> getOrganizationUsage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "current") String period) {

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        String periodStr = period.equals("current")
                ? LocalDate.now().toString().substring(0, 7)
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
