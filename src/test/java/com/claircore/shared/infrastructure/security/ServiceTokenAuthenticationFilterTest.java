package com.claircore.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * This filter is the only thing authenticating {@code /api/v1/edge/**}. The device roster used to
 * repeat the check inline as well; those assertions live here now, since this is where the check is.
 */
class ServiceTokenAuthenticationFilterTest {

    @Test void rejectsMissingTokenOnCommands() throws Exception { assertUnauthorized("/api/v1/edge/commands/pending", null); }
    @Test void rejectsMissingTokenOnAlerts() throws Exception { assertUnauthorized("/api/v1/edge/alerts/pending", null); }
    @Test void rejectsMissingTokenOnPresence() throws Exception { assertUnauthorized("/api/v1/edge/presence", null); }
    @Test void rejectsMissingTokenOnTelemetryBatch() throws Exception { assertUnauthorized("/api/v1/evaluations/telemetry/batch", null); }
    @Test void rejectsMissingTokenOnTheDeviceRoster() throws Exception { assertUnauthorized("/api/v1/edge/devices", null); }
    @Test void rejectsAWrongTokenOnTheDeviceRoster() throws Exception { assertUnauthorized("/api/v1/edge/devices", "wrong"); }

    @Test
    void acceptsTheLegacyEdgeHeaderOnTheDeviceRosterOnly() throws Exception {
        var chain = mock(FilterChain.class);
        var request = new MockHttpServletRequest("GET", "/api/v1/edge/devices");
        request.addHeader("X-Edge-Token", "secret");
        var response = new MockHttpServletResponse();

        new ServiceTokenAuthenticationFilter("secret").doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void theLegacyHeaderDoesNotOpenAnyOtherEdgePath() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/edge/presence");
        request.addHeader("X-Edge-Token", "secret");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        new ServiceTokenAuthenticationFilter("secret").doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void aBlankSecretRejectsRatherThanAcceptingEverything() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/edge/devices");
        request.addHeader("X-Core-Token", "");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        new ServiceTokenAuthenticationFilter("").doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    private void assertUnauthorized(String path, String token) throws Exception {
        var filter = new ServiceTokenAuthenticationFilter("secret");
        var request = new MockHttpServletRequest("GET", path);
        if (token != null) request.addHeader("X-Core-Token", token);
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }
}
