package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.infrastructure.websocket.WebSocketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller para health checks personalizados.
 */
@RestController
@RequestMapping("/api/v2/health")
@Tag(name = "Health", description = "Health checks e monitoramento")
public class HealthController {

    private final DataSource dataSource;
    private final WebSocketService webSocketService;

    public HealthController(DataSource dataSource, WebSocketService webSocketService) {
        this.dataSource = dataSource;
        this.webSocketService = webSocketService;
    }

    /**
     * Health check completo do sistema.
     */
    @GetMapping
    @Operation(summary = "Health check completo", description = "Retorna status de saúde de todos os componentes")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", Instant.now().toString());
        health.put("status", "UP");
        health.put("version", "2.0.0");

        // Database health
        Map<String, Object> db = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            db.put("status", "UP");
            db.put("database", conn.getMetaData().getDatabaseProductName());
            db.put("version", conn.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
            health.put("status", "DEGRADED");
        }
        health.put("database", db);

        // WebSocket health
        Map<String, Object> ws = new HashMap<>();
        ws.put("status", "UP");
        ws.put("activeConnections", webSocketService != null);
        health.put("websocket", ws);

        return ResponseEntity.ok(health);
    }

    /**
     * Health check simples (para load balancers).
     */
    @GetMapping("/live")
    @Operation(summary = "Liveness probe", description = "Verifica se o serviço está rodando")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Readiness check (verifica se o serviço pode receber tráfego).
     */
    @GetMapping("/ready")
    @Operation(summary = "Readiness probe", description = "Verifica se o serviço está pronto para receber tráfego")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> readiness = new HashMap<>();
        boolean ready = true;

        // Check database
        try (Connection conn = dataSource.getConnection()) {
            readiness.put("database", "UP");
        } catch (Exception e) {
            readiness.put("database", "DOWN");
            ready = false;
        }

        readiness.put("status", ready ? "READY" : "NOT_READY");
        readiness.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(ready ? 200 : 503).body(readiness);
    }

    /**
     * Métricas simples do sistema.
     */
    @GetMapping("/metrics")
    @Operation(summary = "Métricas do sistema", description = "Retorna métricas operacionais")
    public ResponseEntity<Map<String, Object>> metrics() {
        Map<String, Object> metrics = new HashMap<>();

        // JVM metrics
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + " MB");
        jvm.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + " MB");
        jvm.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + " MB");
        jvm.put("availableProcessors", runtime.availableProcessors());
        metrics.put("jvm", jvm);

        // System metrics
        Map<String, Object> system = new HashMap<>();
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        metrics.put("system", system);

        metrics.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(metrics);
    }
}
