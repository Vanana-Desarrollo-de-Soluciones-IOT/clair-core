package com.claircore.billing.interfaces.rest.controllers;

import com.claircore.billing.domain.services.SubscriptionCommandService;
import com.claircore.iam.domain.services.TokenQueryService;
import com.claircore.iam.infrastructure.config.JwtAuthenticationEntryPoint;
import com.claircore.iam.infrastructure.config.SecurityConfiguration;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = StripeWebhookController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
        properties = {
                "cors.allowed-origins=http://localhost",
                "stripe.webhook.secret=test-secret"
        }
)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
class StripeWebhookControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionCommandService subscriptionCommandService;

    @MockitoBean
    private TokenQueryService tokenQueryService;

    @Test
    void shouldReturnBadRequestWhenWebhookSignatureIsInvalidWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/stripe")
                        .header("Stripe-Signature", "bad-signature")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
