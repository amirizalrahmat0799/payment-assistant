package com.mizal.pgs.assistant.chat;

import com.mizal.pgs.assistant.chat.ChatDtos.ChatReply;
import com.mizal.pgs.assistant.chat.ChatDtos.ChatRequest;
import com.mizal.pgs.assistant.chat.ChatDtos.Source;
import com.mizal.pgs.assistant.gateway.GatewayClient;
import com.mizal.pgs.assistant.gateway.GatewayModels.Merchant;
import com.mizal.pgs.assistant.knowledge.KnowledgeBase;
import com.mizal.pgs.assistant.knowledge.KnowledgeBase.Snippet;
import com.mizal.pgs.assistant.tools.MerchantTools;
import com.mizal.pgs.assistant.tools.ToolContextKeys;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One chat turn:
 * <ol>
 *   <li>authenticate the merchant with their gateway API key and apply the rate limit,</li>
 *   <li>retrieve relevant documentation (RAG) and put it in the system prompt,</li>
 *   <li>call the model with conversation memory and the read-only merchant tools,</li>
 *   <li>return the answer with its sources and the tools that were used.</li>
 * </ol>
 */
@Service
public class AssistantService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final MerchantTools tools;
    private final KnowledgeBase knowledge;
    private final GatewayClient gateway;
    private final RateLimiter rateLimiter;

    public AssistantService(ChatClient.Builder builder, ChatMemory chatMemory, MerchantTools tools,
                            KnowledgeBase knowledge, GatewayClient gateway, RateLimiter rateLimiter) {
        this.chatMemory = chatMemory;
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.tools = tools;
        this.knowledge = knowledge;
        this.gateway = gateway;
        this.rateLimiter = rateLimiter;
    }

    public ChatReply chat(String apiKey, ChatRequest request) {
        Merchant merchant = gateway.authenticate(apiKey);
        rateLimiter.acquire(merchant.id());

        String conversationId = request.conversationId() == null ? UUID.randomUUID().toString() : request.conversationId();
        List<Snippet> snippets = knowledge.search(request.message());
        List<String> toolLog = Collections.synchronizedList(new ArrayList<>());

        ChatResponse response = chatClient.prompt()
                .system(s -> s.text(Prompts.SYSTEM)
                        .param("merchantName", merchant.name())
                        .param("feePercent", String.format("%.2f", merchant.feeBps() / 100.0))
                        .param("today", LocalDate.now(ZoneOffset.UTC).toString())
                        .param("documentation", Prompts.documentation(snippets)))
                .user(request.message())
                // Memory is namespaced by merchant, so one merchant can never continue another's conversation.
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, memoryKey(merchant, conversationId)))
                .tools(tools)
                .toolContext(Map.of(
                        ToolContextKeys.API_KEY, apiKey,
                        ToolContextKeys.MERCHANT_ID, merchant.id(),
                        ToolContextKeys.TOOL_LOG, toolLog))
                .call()
                .chatResponse();

        String reply = response == null || response.getResult() == null
                ? "Sorry, I couldn't generate an answer. Please try again."
                : response.getResult().getOutput().getText();
        Integer tokens = response == null || response.getMetadata().getUsage() == null
                ? null : response.getMetadata().getUsage().getTotalTokens();

        List<Source> sources = snippets.stream()
                .map(s -> new Source(s.title(), s.section()))
                .distinct()
                .toList();
        return new ChatReply(conversationId, reply, sources, List.copyOf(toolLog), tokens);
    }

    public void forget(String apiKey, String conversationId) {
        Merchant merchant = gateway.authenticate(apiKey);
        chatMemory.clear(memoryKey(merchant, conversationId));
    }

    private static String memoryKey(Merchant merchant, String conversationId) {
        return merchant.id() + ":" + conversationId;
    }
}
