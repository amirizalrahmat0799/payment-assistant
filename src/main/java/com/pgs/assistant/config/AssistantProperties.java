package com.pgs.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant")
public record AssistantProperties(Gateway gateway, Rag rag, RateLimit rateLimit, int maxMessageLength) {

    public record Gateway(String merchantUrl, String paymentUrl, String settlementUrl) {
    }

    /** How many knowledge chunks to retrieve, and how similar they must be to be used. */
    public record Rag(int topK, double similarityThreshold) {
    }

    public record RateLimit(int requestsPerMinute) {
    }
}
