package com.claircore.shared.infrastructure.edge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EdgeEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdgeEventPublisher.class);

    private final RestTemplate restTemplate;
    private final String edgeWebhookUrl;
    private final String edgeToken;

    public EdgeEventPublisher(
            @Value("${EDGE_WEBHOOK_URL:${EDGE_WEBHOOK_DEVICES_URL:${edge.webhook-url:http://localhost:5000}}}") String edgeWebhookUrl,
            @Value("${EDGE_TOKEN:${edge.token:}}") String edgeToken
    ) {
        this.restTemplate = new RestTemplate();
        this.edgeWebhookUrl = edgeWebhookUrl;
        this.edgeToken = edgeToken;
    }

    /** Sends only a hint; edge must reconcile through its authenticated pull endpoints. */
    public void notifyChange(String resource, String hint) {
        sendPost("/api/v1/edge/notify", java.util.Map.of("resource", resource, "hint", hint == null ? "" : hint));
    }

    /** @deprecated use notifyChange; retained for source compatibility. */
    @Deprecated
    public void publishAlertIncident(Object event) { notifyChange("alert", null); }

    /** @deprecated use notifyChange; retained for source compatibility. */
    @Deprecated
    public void publishDeviceCommand(Object event) { notifyChange("command", null); }

    /** @deprecated use notifyChange; retained for source compatibility. */
    @Deprecated
    public void publishDeviceChanged(Object event) {
        notifyChange("device", null);
    }

    private void sendPost(String path, Object payload) {
        String url = edgeWebhookUrl + path;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Edge-Token", edgeToken);

            HttpEntity<Object> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(url, request, Void.class);
            LOGGER.info("Successfully sent event to edge at {}", url);
        } catch (Exception e) {
            LOGGER.error("Failed to send event to edge at {}: {}", url, e.getMessage());
            // In a production system, you would retry or queue this.
        }
    }
}
