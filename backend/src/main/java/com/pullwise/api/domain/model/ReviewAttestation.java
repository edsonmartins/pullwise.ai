package com.pullwise.api.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma attestation criptográfica de review.
 * Fornece prova tamper-evident de que o código foi revisado.
 */
@Entity
@Table(name = "review_attestations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAttestation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private Review review;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    @Column(name = "signature", nullable = false, length = 512)
    private String signature;

    @Column(name = "algorithm", nullable = false, length = 50)
    @Builder.Default
    private String algorithm = "HmacSHA256";

    @Column(name = "key_id", length = 100)
    private String keyId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
