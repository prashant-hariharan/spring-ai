package com.prashant.ai_chat_bot.controller;

import com.prashant.ai_chat_bot.service.ConversationService;
import com.prashant.ai_chat_bot.service.MultiModelProviderService;
import com.prashant.ai_chat_bot.utils.InputSanitizer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/chatmodel")
@AllArgsConstructor
@Slf4j
public class ChatModelController {

    private final MultiModelProviderService multiModelProviderService;
    private final ConversationService conversationService;

    @PostMapping("/chat")
    public String chat(@RequestHeader(value="ai-provider", required = false,defaultValue = "ollama") String aiProvider,
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
      @RequestHeader(value="ai-provider", required = false,defaultValue = "ollama") String aiProvider,
      @RequestBody String messageInput) {
        try {
            messageInput = InputSanitizer.sanitize(messageInput);

            if (messageInput == null || messageInput.isBlank()) {
                return ResponseEntity.badRequest().body("Message input cannot be empty");
            }
            //create conversation id if it doesnt exist
            conversationId = Optional.ofNullable(conversationId)
              .orElseGet(conversationService::generateConversationId);

            //Add user message to conversation service
            conversationService.addUserMessage(conversationId, messageInput);

            //List of messages to be sent to LLM
            List<Message> messagesFromHistory = conversationService.getRecentMessages(conversationId);

            ChatClient chatClient = multiModelProviderService.getChatClient(aiProvider);

            ChatResponse chatResponse = chatClient.prompt().messages(messagesFromHistory).call().chatResponse();

            String content = Optional.ofNullable(chatResponse)
              .map(ChatResponse::getResult)
              .map(result -> result.getOutput())
              .map(Object::toString)
              .filter(s -> !s.isBlank())
              .orElseThrow(() ->
                new IllegalStateException("AI returned empty response"));
            //adds ai output to conversation
            conversationService.addAssistantMessage(conversationId,content);

            int totalTokenCount = Optional.ofNullable(chatResponse)
              .map(ChatResponse::getMetadata)
              .map(metadata -> metadata.getUsage())
              .map(usage -> usage.getTotalTokens())
              .orElse(0);

            int totalConsumedTokenInConversation = conversationService.updateTotalTokenCount(conversationId,totalTokenCount);
            log.info("Total tokens consumed in conversation: {}",totalConsumedTokenInConversation);

            int totalInputToken = conversationService.getTokenCountForHistory(conversationId);
            log.info("Total input tokens part of next conversation: {}",totalInputToken);

            return ResponseEntity.ok(content);
        } catch (IllegalArgumentException e) {
            log.error("AI processing failed", e);
            return generateErrorResponse();
        }
        catch (Exception e) {
            log.error("AI processing failed", e);
            return generateErrorResponse();
        }

    }

    private ResponseEntity generateErrorResponse(){
        return ResponseEntity.internalServerError()
          .body("AI processing failed");
    }


}
