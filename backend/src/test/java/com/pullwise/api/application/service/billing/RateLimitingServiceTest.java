package com.pullwise.api.application.service.billing;

import com.pullwise.api.domain.enums.PlanType;
import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.UsageRecord;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.UsageRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService(usageRecordRepository, organizationRepository);
    }

    @Test
    void checkReviewLimit_freePlanUnderLimit_shouldPass() {
        Organization org = new Organization();
        org.setId(1L);
        org.setPlanType(PlanType.FREE);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                eq(1L), anyString(), eq(UsageRecord.METRIC_REVIEWS))).thenReturn(30L);

        assertThatCode(() -> rateLimitingService.checkReviewLimit(1L))
                .doesNotThrowAnyException();
    }

    @Test
    void checkReviewLimit_freePlanOverLimit_shouldThrow() {
        Organization org = new Organization();
        org.setId(1L);
        org.setPlanType(PlanType.FREE);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                eq(1L), anyString(), eq(UsageRecord.METRIC_REVIEWS))).thenReturn(50L);

        assertThatThrownBy(() -> rateLimitingService.checkReviewLimit(1L))
                .isInstanceOf(RateLimitingService.RateLimitExceededException.class)
                .hasMessageContaining("Monthly review limit exceeded");
    }

    @Test
    void checkReviewLimit_proPlanOverFreeLimit_shouldPass() {
        Organization org = new Organization();
        org.setId(1L);
        org.setPlanType(PlanType.PRO);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                eq(1L), anyString(), eq(UsageRecord.METRIC_REVIEWS))).thenReturn(100L);

        assertThatCode(() -> rateLimitingService.checkReviewLimit(1L))
                .doesNotThrowAnyException();
    }

    @Test
    void checkReviewLimit_enterprisePlan_shouldAlwaysPass() {
        Organization org = new Organization();
        org.setId(1L);
        org.setPlanType(PlanType.ENTERPRISE);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));

        assertThatCode(() -> rateLimitingService.checkReviewLimit(1L))
                .doesNotThrowAnyException();
    }

    @Test
    void checkReviewLimit_organizationNotFound_shouldThrow() {
        when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rateLimitingService.checkReviewLimit(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkRepositoryLimit_freePlanAtLimit_shouldThrow() {
        Organization org = new Organization();
        org.setId(1L);
        org.setPlanType(PlanType.FREE);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> rateLimitingService.checkRepositoryLimit(1L, 3))
                .isInstanceOf(RateLimitingService.RateLimitExceededException.class)
                .hasMessageContaining("Repository limit exceeded");
    }

    @Test
    void checkRepositoryLimit_enterprisePlan_shouldAlwaysPass() {
        Organization org = new Organization();
        org.setId(1L);
        org.setPlanType(PlanType.ENTERPRISE);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));

        assertThatCode(() -> rateLimitingService.checkRepositoryLimit(1L, 100))
                .doesNotThrowAnyException();
    }

    @Test
    void getReviewUsage_shouldReturnCorrectUsageInfo() {
        Organization org = new Organization();
        org.setId(1L);
        org.setPlanType(PlanType.FREE);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                eq(1L), anyString(), eq(UsageRecord.METRIC_REVIEWS))).thenReturn(25L);

        RateLimitingService.UsageInfo info = rateLimitingService.getReviewUsage(1L);

        assertThat(info.current()).isEqualTo(25);
        assertThat(info.limit()).isEqualTo(50);
        assertThat(info.planType()).isEqualTo(PlanType.FREE);
        assertThat(info.isExceeded()).isFalse();
        assertThat(info.usagePercentage()).isEqualTo(50.0);
    }

    @Test
    void getReviewUsage_enterprisePlan_shouldReturnUnlimited() {
        Organization org = new Organization();
        org.setId(1L);
        org.setPlanType(PlanType.ENTERPRISE);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                eq(1L), anyString(), eq(UsageRecord.METRIC_REVIEWS))).thenReturn(1000L);

        RateLimitingService.UsageInfo info = rateLimitingService.getReviewUsage(1L);

        assertThat(info.limit()).isEqualTo(-1);
        assertThat(info.isExceeded()).isFalse();
    }
}
