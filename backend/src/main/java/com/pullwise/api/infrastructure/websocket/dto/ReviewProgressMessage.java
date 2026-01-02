package com.pullwise.api.infrastructure.websocket.dto;

import java.time.Instant;

/**
 * Mensagem de progresso de review enviada via WebSocket.
 */
public record ReviewProgressMessage(
    String type,
    ReviewProgressData data,
    Instant timestamp
) {
    public enum Status {
        QUEUED,
        ANALYZING,
        GENERATING_FIX,
        COMPLETED,
        FAILED
    }

    public record ReviewProgressData(
        Long reviewId,
        int progress,
        String stage,
        Status status,
        String message
    ) {
        public static ReviewProgressData create(Long reviewId, int progress, String stage, Status status, String message) {
            return new ReviewProgressData(reviewId, progress, stage, status, message);
        }
    }

    public static ReviewProgressMessage create(String type, ReviewProgressData data) {
        return new ReviewProgressMessage(type, data, Instant.now());
    }
}
