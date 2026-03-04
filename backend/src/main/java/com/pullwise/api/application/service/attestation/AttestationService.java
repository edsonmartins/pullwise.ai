package com.pullwise.api.application.service.attestation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.model.ReviewAttestation;
import com.pullwise.api.domain.repository.ReviewAttestationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço de attestation criptográfica para reviews.
 *
 * <p>Gera provas tamper-evident de que um review foi executado.
 * Usa HMAC-SHA256 para assinar o payload do review, criando um
 * registro verificável independentemente para compliance (SOX, HIPAA, SOC2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttestationService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";

    private final ReviewAttestationRepository attestationRepository;
    private final ObjectMapper objectMapper;

    @Value("${pullwise.attestation.signing-key:pullwise-attestation-key-change-in-production}")
    private String signingKey;

    @Value("${pullwise.attestation.key-id:default}")
    private String keyId;

    /**
     * Cria uma attestation para um review completado.
     */
    @Transactional
    public ReviewAttestation createAttestation(Review review, List<Issue> issues) {
        if (attestationRepository.existsByReviewId(review.getId())) {
            log.warn("Attestation already exists for review {}", review.getId());
            return attestationRepository.findByReviewId(review.getId()).orElseThrow();
        }

        try {
            // Construir payload
            Map<String, Object> payloadMap = buildPayload(review, issues);
            String payloadJson = objectMapper.writeValueAsString(payloadMap);

            // Gerar hash SHA-256 do payload
            String payloadHash = sha256Hash(payloadJson);

            // Assinar com HMAC-SHA256
            String signature = hmacSign(payloadJson);

            ReviewAttestation attestation = ReviewAttestation.builder()
                    .review(review)
                    .payload(payloadJson)
                    .payloadHash(payloadHash)
                    .signature(signature)
                    .algorithm(ALGORITHM)
                    .keyId(keyId)
                    .build();

            attestation = attestationRepository.save(attestation);
            log.info("Attestation created for review {} (hash: {})", review.getId(), payloadHash.substring(0, 16));

            return attestation;

        } catch (Exception e) {
            log.error("Failed to create attestation for review {}: {}", review.getId(), e.getMessage());
            throw new RuntimeException("Attestation creation failed", e);
        }
    }

    /**
     * Verifica a integridade de uma attestation.
     * Recalcula o hash e a assinatura e compara com os valores armazenados.
     */
    public VerificationResult verify(Long reviewId) {
        Optional<ReviewAttestation> optAttestation = attestationRepository.findByReviewId(reviewId);
        if (optAttestation.isEmpty()) {
            return new VerificationResult(false, "No attestation found for review " + reviewId, null, null);
        }

        ReviewAttestation attestation = optAttestation.get();

        try {
            // Verificar hash
            String computedHash = sha256Hash(attestation.getPayload());
            boolean hashValid = computedHash.equals(attestation.getPayloadHash());

            // Verificar assinatura
            String computedSignature = hmacSign(attestation.getPayload());
            boolean signatureValid = computedSignature.equals(attestation.getSignature());

            boolean valid = hashValid && signatureValid;
            String message = valid
                    ? "Attestation is valid and untampered"
                    : "INTEGRITY VIOLATION: " +
                        (!hashValid ? "hash mismatch " : "") +
                        (!signatureValid ? "signature mismatch" : "");

            return new VerificationResult(valid, message, attestation.getPayloadHash(), attestation.getCreatedAt());

        } catch (Exception e) {
            return new VerificationResult(false, "Verification error: " + e.getMessage(), null, null);
        }
    }

    /**
     * Retorna a attestation de um review.
     */
    public Optional<AttestationDTO> getAttestation(Long reviewId) {
        return attestationRepository.findByReviewId(reviewId)
                .map(a -> new AttestationDTO(
                        a.getId(),
                        a.getReview().getId(),
                        a.getPayloadHash(),
                        a.getAlgorithm(),
                        a.getKeyId(),
                        a.getCreatedAt()
                ));
    }

    private Map<String, Object> buildPayload(Review review, List<Issue> issues) {
        long criticalCount = issues.stream()
                .filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.CRITICAL).count();
        long highCount = issues.stream()
                .filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.HIGH).count();
        long mediumCount = issues.stream()
                .filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.MEDIUM).count();
        long lowCount = issues.stream()
                .filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.LOW).count();

        return Map.of(
                "reviewId", review.getId(),
                "status", review.getStatus().name(),
                "pullRequestId", review.getPullRequest().getId(),
                "prNumber", review.getPullRequest().getPrNumber(),
                "repository", review.getPullRequest().getProject() != null
                        ? review.getPullRequest().getProject().getRepositoryUrl() : "unknown",
                "filesAnalyzed", review.getFilesAnalyzed() != null ? review.getFilesAnalyzed() : 0,
                "totalIssues", issues.size(),
                "issueSummary", Map.of(
                        "critical", criticalCount,
                        "high", highCount,
                        "medium", mediumCount,
                        "low", lowCount
                ),
                "completedAt", review.getCompletedAt() != null
                        ? review.getCompletedAt().toString() : LocalDateTime.now().toString(),
                "attestedAt", LocalDateTime.now().toString()
        );
    }

    private String sha256Hash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String hmacSign(String data) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        mac.init(keySpec);
        byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public record VerificationResult(
            boolean valid,
            String message,
            String payloadHash,
            LocalDateTime attestedAt
    ) {}

    public record AttestationDTO(
            Long id,
            Long reviewId,
            String payloadHash,
            String algorithm,
            String keyId,
            LocalDateTime createdAt
    ) {}
}
