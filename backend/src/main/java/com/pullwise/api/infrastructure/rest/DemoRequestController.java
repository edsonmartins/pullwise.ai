package com.pullwise.api.infrastructure.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para receber solicitações de demo.
 * Endpoint público (sem autenticação).
 */
@Slf4j
@RestController
@RequestMapping("/api/demo-requests")
@RequiredArgsConstructor
public class DemoRequestController {

    @PostMapping
    public ResponseEntity<Map<String, String>> createDemoRequest(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        String company = request.get("company");
        String message = request.get("message");

        // Validate required fields
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Name is required"));
        }
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
        }

        log.info("Demo request received from: {} <{}> at company: {}. Message: {}",
                name, email, company, message);

        // Note: When a DemoRequest entity is introduced, persist here.
        // For now the request is logged for manual follow-up.

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "status", "received",
                        "message", "Thank you for your interest! We will contact you soon."
                ));
    }
}
