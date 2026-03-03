package com.pullwise.api.application.service.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.domain.enums.PlanType;
import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.Subscription;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    private StripeService stripeService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        stripeService = new StripeService(organizationRepository, subscriptionRepository, objectMapper);
        // Don't set API key — Stripe not configured
        ReflectionTestUtils.setField(stripeService, "stripeApiKey", "");
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "");
        ReflectionTestUtils.setField(stripeService, "proPriceId", "price_test_pro");
        ReflectionTestUtils.setField(stripeService, "enterprisePriceId", "price_test_enterprise");
    }

    @Test
    void isConfigured_whenNoApiKey_shouldReturnFalse() {
        assertThat(stripeService.isConfigured()).isFalse();
    }

    @Test
    void createCheckoutSession_whenNotConfigured_shouldReturnEmpty() {
        Optional<String> result = stripeService.createCheckoutSession(1L, PlanType.PRO);
        assertThat(result).isEmpty();
    }

    @Test
    void createCustomerPortalSession_whenNotConfigured_shouldReturnEmpty() {
        Optional<String> result = stripeService.createCustomerPortalSession(1L);
        assertThat(result).isEmpty();
    }

    @Test
    void processWebhook_whenNotConfigured_shouldReturnFalse() {
        boolean result = stripeService.processWebhook("{}", "sig");
        assertThat(result).isFalse();
    }

    @Test
    void cancelSubscription_noActiveSubscription_shouldReturnFalse() {
        Organization org = new Organization();
        org.setId(1L);
        org.setName("TestOrg");

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(subscriptionRepository.findByOrganizationIdAndStatus(1L, "ACTIVE"))
                .thenReturn(Optional.empty());

        boolean result = stripeService.cancelSubscription(1L);
        assertThat(result).isFalse();
    }

    @Test
    void cancelSubscription_withActiveSubscription_noStripeId_shouldCancelLocally() {
        Organization org = new Organization();
        org.setId(1L);
        org.setName("TestOrg");
        org.setPlanType(PlanType.PRO);

        Subscription sub = new Subscription();
        sub.setId(1L);
        sub.setOrganization(org);
        sub.setStatus("ACTIVE");
        sub.setStripeSubscriptionId(null); // No Stripe ID

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(subscriptionRepository.findByOrganizationIdAndStatus(1L, "ACTIVE"))
                .thenReturn(Optional.of(sub));

        boolean result = stripeService.cancelSubscription(1L);

        assertThat(result).isTrue();
        assertThat(sub.getStatus()).isEqualTo("CANCELLED");
        assertThat(org.getPlanType()).isEqualTo(PlanType.FREE);

        verify(subscriptionRepository).save(sub);
        verify(organizationRepository).save(org);
    }

    @Test
    void getMonthlyPrice_shouldReturnCorrectPrices() {
        assertThat(stripeService.getMonthlyPrice(PlanType.FREE)).isEqualTo(0);
        assertThat(stripeService.getMonthlyPrice(PlanType.PRO)).isEqualTo(24.0);
        assertThat(stripeService.getMonthlyPrice(PlanType.ENTERPRISE)).isEqualTo(299.0);
    }

    @Test
    void getAnnualPrice_shouldApply20PercentDiscount() {
        double proAnnual = stripeService.getAnnualPrice(PlanType.PRO);
        assertThat(proAnnual).isEqualTo(24.0 * 12 * 0.8);

        double enterpriseAnnual = stripeService.getAnnualPrice(PlanType.ENTERPRISE);
        assertThat(enterpriseAnnual).isEqualTo(299.0 * 12 * 0.8);
    }
}
