package com.pullwise.api.application.service.review;

import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.model.ReviewCoverage;
import com.pullwise.api.domain.repository.ReviewCoverageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço de rastreamento de cobertura de review.
 * Calcula qual porcentagem das mudanças de um PR foram efetivamente revisadas.
 *
 * <p>Uma linha é considerada "revisada" quando:
 * <ul>
 *   <li>O LLM ou SAST gerou issue apontando para aquele arquivo</li>
 *   <li>O arquivo foi analisado (mesmo sem issues — análise implica cobertura)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoverageTrackingService {

    private final ReviewCoverageRepository coverageRepository;

    /**
     * Atualiza a cobertura de review com base nos diffs analisados e issues encontradas.
     * Chamado após a consolidação de issues no ReviewOrchestrator.
     */
    @Transactional
    public void updateCoverage(Review review, List<GitHubService.FileDiff> diffs, List<Issue> issues) {
        // Agrupar issues por arquivo
        Map<String, List<Issue>> issuesByFile = issues.stream()
                .filter(i -> i.getFilePath() != null)
                .collect(Collectors.groupingBy(Issue::getFilePath));

        BigDecimal totalCoverage = BigDecimal.ZERO;
        int filesWithCoverage = 0;

        for (GitHubService.FileDiff diff : diffs) {
            int totalLines = diff.additions() + diff.deletions();
            if (totalLines == 0) continue;

            ReviewCoverage coverage = coverageRepository
                    .findByReviewIdAndFilePath(review.getId(), diff.filename())
                    .orElseGet(() -> ReviewCoverage.builder()
                            .review(review)
                            .filePath(diff.filename())
                            .totalLinesChanged(totalLines)
                            .build());

            // Atualizar total de linhas (pode mudar em re-reviews)
            coverage.setTotalLinesChanged(totalLines);

            // Calcular linhas revisadas
            // Se o arquivo teve issues, todas as linhas alteradas com issues são consideradas revisadas
            // Se o arquivo foi analisado sem issues, consideramos cobertura parcial (análise sem achados)
            List<Issue> fileIssues = issuesByFile.get(diff.filename());
            if (fileIssues != null && !fileIssues.isEmpty()) {
                // Arquivo com issues = linhas onde há issue são 100% revisadas
                // Estimativa: cada issue cobre ~10 linhas de contexto
                int coveredByIssues = Math.min(fileIssues.size() * 10, totalLines);
                coverage.addReviewedLines(coveredByIssues);
            } else {
                // Arquivo analisado sem issues = 70% cobertura (revisou, nada encontrado)
                int implicitCoverage = (int) (totalLines * 0.7);
                coverage.addReviewedLines(implicitCoverage);
            }

            coverageRepository.save(coverage);
            totalCoverage = totalCoverage.add(coverage.getCoveragePercentage());
            filesWithCoverage++;
        }

        // Atualizar cobertura média no review
        if (filesWithCoverage > 0) {
            BigDecimal avg = totalCoverage.divide(
                    BigDecimal.valueOf(filesWithCoverage), 2, java.math.RoundingMode.HALF_UP);
            review.setCoveragePercentage(avg);
            log.info("Review {} coverage: {}% across {} files", review.getId(), avg, filesWithCoverage);
        }
    }

    /**
     * Retorna a cobertura detalhada de um review.
     */
    public CoverageReport getCoverageReport(Long reviewId) {
        List<ReviewCoverage> coverages = coverageRepository.findByReviewId(reviewId);
        BigDecimal avgCoverage = coverageRepository.calculateAverageCoverage(reviewId);
        long reviewedFiles = coverageRepository.countReviewedFiles(reviewId);
        long totalFiles = coverageRepository.countTotalFiles(reviewId);

        List<FileCoverage> fileCoverages = coverages.stream()
                .map(c -> new FileCoverage(
                        c.getFilePath(),
                        c.getTotalLinesChanged(),
                        c.getLinesReviewed(),
                        c.getCoveragePercentage(),
                        c.getFirstReviewedAt(),
                        c.getLastReviewedAt()
                ))
                .toList();

        return new CoverageReport(
                reviewId,
                avgCoverage,
                totalFiles,
                reviewedFiles,
                fileCoverages
        );
    }

    public record CoverageReport(
            Long reviewId,
            BigDecimal averageCoverage,
            long totalFiles,
            long reviewedFiles,
            List<FileCoverage> files
    ) {}

    public record FileCoverage(
            String filePath,
            int totalLinesChanged,
            int linesReviewed,
            BigDecimal coveragePercentage,
            java.time.LocalDateTime firstReviewedAt,
            java.time.LocalDateTime lastReviewedAt
    ) {}
}
