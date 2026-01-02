package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.infrastructure.websocket.WebSocketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para operações relacionadas ao WebSocket.
 */
@RestController
@RequestMapping("/api/v2/websocket")
@Tag(name = "WebSocket", description = "Operações de WebSocket em tempo real")
public class WebSocketController {

    private final WebSocketService webSocketService;

    public WebSocketController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    /**
     * Envia broadcast de notificação para todos os clientes conectados a um tópico.
     */
    @PostMapping("/broadcast")
    @Operation(summary = "Envia broadcast para um tópico", description = "Envia mensagem para todos os inscritos no tópico")
    public ResponseEntity<?> broadcast(
            @RequestParam String topic,
            @RequestBody Map<String, Object> message
    ) {
        webSocketService.sendToTopic(topic, message);
        return ResponseEntity.ok(Map.of("success", true, "subscribers", webSocketService.hasActiveSubscriptions(topic)));
    }

    /**
     * Envia notificação de progresso de review.
     */
    @PostMapping("/review/progress")
    @Operation(summary = "Envia progresso de review", description = "Broadcast de progresso para um review específico")
    public ResponseEntity<?> sendReviewProgress(
            @RequestParam Long reviewId,
            @RequestParam int progress,
            @RequestParam String stage,
            @RequestParam(required = false, defaultValue = "ANALYZING") String status,
            @RequestParam(required = false) String message
    ) {
        com.pullwise.api.infrastructure.websocket.dto.ReviewProgressMessage.Status st =
            com.pullwise.api.infrastructure.websocket.dto.ReviewProgressMessage.Status.valueOf(status);
        webSocketService.sendReviewProgress(reviewId, progress, stage, st, message);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Envia notificação de issue detectada.
     */
    @PostMapping("/review/issue")
    @Operation(summary = "Notifica issue detectada", description = "Envia notificação quando issue é encontrada")
    public ResponseEntity<?> sendIssueDetected(
            @RequestParam Long reviewId,
            @RequestParam Long issueId,
            @RequestParam String severity,
            @RequestParam String type,
            @RequestParam String title,
            @RequestParam String filePath,
            @RequestParam Integer lineStart
    ) {
        webSocketService.sendIssueDetected(reviewId, issueId, severity, type, title, filePath, lineStart);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Verifica status das conexões ativas.
     */
    @GetMapping("/status")
    @Operation(summary = "Status do WebSocket", description = "Retorna informações sobre conexões ativas")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(Map.of(
            "websocket", "enabled",
            "endpoint", "/ws",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
