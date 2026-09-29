package com.mizal.pgs.assistant.web;

import com.mizal.pgs.assistant.chat.AssistantService;
import com.mizal.pgs.assistant.chat.ChatDtos.ChatReply;
import com.mizal.pgs.assistant.chat.ChatDtos.Source;
import com.mizal.pgs.assistant.chat.RateLimiter;
import com.mizal.pgs.assistant.gateway.GatewayClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssistantController.class)
class AssistantControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AssistantService assistant;

    @Test
    void returnsTheReplyWithSourcesAndTools() throws Exception {
        when(assistant.chat(eq("sk_test_key"), any())).thenReturn(new ChatReply("conv-123456", "RM 77.50 is pending.",
                List.of(new Source("Fees and settlement", "Settlement timing")), List.of("getPayoutSummary"), 812));

        mvc.perform(post("/api/v1/assistant/chat")
                        .header("X-Api-Key", "sk_test_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"When is my next payout?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("RM 77.50 is pending."))
                .andExpect(jsonPath("$.toolsUsed[0]").value("getPayoutSummary"))
                .andExpect(jsonPath("$.sources[0].section").value("Settlement timing"));
    }

    @Test
    void rejectsBlankMessages() throws Exception {
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("X-Api-Key", "sk_test_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAnApiKey() throws Exception {
        mvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsInvalidKeyTo401() throws Exception {
        when(assistant.chat(any(), any())).thenThrow(new GatewayClient.InvalidApiKeyException());

        mvc.perform(post("/api/v1/assistant/chat")
                        .header("X-Api-Key", "sk_test_bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mapsRateLimitTo429() throws Exception {
        when(assistant.chat(any(), any())).thenThrow(new RateLimiter.RateLimitExceededException(20));

        mvc.perform(post("/api/v1/assistant/chat")
                        .header("X-Api-Key", "sk_test_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void hidesModelProviderErrors() throws Exception {
        when(assistant.chat(any(), any())).thenThrow(new IllegalStateException("ThrottlingException: secret details"));

        mvc.perform(post("/api/v1/assistant/chat")
                        .header("X-Api-Key", "sk_test_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("The assistant couldn't answer right now. Please try again in a moment."));
    }
}
