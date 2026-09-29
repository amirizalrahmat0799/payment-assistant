package com.mizal.pgs.assistant.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ChatDtos {

    private ChatDtos() {
    }

    /**
     * @param conversationId omit to start a new conversation; send back the returned id to continue it
     */
    public record ChatRequest(
            @NotBlank @Size(max = 1000) String message,
            @Pattern(regexp = "[A-Za-z0-9-]{8,64}") String conversationId) {
    }

    public record Source(String title, String section) {
    }

    public record ChatReply(String conversationId, String reply, List<Source> sources, List<String> toolsUsed,
                            Integer totalTokens) {
    }
}
