package com.pullwise.api.infrastructure.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.domain.enums.ReviewStatus;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Controller SSE para streaming de progresso de review.
 * Permite que o CLI e outros HTTP clients recebam atualizações em tempo real
 * sem precisar de WebSocket.
 */
@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewStreamController {

    private final ReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;

    // Mapa de emitters ativos por reviewId
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> activeEmitters = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * SSE endpoint para streaming de progresso de review.
     * O CLI se conecta aqui e recebe eventos em tempo real.
     */
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamReview(@PathVariable Long id) {
        // 5 minutos de timeout (reviews grandes podem demorar)
        SseEmitter emitter = new SseEmitter(300_000L);

        // Verificar se review existe
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"message\":\"Review not found\"}"));
                emitter.complete();
            } catch (IOException e) {
                log.warn("Failed to send SSE event: {}", e.getMessage());
            }
            return emitter;
        }

        // Se já completou, enviar status final imediatamente
        if (review.getStatus().isTerminal()) {
            try {
                emitter.send(SseEmitter.event()
                        .name("review.completed")
                        .data(buildStatusEvent(review)));
                emitter.complete();
            } catch (IOException e) {
                log.warn("Failed to send SSE event: {}", e.getMessage());
            }
            return emitter;
        }

        // Registrar emitter
        activeEmitters.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Cleanup on completion/timeout
        emitter.onCompletion(() -> removeEmitter(id, emitter));
        emitter.onTimeout(() -> removeEmitter(id, emitter));
        emitter.onError(e -> removeEmitter(id, emitter));

        // Enviar status inicial
        try {
            emitter.send(SseEmitter.event()
                    .name("review.status")
                    .data(buildStatusEvent(review)));
        } catch (IOException e) {
                log.warn("Failed to send SSE event: {}", e.getMessage());
            }

        // Iniciar polling do status do review (a cada 2 segundos)
        ScheduledFuture<?> pollingTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                Review current = reviewRepository.findById(id).orElse(null);
                if (current == null || current.getStatus().isTerminal()) {
                    String eventName = current != null && current.getStatus() == ReviewStatus.COMPLETED
                            ? "review.completed" : "review.failed";
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(buildStatusEvent(current != null ? current : review)));
                    emitter.complete();
                } else {
                    emitter.send(SseEmitter.event()
                            .name("review.progress")
                            .data(buildStatusEvent(current)));
                }
            } catch (Exception e) {
                removeEmitter(id, emitter);
            }
        }, 2, 2, TimeUnit.SECONDS);

        // Cancelar polling quando emitter terminar
        emitter.onCompletion(() -> pollingTask.cancel(false));
        emitter.onTimeout(() -> pollingTask.cancel(false));
        emitter.onError(e -> pollingTask.cancel(false));

        return emitter;
    }

    /**
     * Envia evento SSE para todos os emitters de um review.
     * Chamado pelo WebSocketService para bridge de eventos.
     */
    public void sendEvent(Long reviewId, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> emitters = activeEmitters.get(reviewId);
        if (emitters == null || emitters.isEmpty()) return;

        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(jsonData));
            } catch (Exception e) {
                removeEmitter(reviewId, emitter);
            }
        }
    }

    private void removeEmitter(Long reviewId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = activeEmitters.get(reviewId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                activeEmitters.remove(reviewId);
            }
        }
    }

    private String buildStatusEvent(Review review) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "reviewId", review.getId(),
                    "status", review.getStatus().name(),
                    "filesAnalyzed", review.getFilesAnalyzed() != null ? review.getFilesAnalyzed() : 0,
                    "durationMs", review.getDurationMs() != null ? review.getDurationMs() : 0
            ));
        } catch (Exception e) {
            return "{}";
        }
    }
}
