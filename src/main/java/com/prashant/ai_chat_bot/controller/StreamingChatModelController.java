package com.prashant.ai_chat_bot.controller;

import com.prashant.ai_chat_bot.service.ConversationService;
import com.prashant.ai_chat_bot.service.MultiModelProviderService;
import com.prashant.ai_chat_bot.utils.InputSanitizer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/chatmodel/streaming")
@AllArgsConstructor
@Slf4j
public class StreamingChatModelController {

    private final MultiModelProviderService multiModelProviderService;
    private final ConversationService conversationService;

    //text/event-stream: this is a SSE (Server Sent events) endpoint. Spring boot handles streaming internally
    @PostMapping(value= "/chat", produces = "text/event-stream")
    public Flux<String> chat(
      @RequestHeader(value = "ai-provider", required = false) String aiProvider,
      @RequestBody String messageInput) {
        String resolvedProvider = aiProvider == null ? "ollama" : aiProvider;
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
      @RequestHeader(value = "ai-provider", required = false, defaultValue = "ollama") String aiProvider,
      @RequestBody String messageInput) {

        return Flux.defer(() -> {

            String sanitized = InputSanitizer.sanitize(messageInput);
            if (sanitized == null || sanitized.isBlank()) {
                return Flux.error(new IllegalArgumentException("Message input cannot be empty"));
            }

            Integer finalConversationId = Optional.ofNullable(conversationId)
              .orElseGet(conversationService::generateConversationId);

            conversationService.addUserMessage(finalConversationId, sanitized);

            List<Message> history =
              conversationService.getRecentMessages(finalConversationId);

            ChatClient chatClient = Optional
              .ofNullable(multiModelProviderService.getChatClient(aiProvider))
              .orElseThrow(() ->
                new IllegalArgumentException("Invalid AI provider: " + aiProvider));


            AtomicReference<String> lastMessage = new AtomicReference<>("");

            return chatClient.prompt()
              .messages(history)
              .stream()
              .chatResponse()

              // Convert ChatResponse -> String (for frontend)
              .map(response -> {

                  String text = Optional.ofNullable(response.getResult())
                    .map(r -> r.getOutput())
                    .map(AssistantMessage::getText)
                    .orElse("");

                  if (!text.isEmpty()) {
                      lastMessage.updateAndGet(prev -> prev + text);
                  }

                  return text;

              })

              // Persist after stream completes
              .doOnComplete(() -> {
                  String finalContent = lastMessage.get();
                  if (!finalContent.isBlank()) {
                      conversationService.addAssistantMessage(
                        finalConversationId,
                        finalContent
                      );
                  }
                  int totalInputToken =
                    conversationService.getTokenCountForHistory(finalConversationId);

                  log.info("Total input tokens part of next conversation: {}",
                    totalInputToken);


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
