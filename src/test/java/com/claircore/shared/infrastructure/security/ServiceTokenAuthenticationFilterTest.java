package com.claircore.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ServiceTokenAuthenticationFilterTest {
    @Test void rejectsMissingTokenOnCommands() throws Exception { assertUnauthorized("/api/v1/edge/commands/pending", null); }
    @Test void rejectsMissingTokenOnAlerts() throws Exception { assertUnauthorized("/api/v1/edge/alerts/pending", null); }
    @Test void rejectsMissingTokenOnPresence() throws Exception { assertUnauthorized("/api/v1/edge/presence", null); }
    @Test void rejectsMissingTokenOnTelemetryBatch() throws Exception { assertUnauthorized("/api/v1/evaluations/telemetry/batch", null); }
    private void assertUnauthorized(String path, String token) throws Exception {
        var filter = new ServiceTokenAuthenticationFilter("secret");
        var request = new MockHttpServletRequest("GET", path);
        if (token != null) request.addHeader("X-Core-Token", token);
        var response = new MockHttpServletResponse(); FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus()); verifyNoInteractions(chain);
    }
}
