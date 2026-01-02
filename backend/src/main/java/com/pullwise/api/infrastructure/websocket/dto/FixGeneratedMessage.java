package com.pullwise.api.infrastructure.websocket.dto;

import java.time.Instant;

/**
 * Mensagem emitida quando um auto-fix é gerado.
 */
public record FixGeneratedMessage(
    String type,
    FixData data,
    Instant timestamp
) {
    public record FixData(
        Long suggestionId,
        Long issueId,
        String status,
        String modelUsed
    ) {}

    public static FixGeneratedMessage create(Long suggestionId, Long issueId, String status, String modelUsed) {
        FixData data = new FixData(suggestionId, issueId, status, modelUsed);
        return new FixGeneratedMessage("fix.generated", data, Instant.now());
    }
}
