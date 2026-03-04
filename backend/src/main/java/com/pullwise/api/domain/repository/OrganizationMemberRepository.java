package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    Optional<OrganizationMember> findByUserIdAndOrganizationId(Long userId, Long organizationId);

    List<OrganizationMember> findByUserId(Long userId);

    boolean existsByUserIdAndOrganizationId(Long userId, Long organizationId);

    @Query("SELECT om.organization.id FROM OrganizationMember om WHERE om.user.id = :userId")
    List<Long> findOrganizationIdsByUserId(@Param("userId") Long userId);
}
