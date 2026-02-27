package com.prashant.ai_chat_bot.controller;

import com.prashant.ai_chat_bot.service.ConversationIdGenerator;
import com.prashant.ai_chat_bot.service.MultiModelProviderService;
import com.prashant.ai_chat_bot.utils.AIProviderConstants;
import com.prashant.ai_chat_bot.utils.InputSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@RestController
@RequestMapping("/chatmodel/streaming")
@RequiredArgsConstructor
@Slf4j
public class StreamingChatModelController {

    private static final String CHAT_MEMORY_CONVERSATION_ID = "chat_memory_conversation_id";
    private final MultiModelProviderService multiModelProviderService;
    private final ConversationIdGenerator conversationIdGenerator;
    private final ChatMemory chatMemory;
    @Value("${app.ai.chat-memory.enabled:false}")
    private boolean defaultChatMemoryEnabled;

    //text/event-stream: this is a SSE (Server Sent events) endpoint. Spring boot handles streaming internally
    @PostMapping(value= "/chat", produces = "text/event-stream")
    public Flux<String> chat(
      @RequestHeader(value = AIProviderConstants.AI_PROVIDER_HEADER, required = false) String aiProvider,
      @RequestBody String messageInput) {
        String resolvedProvider = aiProvider == null ? AIProviderConstants.OLLAMA : aiProvider;
        String resolvedMessage = messageInput == null ? "" : messageInput;

        return multiModelProviderService.getChatClient(resolvedProvider)
                .prompt()
                .user(resolvedMessage)
                .stream()
                .content();
    }

    @PostMapping(
      value = "/chat/conversation",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> chat(
      @RequestParam(value = "conversationId", required = false) Integer conversationId,
      @RequestHeader(value = AIProviderConstants.AI_PROVIDER_HEADER, required = false, defaultValue = AIProviderConstants.OLLAMA) String aiProvider,
      @RequestBody String messageInput) {

        return Flux.defer(() -> {

            String sanitized = InputSanitizer.sanitize(messageInput);
            if (sanitized == null || sanitized.isBlank()) {
                return Flux.error(new IllegalArgumentException("Message input cannot be empty"));
            }

            Integer finalConversationId = Optional.ofNullable(conversationId)
              .orElseGet(conversationIdGenerator::nextId);

            ChatClient chatClient = Optional
              .ofNullable(multiModelProviderService.getChatClient(aiProvider))
              .orElseThrow(() ->
                new IllegalArgumentException("Invalid AI provider: " + aiProvider));

            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
              .user(sanitized)
              .advisors(advisorSpec -> advisorSpec.param(CHAT_MEMORY_CONVERSATION_ID, String.valueOf(finalConversationId)));
            if (!defaultChatMemoryEnabled) {
                requestSpec = requestSpec.advisors(
                  MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(String.valueOf(finalConversationId))
                    .build());
            }

            return requestSpec.stream().chatResponse()

              // Convert ChatResponse -> String (for frontend)
              .map(response -> {

                  String text = Optional.ofNullable(response.getResult())
                    .map(r -> r.getOutput())
                    .map(AssistantMessage::getText)
                    .orElse("");

                  return text;

              })

              // Handle client disconnect
              .doFinally(signal -> {
                  if (signal == SignalType.CANCEL) {
                      log.warn("Client disconnected: conversation {}",
                        finalConversationId);
                  }
              })

              // Handle errors AFTER conversion to String
              .onErrorResume(error -> {
                  log.error("Streaming error", error);
                  return Flux.just("Error: AI processing failed");
              })

              // Move blocking calls off event loop
              .publishOn(Schedulers.boundedElastic());
        });
    }





}
