package com.pullwise.api.domain.enums;

import lombok.Getter;

/**
 * Tipos de planos de assinatura disponíveis no Pullwise.
 */
@Getter
public enum PlanType {
    /**
     * Plano gratuito - limitado a 3 repositórios e 50 reviews/mês
     */
    FREE("free", "Plano Gratuito", 3, 50),

    /**
     * Plano Pro - 20 repositórios e 500 reviews/mês
     */
    PRO("pro", "Plano Pro", 20, 500),

    /**
     * Plano Enterprise - repositórios ilimitados e reviews ilimitados
     */
    ENTERPRISE("enterprise", "Plano Enterprise", -1, -1);

    private final String code;
    private final String displayName;
    private final int maxRepositories;
    private final int maxReviewsPerMonth;

    PlanType(String code, String displayName, int maxRepositories, int maxReviewsPerMonth) {
        this.code = code;
        this.displayName = displayName;
        this.maxRepositories = maxRepositories;
        this.maxReviewsPerMonth = maxReviewsPerMonth;
    }

    public boolean hasUnlimitedRepositories() {
        return maxRepositories == -1;
    }

    public boolean hasUnlimitedReviews() {
        return maxReviewsPerMonth == -1;
    }

    public static PlanType fromCode(String code) {
        for (PlanType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown plan type: " + code);
    }
}
