package com.truholdem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
@Tag(name = "Root API", description = "Root endpoint operations including welcome message.")
public class RootController {

    @GetMapping("/")
    @Operation(summary = "Root Welcome Message", description = "Displays a welcome message at the root path")
    public ResponseEntity<Map<String, String>> rootWelcome() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Welcome to the Truholdem Poker Game API v.1.3.0 Root!");
        response.put("documentation", "Swagger documentation is available at: http://localhost:8080/swagger-ui.html");
        return ResponseEntity.ok(response);
    }
}