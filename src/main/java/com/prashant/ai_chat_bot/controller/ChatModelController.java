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
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.ModelResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/chatmodel")
@RequiredArgsConstructor
@Slf4j
public class ChatModelController {

    public static final String AI_PROCESSING_FAILED = "AI processing failed";
    private static final String CHAT_MEMORY_CONVERSATION_ID = "chat_memory_conversation_id";
    private final MultiModelProviderService multiModelProviderService;
    private final ConversationIdGenerator conversationIdGenerator;
    private final ChatMemory chatMemory;
    @Value("${app.ai.chat-memory.enabled:false}")
    private boolean defaultChatMemoryEnabled;

    @PostMapping("/chat")
    public String chat(@RequestHeader(value= AIProviderConstants.AI_PROVIDER_HEADER, required = false,defaultValue = AIProviderConstants.OLLAMA) String aiProvider,
      @RequestBody String messageInput) {
        messageInput = InputSanitizer.sanitize(messageInput);
        return multiModelProviderService.getChatClient(aiProvider)
                .prompt()
                .user(messageInput)
                .call()
                .content();
    }

    @PostMapping("/chat/conversation")
    public ResponseEntity<String> chat(
      @RequestParam(value = "conversationId", required = false) Integer conversationId,
      @RequestHeader(value= AIProviderConstants.AI_PROVIDER_HEADER, required = false,defaultValue = AIProviderConstants.OLLAMA) String aiProvider,
      @RequestBody String messageInput) {
        try {
            messageInput = InputSanitizer.sanitize(messageInput);

            if (messageInput == null || messageInput.isBlank()) {
                return ResponseEntity.badRequest().body("Message input cannot be empty");
            }
            //create conversation id if it doesnt exist
            conversationId = Optional.ofNullable(conversationId)
              .orElseGet(conversationIdGenerator::nextId);
            Integer finalConversationId = conversationId;

            ChatClient chatClient = multiModelProviderService.getChatClient(aiProvider);

            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
              .user(messageInput)
              //MessageChatMemoryAdvisor reads this param and uses it to decide which memory thread/history bucket to load and update for that request.
              //Without it, the advisor falls back to its default conversation id
              .advisors(advisorSpec -> advisorSpec.param(CHAT_MEMORY_CONVERSATION_ID, String.valueOf(finalConversationId)));
            if (!defaultChatMemoryEnabled) {
                requestSpec = requestSpec.advisors(
                  MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(String.valueOf(finalConversationId))
                    .build());
            }

            ChatResponse chatResponse = requestSpec.call().chatResponse();

            String content = Optional.ofNullable(chatResponse)
              .map(ChatResponse::getResult)
              .map(ModelResult::getOutput)
              .map(AssistantMessage::getText)
              .filter(s -> !s.isBlank())
              .orElseThrow(() ->
                new IllegalStateException("AI returned empty response"));

            int totalTokenCount = Optional.ofNullable(chatResponse)
              .map(ChatResponse::getMetadata)
              .map(ChatResponseMetadata::getUsage)
              .map(Usage::getTotalTokens)
              .orElse(0);

            log.info("Total tokens consumed for conversation {}: {}", finalConversationId, totalTokenCount);

            return ResponseEntity.ok(content);
        } catch (IllegalArgumentException e) {
            log.error(AI_PROCESSING_FAILED, e);
            return generateErrorResponse();
        }
        catch (Exception e) {
            log.error(AI_PROCESSING_FAILED, e);
            return generateErrorResponse();
        }

    }

    private ResponseEntity<String> generateErrorResponse(){
        return ResponseEntity.internalServerError()
          .body(AI_PROCESSING_FAILED);
    }


}
