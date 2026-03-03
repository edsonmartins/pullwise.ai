package com.pullwise.api.application.service.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.Subscription;
import com.pullwise.api.domain.enums.PlanType;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.SubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Serviço para integração com Stripe para gestão de assinaturas e pagamentos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${integrations.stripe.api-key:}")
    private String stripeApiKey;

    @Value("${integrations.stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${integrations.stripe.pro-price-id:price_1PMnvjGv5JTPxd}")
    private String proPriceId;

    @Value("${integrations.stripe.enterprise-price-id:price_1PMnvjGv5JTPxE}")
    private String enterprisePriceId;

    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            Stripe.apiKey = stripeApiKey;
            log.info("Stripe SDK initialized");
        } else {
            log.warn("Stripe API key not configured — billing features disabled");
        }
    }

    /**
     * Verifica se o Stripe está configurado.
     */
    public boolean isConfigured() {
        return stripeApiKey != null && !stripeApiKey.isBlank();
    }

    /**
     * Cria uma checkout session no Stripe para upgrade de plano.
     */
    public Optional<String> createCheckoutSession(Long organizationId, PlanType planType) {
        if (!isConfigured()) {
            log.warn("Stripe is not configured");
            return Optional.empty();
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        String priceId = getPriceIdForPlan(planType);
        if (priceId == null) {
            log.warn("No price ID configured for plan {}", planType);
            return Optional.empty();
        }

        String successUrl = buildSuccessUrl(organizationId);
        String cancelUrl = buildCancelUrl();

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .putMetadata("organization_id", organizationId.toString())
                    .putMetadata("plan_type", planType.name())
                    .build();

            Session session = Session.create(params);

            log.info("Created Stripe checkout session {} for organization {} upgrading to {}",
                    session.getId(), organization.getName(), planType);

            return Optional.of(session.getUrl());

        } catch (Exception e) {
            log.error("Error creating Stripe checkout session", e);
            return Optional.empty();
        }
    }

    /**
     * Cria um portal session para o cliente gerenciar a assinatura.
     */
    public Optional<String> createCustomerPortalSession(Long organizationId) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        // Buscar subscription ativa para obter customer ID
        Optional<Subscription> subscription = subscriptionRepository
                .findByOrganizationIdAndStatus(organizationId, "ACTIVE");

        if (subscription.isEmpty() || subscription.get().getStripeCustomerId() == null) {
            log.warn("No active subscription with Stripe customer for organization {}", organizationId);
            return Optional.empty();
        }

        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(subscription.get().getStripeCustomerId())
                            .setReturnUrl(buildSuccessUrl(organizationId))
                            .build();

            com.stripe.model.billingportal.Session portalSession =
                    com.stripe.model.billingportal.Session.create(params);

            log.info("Created Stripe portal session for organization {}", organization.getName());
            return Optional.of(portalSession.getUrl());

        } catch (Exception e) {
            log.error("Error creating Stripe portal session", e);
            return Optional.empty();
        }
    }

    /**
     * Processa webhook do Stripe com verificação de assinatura.
     */
    @Transactional
    public boolean processWebhook(String payload, String signature) {
        if (!isConfigured()) {
            log.warn("Received Stripe webhook but Stripe is not configured");
            return false;
        }

        try {
            // Verificar assinatura do webhook
            Event event;
            if (webhookSecret != null && !webhookSecret.isBlank()) {
                event = Webhook.constructEvent(payload, signature, webhookSecret);
            } else {
                log.warn("Stripe webhook secret not configured — skipping signature verification");
                event = Event.GSON.fromJson(payload, Event.class);
            }

            log.info("Processing Stripe webhook event: {} (type: {})", event.getId(), event.getType());

            switch (event.getType()) {
                case "checkout.session.completed" -> handleCheckoutCompleted(event);
                case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
                case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
                case "invoice.paid" -> handleInvoicePaid(event);
                default -> log.debug("Unhandled Stripe event type: {}", event.getType());
            }

            return true;

        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature", e);
            return false;
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return false;
        }
    }

    /**
     * Trata evento de checkout completado.
     * Cria/atualiza Subscription no banco.
     */
    private void handleCheckoutCompleted(Event event) {
        try {
            JsonNode data = objectMapper.readTree(event.getData().toJson());
            JsonNode sessionObj = data.get("object");

            String stripeSubscriptionId = sessionObj.has("subscription")
                    ? sessionObj.get("subscription").asText() : null;
            String stripeCustomerId = sessionObj.has("customer")
                    ? sessionObj.get("customer").asText() : null;

            JsonNode metadata = sessionObj.get("metadata");
            String organizationIdStr = metadata != null && metadata.has("organization_id")
                    ? metadata.get("organization_id").asText() : null;
            String planTypeStr = metadata != null && metadata.has("plan_type")
                    ? metadata.get("plan_type").asText() : null;

            if (organizationIdStr == null || stripeSubscriptionId == null) {
                log.warn("Checkout completed but missing organization_id or subscription_id");
                return;
            }

            Long organizationId = Long.parseLong(organizationIdStr);
            PlanType planType = planTypeStr != null ? PlanType.valueOf(planTypeStr) : PlanType.PRO;

            Organization org = organizationRepository.findById(organizationId).orElse(null);
            if (org == null) {
                log.warn("Organization {} not found for checkout session", organizationId);
                return;
            }

            // Criar ou atualizar subscription
            Subscription sub = subscriptionRepository
                    .findByOrganizationIdAndStatus(organizationId, "ACTIVE")
                    .orElse(Subscription.builder().organization(org).build());

            sub.setStripeSubscriptionId(stripeSubscriptionId);
            sub.setStripeCustomerId(stripeCustomerId);
            sub.setPlanType(planType);
            sub.setStatus("ACTIVE");
            sub.setCurrentPeriodStart(java.time.LocalDate.now());
            sub.setCurrentPeriodEnd(java.time.LocalDate.now().plusMonths(1));
            subscriptionRepository.save(sub);

            // Upgrade plano da organização
            org.setPlanType(planType);
            organizationRepository.save(org);

            log.info("Checkout completed — activated {} subscription for organization {}",
                    planType, org.getName());

        } catch (Exception e) {
            log.error("Failed to handle checkout.session.completed", e);
        }
    }

    /**
     * Trata evento de atualização de subscription.
     */
    private void handleSubscriptionUpdated(Event event) {
        try {
            JsonNode data = objectMapper.readTree(event.getData().toJson());
            JsonNode subObj = data.get("object");

            String stripeSubId = subObj.has("id") ? subObj.get("id").asText() : null;
            String status = subObj.has("status") ? subObj.get("status").asText() : null;
            boolean cancelAtPeriodEnd = subObj.has("cancel_at_period_end")
                    && subObj.get("cancel_at_period_end").asBoolean();

            if (stripeSubId == null) return;

            subscriptionRepository.findByStripeSubscriptionId(stripeSubId).ifPresent(sub -> {
                if (status != null) {
                    sub.setStatus(status.toUpperCase().equals("ACTIVE") ? "ACTIVE" : status.toUpperCase());
                }
                sub.setCancelAtPeriodEnd(cancelAtPeriodEnd);

                if (subObj.has("current_period_end")) {
                    long endEpoch = subObj.get("current_period_end").asLong();
                    sub.setCurrentPeriodEnd(
                            Instant.ofEpochSecond(endEpoch).atZone(ZoneId.systemDefault()).toLocalDate());
                }

                subscriptionRepository.save(sub);
                log.info("Subscription {} updated to status {}", stripeSubId, status);
            });

        } catch (Exception e) {
            log.error("Failed to handle customer.subscription.updated", e);
        }
    }

    /**
     * Trata evento de cancelamento de subscription.
     * BUGFIX: Cancela apenas a subscription específica, não todas.
     */
    private void handleSubscriptionDeleted(Event event) {
        try {
            JsonNode data = objectMapper.readTree(event.getData().toJson());
            JsonNode subObj = data.get("object");

            String stripeSubId = subObj.has("id") ? subObj.get("id").asText() : null;
            if (stripeSubId == null) return;

            subscriptionRepository.findByStripeSubscriptionId(stripeSubId).ifPresent(sub -> {
                sub.setStatus("CANCELLED");
                subscriptionRepository.save(sub);

                // Downgrade organização para FREE
                Organization org = sub.getOrganization();
                if (org != null) {
                    org.setPlanType(PlanType.FREE);
                    organizationRepository.save(org);
                    log.info("Subscription {} cancelled — downgraded organization {} to FREE",
                            stripeSubId, org.getName());
                }
            });

        } catch (Exception e) {
            log.error("Failed to handle customer.subscription.deleted", e);
        }
    }

    /**
     * Trata evento de invoice paga.
     */
    private void handleInvoicePaid(Event event) {
        try {
            JsonNode data = objectMapper.readTree(event.getData().toJson());
            JsonNode invoiceObj = data.get("object");
            String invoiceId = invoiceObj.has("id") ? invoiceObj.get("id").asText() : "unknown";
            String amountPaid = invoiceObj.has("amount_paid")
                    ? String.valueOf(invoiceObj.get("amount_paid").asLong()) : "0";
            log.info("Invoice {} paid: {} cents", invoiceId, amountPaid);
        } catch (Exception e) {
            log.error("Failed to handle invoice.paid", e);
        }
    }

    /**
     * Cancela uma subscription no Stripe e no banco.
     */
    @Transactional
    public boolean cancelSubscription(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        Optional<Subscription> subscription = subscriptionRepository
                .findByOrganizationIdAndStatus(organizationId, "ACTIVE");

        if (subscription.isEmpty()) {
            log.warn("No active subscription found for organization {}", organizationId);
            return false;
        }

        try {
            Subscription sub = subscription.get();

            // Cancelar no Stripe se tiver subscription ID
            if (sub.getStripeSubscriptionId() != null && !sub.getStripeSubscriptionId().isBlank()) {
                com.stripe.model.Subscription stripeSub =
                        com.stripe.model.Subscription.retrieve(sub.getStripeSubscriptionId());
                stripeSub.cancel();
                log.info("Cancelled Stripe subscription {}", sub.getStripeSubscriptionId());
            }

            // Atualizar localmente
            sub.setStatus("CANCELLED");
            sub.setCancelAtPeriodEnd(true);
            subscriptionRepository.save(sub);

            // Downgrade plano da organização
            organization.setPlanType(PlanType.FREE);
            organizationRepository.save(organization);

            log.info("Cancelled subscription for organization {}", organization.getName());
            return true;

        } catch (Exception e) {
            log.error("Error cancelling subscription", e);
            return false;
        }
    }

    /**
     * Obtém o price ID do Stripe para um plano.
     */
    private String getPriceIdForPlan(PlanType planType) {
        return switch (planType) {
            case PRO -> proPriceId;
            case ENTERPRISE -> enterprisePriceId;
            default -> null;
        };
    }

    private String buildSuccessUrl(Long organizationId) {
        return "https://pullwise.ai/settings?session_id={CHECKOUT_SESSION_ID}&org=" + organizationId;
    }

    private String buildCancelUrl() {
        return "https://pullwise.ai/settings?cancelled=true";
    }

    /**
     * Calcula o preço mensal baseado no plano.
     */
    public double getMonthlyPrice(PlanType planType) {
        return switch (planType) {
            case FREE -> 0;
            case PRO -> 24.0;
            case ENTERPRISE -> 299.0;
        };
    }

    /**
     * Calcula o preço anual com desconto.
     */
    public double getAnnualPrice(PlanType planType) {
        double monthly = getMonthlyPrice(planType);
        return monthly * 12 * 0.8; // 20% de desconto
    }
}
