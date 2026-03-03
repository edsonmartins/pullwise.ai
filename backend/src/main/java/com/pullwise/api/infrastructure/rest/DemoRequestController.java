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
        log.info("Demo request received from: {} <{}> at {}",
                request.get("name"),
                request.get("email"),
                request.get("company"));

        // TODO: Persist to database or send notification email when needed
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("status", "received"));
    }
}
