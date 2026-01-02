package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade UsageRecord.
 */
@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

    List<UsageRecord> findByOrganizationId(Long organizationId);

    List<UsageRecord> findByOrganizationIdAndPeriod(Long organizationId, String period);

    @Query("SELECT COALESCE(SUM(u.metricValue), 0) FROM UsageRecord u WHERE u.organization.id = :orgId AND u.period = :period AND u.metricType = :metricType")
    Long sumByOrganizationIdAndPeriodAndMetricType(
            @Param("orgId") Long orgId,
            @Param("period") String period,
            @Param("metricType") String metricType
    );

    @Query("SELECT COALESCE(SUM(u.metricValue), 0) FROM UsageRecord u WHERE u.organization.id = :orgId AND u.usageDate = :date AND u.metricType = :metricType")
    Long sumByOrganizationIdAndDateAndMetricType(
            @Param("orgId") Long orgId,
            @Param("date") LocalDate date,
            @Param("metricType") String metricType
    );

    @Query("SELECT COALESCE(SUM(u.metricValue), 0) FROM UsageRecord u WHERE u.project.id = :projectId AND u.period = :period AND u.metricType = :metricType")
    Long sumByProjectIdAndPeriodAndMetricType(
            @Param("projectId") Long projectId,
            @Param("period") String period,
            @Param("metricType") String metricType
    );

    @Query(value = "SELECT u FROM UsageRecord u WHERE u.organization.id = :orgId AND u.period = :period")
    List<UsageRecord> findByOrganizationIdAndPeriodOrderByDateDesc(@Param("orgId") Long orgId, @Param("period") String period);

    @Query("SELECT u FROM UsageRecord u WHERE u.organization.id = :orgId AND u.metricType = :metricType AND u.usageDate >= :start AND u.usageDate < :end")
    List<UsageRecord> findByOrganizationIdAndMetricTypeAndDateRange(
            @Param("orgId") Long orgId,
            @Param("metricType") String metricType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("SELECT u FROM UsageRecord u WHERE u.organization.id = :orgId AND u.project.id = :projectId AND u.period = :period AND u.metricType = :metricType")
    Optional<UsageRecord> findByOrganizationIdAndProjectIdAndPeriodAndMetricType(
            @Param("orgId") Long orgId,
            @Param("projectId") Long projectId,
            @Param("period") String period,
            @Param("metricType") String metricType
    );

    @Query(value = """
        SELECT period, metric_type, SUM(metric_value) as total
        FROM usage_records
        WHERE organization_id = :orgId
        AND period >= :startPeriod
        AND period <= :endPeriod
        GROUP BY period, metric_type
        ORDER BY period DESC, metric_type
        """, nativeQuery = true)
    List<Object[]> summarizeByPeriodRange(
            @Param("orgId") Long orgId,
            @Param("startPeriod") String startPeriod,
            @Param("endPeriod") String endPeriod
    );
}
