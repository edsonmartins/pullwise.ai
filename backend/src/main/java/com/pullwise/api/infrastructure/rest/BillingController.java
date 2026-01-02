package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.response.SubscriptionDTO;
import com.pullwise.api.application.service.billing.StripeService;
import com.pullwise.api.domain.enums.PlanType;
import com.pullwise.api.domain.model.Subscription;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para gestão de Billing e Assinaturas.
 */
@Slf4j
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final StripeService stripeService;
    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Lista preços disponíveis.
     */
    @GetMapping("/prices")
    public ResponseEntity<Map<String, Object>> getPrices() {
        Map<String, Object> prices = new HashMap<>();
        prices.put("free", Map.of(
                "monthly", 0,
                "annual", 0,
                "repos", 3,
                "reviewsPerMonth", 50
        ));
        prices.put("pro", Map.of(
                "monthly", stripeService.getMonthlyPrice(PlanType.PRO),
                "annual", stripeService.getAnnualPrice(PlanType.PRO),
                "repos", -1,
                "reviewsPerMonth", -1
        ));
        prices.put("enterprise", Map.of(
                "monthly", stripeService.getMonthlyPrice(PlanType.ENTERPRISE),
                "annual", stripeService.getAnnualPrice(PlanType.ENTERPRISE),
                "repos", -1,
                "reviewsPerMonth", -1
        ));
        return ResponseEntity.ok(prices);
    }

    /**
     * Obtém subscription atual da organização.
     */
    @GetMapping("/organizations/{organizationId}/subscription")
    public ResponseEntity<SubscriptionDTO> getSubscription(@PathVariable Long organizationId) {
        return subscriptionRepository
                .findByOrganizationIdAndStatus(organizationId, "ACTIVE")
                .map(sub -> ResponseEntity.ok(SubscriptionDTO.from(sub)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Inicia checkout para upgrade de plano.
     */
    @PostMapping("/organizations/{organizationId}/checkout")
    public ResponseEntity<Map<String, String>> startCheckout(
            @PathVariable Long organizationId,
            @RequestParam PlanType plan) {

        if (plan == PlanType.FREE) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot checkout for FREE plan"));
        }

        return stripeService.createCheckoutSession(organizationId, plan)
                .map(url -> ResponseEntity.ok(Map.of("checkoutUrl", url)))
                .orElse(ResponseEntity.status(500).body(Map.of("error", "Stripe not configured")));
    }

    /**
     * Cria sessão do portal do cliente.
     */
    @PostMapping("/organizations/{organizationId}/portal")
    public ResponseEntity<Map<String, String>> createPortalSession(@PathVariable Long organizationId) {
        return stripeService.createCustomerPortalSession(organizationId)
                .map(url -> ResponseEntity.ok(Map.of("portalUrl", url)))
                .orElse(ResponseEntity.status(500).body(Map.of("error", "Stripe not configured")));
    }

    /**
     * Cancela subscription da organização.
     */
    @PostMapping("/organizations/{organizationId}/cancel")
    public ResponseEntity<Void> cancelSubscription(@PathVariable Long organizationId) {
        boolean cancelled = stripeService.cancelSubscription(organizationId);
        return cancelled ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    /**
     * Webhook do Stripe para receber eventos.
     */
    @PostMapping("/webhook/stripe")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        boolean processed = stripeService.processWebhook(payload, signature);
        return processed ? ResponseEntity.ok().build() : ResponseEntity.status(500).build();
    }
}
