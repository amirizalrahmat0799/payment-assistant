package com.pgs.assistant.chat;

import com.pgs.assistant.knowledge.KnowledgeBase.Snippet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptsTest {

    @Test
    void numbersDocumentationSnippetsWithTheirOrigin() {
        String docs = Prompts.documentation(List.of(
                new Snippet("decline-reasons.md", "Decline reasons", "card_declined", "The bank said no.", 0.8)));

        assertThat(docs).isEqualTo("[1] Decline reasons > card_declined\nThe bank said no.");
    }

    @Test
    void saysSoWhenNothingWasRetrieved() {
        assertThat(Prompts.documentation(List.of())).contains("no relevant documentation");
    }

    @Test
    void systemPromptKeepsTheAssistantReadOnly() {
        assertThat(Prompts.SYSTEM).contains("You are read-only").contains("{documentation}");
    }
}
