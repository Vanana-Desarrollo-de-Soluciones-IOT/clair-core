package com.claircore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Hello", description = "Hello World API")
public class HelloController {

    @GetMapping("/hello-world")
    @Operation(
        summary = "Hello World",
        description = "Returns a simple hello world message"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(example = "{\"message\": \"Hello World\"}")
        )
    )
    public ResponseEntity<?> helloWorld() {
        return ResponseEntity.ok().body(new HelloResponse("Hello World"));
    }

    public static class HelloResponse {
        private String message;

        public HelloResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
