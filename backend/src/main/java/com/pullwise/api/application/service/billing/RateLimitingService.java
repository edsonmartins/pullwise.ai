package com.pullwise.api.application.service.billing;

import com.pullwise.api.domain.enums.PlanType;
import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.UsageRecord;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Serviço de rate limiting baseado no plano da organização.
 * Verifica se a organização excedeu seus limites de uso antes de permitir ações.
 *
 * <p>Limites por plano:
 * <ul>
 *   <li>FREE: 50 reviews/mês, 3 repositórios</li>
 *   <li>PRO: 500 reviews/mês, 20 repositórios</li>
 *   <li>ENTERPRISE: ilimitado</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final UsageRecordRepository usageRecordRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Verifica se a organização pode criar um novo review.
     *
     * @param organizationId ID da organização
     * @throws RateLimitExceededException se o limite foi excedido
     */
    public void checkReviewLimit(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        PlanType planType = org.getPlanType();
        if (planType == null) {
            planType = PlanType.FREE;
        }

        if (planType.hasUnlimitedReviews()) {
            return; // ENTERPRISE — sem limites
        }

        String currentPeriod = LocalDate.now().toString().substring(0, 7); // YYYY-MM
        long currentUsage = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                organizationId, currentPeriod, UsageRecord.METRIC_REVIEWS);

        int limit = planType.getMaxReviewsPerMonth();

        if (currentUsage >= limit) {
            log.warn("Rate limit exceeded for organization {} (plan: {}, usage: {}/{})",
                    org.getName(), planType, currentUsage, limit);
            throw new RateLimitExceededException(
                    String.format("Monthly review limit exceeded (%d/%d). Upgrade your plan for more reviews.",
                            currentUsage, limit),
                    planType, currentUsage, limit
            );
        }

        log.debug("Rate limit check passed for organization {} ({}/{})",
                organizationId, currentUsage, limit);
    }

    /**
     * Verifica se a organização pode adicionar um novo repositório.
     *
     * @param organizationId ID da organização
     * @param currentRepoCount Número atual de repositórios
     * @throws RateLimitExceededException se o limite foi excedido
     */
    public void checkRepositoryLimit(Long organizationId, int currentRepoCount) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        PlanType planType = org.getPlanType();
        if (planType == null) {
            planType = PlanType.FREE;
        }

        if (planType.hasUnlimitedRepositories()) {
            return;
        }

        int limit = planType.getMaxRepositories();

        if (currentRepoCount >= limit) {
            log.warn("Repository limit exceeded for organization {} (plan: {}, count: {}/{})",
                    org.getName(), planType, currentRepoCount, limit);
            throw new RateLimitExceededException(
                    String.format("Repository limit exceeded (%d/%d). Upgrade your plan to add more repositories.",
                            currentRepoCount, limit),
                    planType, currentRepoCount, limit
            );
        }
    }

    /**
     * Retorna o uso atual de reviews da organização no período atual.
     */
    public UsageInfo getReviewUsage(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId).orElse(null);
        if (org == null) {
            return new UsageInfo(0, 0, PlanType.FREE);
        }

        PlanType planType = org.getPlanType() != null ? org.getPlanType() : PlanType.FREE;
        String currentPeriod = LocalDate.now().toString().substring(0, 7);

        long currentUsage = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                organizationId, currentPeriod, UsageRecord.METRIC_REVIEWS);

        int limit = planType.hasUnlimitedReviews() ? -1 : planType.getMaxReviewsPerMonth();

        return new UsageInfo(currentUsage, limit, planType);
    }

    /**
     * Informações de uso para resposta da API.
     */
    public record UsageInfo(long current, int limit, PlanType planType) {
        public boolean isExceeded() {
            return limit >= 0 && current >= limit;
        }

        public double usagePercentage() {
            if (limit <= 0) return 0;
            return (double) current / limit * 100;
        }
    }

    /**
     * Exceção lançada quando o limite de uso foi excedido.
     */
    public static class RateLimitExceededException extends RuntimeException {
        private final PlanType planType;
        private final long currentUsage;
        private final int limit;

        public RateLimitExceededException(String message, PlanType planType, long currentUsage, int limit) {
            super(message);
            this.planType = planType;
            this.currentUsage = currentUsage;
            this.limit = limit;
        }

        public PlanType getPlanType() { return planType; }
        public long getCurrentUsage() { return currentUsage; }
        public int getLimit() { return limit; }
    }
}
