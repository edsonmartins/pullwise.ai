package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para a entidade User.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.githubId = :githubId")
    Optional<User> findByGitHubId(@Param("githubId") String githubId);

    @Query("SELECT u FROM User u WHERE u.githubLogin = :githubLogin")
    Optional<User> findByGitHubLogin(@Param("githubLogin") String githubLogin);

    @Query("SELECT u FROM User u WHERE u.bitbucketUuid = :uuid")
    Optional<User> findByBitbucketUuid(@Param("uuid") String uuid);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();
}
