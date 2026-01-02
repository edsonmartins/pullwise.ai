package com.pullwise.api.application.service.billing;

import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.Subscription;
import com.pullwise.api.domain.enums.PlanType;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
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

    /**
     * Verifica se o Stripe está configurado.
     */
    public boolean isConfigured() {
        return stripeApiKey != null && !stripeApiKey.isBlank();
    }

    /**
     * Cria uma checkout session no Stripe para upgrade de plano.
     *
     * @param organizationId ID da organização
     * @param planType Tipo de plano desejado
     * @return URL de checkout ou vazio se não configurado
     */
    public Optional<String> createCheckoutSession(Long organizationId, PlanType planType) {
        if (!isConfigured()) {
            log.warn("Stripe is not configured");
            return Optional.empty();
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        String priceId = getPriceIdForPlan(planType);
        String successUrl = buildSuccessUrl(organizationId);
        String cancelUrl = buildCancelUrl();

        try {
            // TODO: Implementar chamada real à API do Stripe
            // SessionCreateParams params = SessionCreateParams.builder()
            //     .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            //     .setSuccessUrl(successUrl)
            //     .setCancelUrl(cancelUrl)
            //     .addLineItem(SessionCreateParams.LineItem.builder()
            //         .setPrice(priceId)
            //         .setQuantity(1L)
            //         .build())
            //     .setCustomerEmail(organization.getOwnerEmail())
            //     .build();
            // Session session = Session.create(params);

            log.info("Created Stripe checkout session for organization {} upgrading to {}",
                    organization.getName(), planType);

            // Por ora retorna URL mockada
            return Optional.of("https://checkout.stripe.com/mock/" + organizationId);

        } catch (Exception e) {
            log.error("Error creating Stripe checkout session", e);
            return Optional.empty();
        }
    }

    /**
     * Cria um portal session para o cliente gerenciar a assinatura.
     *
     * @param organizationId ID da organização
     * @return URL do portal ou vazio
     */
    public Optional<String> createCustomerPortalSession(Long organizationId) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        try {
            // TODO: Implementar chamada real à API do Stripe
            log.info("Created Stripe portal session for organization {}", organization.getName());
            return Optional.of("https://portal.stripe.com/mock/" + organizationId);
        } catch (Exception e) {
            log.error("Error creating Stripe portal session", e);
            return Optional.empty();
        }
    }

    /**
     * Processa webhook do Stripe.
     *
     * @param payload Payload do webhook
     * @param signature Assinatura do Stripe
     * @return true se processado com sucesso
     */
    @Transactional
    public boolean processWebhook(String payload, String signature) {
        if (!isConfigured()) {
            log.warn("Received Stripe webhook but Stripe is not configured");
            return false;
        }

        try {
            // TODO: Verificar assinatura
            // TODO: Parse evento e processar

            String eventId = extractEventId(payload);
            log.info("Processing Stripe webhook event: {}", eventId);

            // Exemplo de processamento de eventos
            if (payload.contains("checkout.session.completed")) {
                handleCheckoutCompleted(payload);
            } else if (payload.contains("customer.subscription.updated")) {
                handleSubscriptionUpdated(payload);
            } else if (payload.contains("customer.subscription.deleted")) {
                handleSubscriptionDeleted(payload);
            } else if (payload.contains("invoice.paid")) {
                handleInvoicePaid(payload);
            }

            return true;

        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return false;
        }
    }

    /**
     * Trata evento de checkout completado.
     */
    private void handleCheckoutCompleted(String payload) {
        // TODO: Parse payload JSON e extrair informações
        // Atualizar ou criar Subscription para a organização

        log.info("Checkout completed - activating subscription");
    }

    /**
     * Trata evento de atualização de subscription.
     */
    private void handleSubscriptionUpdated(String payload) {
        // TODO: Parse payload e atualizar Subscription

        log.info("Subscription updated");
    }

    /**
     * Trata evento de cancelamento de subscription.
     */
    @Transactional
    private void handleSubscriptionDeleted(String payload) {
        // TODO: Parse payload e marcar Subscription como cancelada

        // Fallback: downgrade para FREE
        subscriptionRepository.findAll().stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .forEach(s -> {
                    s.setStatus("CANCELLED");
                    subscriptionRepository.save(s);
                });

        log.info("Subscription deleted - downgraded to FREE");
    }

    /**
     * Trata evento de invoice paga.
     */
    private void handleInvoicePaid(String payload) {
        // TODO: Registrar pagamento
        log.info("Invoice paid");
    }

    /**
     * Cancela uma subscription no Stripe.
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
            // TODO: Chamar Stripe API para cancelar
            Subscription sub = subscription.get();
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

    /**
     * Extrai o ID do evento do payload.
     */
    private String extractEventId(String payload) {
        // TODO: Implementar parse JSON correto
        return payload.substring(0, Math.min(50, payload.length()));
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
