package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Subscription.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("SELECT s FROM Subscription s WHERE s.organization.id = :organizationId AND s.status = :status")
    Optional<Subscription> findByOrganizationIdAndStatus(@Param("organizationId") Long organizationId, @Param("status") String status);

    @Query("SELECT s FROM Subscription s WHERE s.organization.id = :organizationId ORDER BY s.createdAt DESC")
    List<Subscription> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("SELECT s FROM Subscription s WHERE s.stripeSubscriptionId = :subscriptionId")
    Optional<Subscription> findByStripeSubscriptionId(@Param("subscriptionId") String subscriptionId);

    @Query("SELECT s FROM Subscription s WHERE s.status IN :statuses AND s.currentPeriodEnd < :expiration")
    List<Subscription> findExpiringSubscriptions(@Param("statuses") List<String> statuses, @Param("expiration") LocalDateTime expiration);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.planType = :planType")
    long countByPlanType(@Param("planType") com.pullwise.api.domain.enums.PlanType planType);
}
