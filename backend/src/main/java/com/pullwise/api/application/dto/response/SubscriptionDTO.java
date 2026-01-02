package com.pullwise.api.application.dto.response;

import com.pullwise.api.domain.model.Subscription;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para resposta de Subscription.
 */
@Builder
public record SubscriptionDTO(
        Long id,
        Long organizationId,
        String organizationName,
        String planType,
        String status,
        String stripeSubscriptionId,
        String stripeCustomerId,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        Boolean cancelAtPeriodEnd,
        LocalDate trialStart,
        LocalDate trialEnd,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SubscriptionDTO from(Subscription subscription) {
        return SubscriptionDTO.builder()
                .id(subscription.getId())
                .organizationId(subscription.getOrganization().getId())
                .organizationName(subscription.getOrganization().getName())
                .planType(subscription.getPlanType().name())
                .status(subscription.getStatus())
                .stripeSubscriptionId(subscription.getStripeSubscriptionId())
                .stripeCustomerId(subscription.getStripeCustomerId())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .cancelAtPeriodEnd(subscription.getCancelAtPeriodEnd())
                .trialStart(subscription.getTrialStart())
                .trialEnd(subscription.getTrialEnd())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}
