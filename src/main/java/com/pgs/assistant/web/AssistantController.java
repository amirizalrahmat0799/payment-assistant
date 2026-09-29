package com.pgs.assistant.web;

import com.pgs.assistant.chat.AssistantService;
import com.pgs.assistant.chat.ChatDtos.ChatReply;
import com.pgs.assistant.chat.ChatDtos.ChatRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/chat")
    public ChatReply chat(@RequestHeader("X-Api-Key") String apiKey, @Valid @RequestBody ChatRequest request) {
        return assistant.chat(apiKey, request);
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forget(@RequestHeader("X-Api-Key") String apiKey, @PathVariable String conversationId) {
        assistant.forget(apiKey, conversationId);
    }
}
