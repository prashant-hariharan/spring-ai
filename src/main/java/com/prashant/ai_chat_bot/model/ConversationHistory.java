package com.prashant.ai_chat_bot.model;


import com.prashant.ai_chat_bot.utils.TokenCounter;
import lombok.Getter;
import org.springframework.ai.chat.messages.Message;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Model class representing a conversation's history with an AI.
 *
 * KEY CONCEPT: Conversation State Management
 *
 * Since LLMs are stateless (each API call is independent), we need to:
 * 1. Store all messages exchanged in a conversation
 * 2. Replay relevant history with each new request
 * 3. Manage token limits to avoid exceeding context windows
 *
 * This class encapsulates:
 * - Message storage (user and assistant messages)
 * - Token counting (approximate estimation)
 * - Sliding window retrieval (get recent messages within token budget)
 * - Metadata tracking (timestamps, message counts)
 *

 *
 * @author prashant
 * @author HungryCoders - Original Version
 */
@Getter
public class ConversationHistory {

  private final Integer conversationId;

  private final List<TokenizedMessage> messages;

  private final LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private int totalInputTokens;

  private int totalTokensConsumedInConversation;


  public ConversationHistory(Integer conversationId) {
    this.conversationId = conversationId;
    this.messages = new ArrayList<>();
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.totalInputTokens = 0;
    this.totalTokensConsumedInConversation = 0;
  }

  /**
   * Adds a message to the conversation history.
   *

   * TOKEN ESTIMATION:
   * We use a simple heuristic: ~4 characters per token.
   *
   * For PRODUCTION applications, consider using:
   * - tiktoken library (OpenAI's actual tokenizer)
   * - Model-specific tokenizer APIs
   * - Spring AI's built-in token counting (if available)
   *
   * @param message The Message (UserMessage or AssistantMessage) to add
   */
  public void addMessage(Message message) {

    this.updatedAt = LocalDateTime.now();
    int tokens = TokenCounter.countTokens(message.getText());
    TokenizedMessage tokenizedMessage = new TokenizedMessage(message, tokens);
    this.messages.add(tokenizedMessage);
    this.totalInputTokens += tokens;

  }

  public void updateResponseToken(int tokens){
    this.totalTokensConsumedInConversation += tokens;
  }

  /**
   * Returns a COPY of all messages in this conversation.
   *
   * WHY RETURN A COPY?
   * - Encapsulation: Prevents external code from modifying internal state
   * - Thread safety: Callers can iterate without ConcurrentModificationException
   * - Predictability: Changes to returned list don't affect conversation
   *
   * This is a defensive programming best practice.
   *
   * @return A new ArrayList containing all messages (safe to modify)
   */
  public List<TokenizedMessage> getMessages() {
    return List.copyOf(messages);// Return copy
  }

  /**
   * Retrieves recent messages that fit within a token budget.
   *
   * KEY CONCEPT: Sliding Window / Truncation Strategy
   *
   * When conversation history exceeds the model's context window,
   * we must decide which messages to keep. Common strategies:
   *
   * 1. SLIDING WINDOW (used here):
   *    Keep the most recent N messages/tokens
   *    Pros: Recent context is most relevant
   *    Cons: Loses early conversation setup
   *
   * 2. SUMMARIZATION:
   *    Use AI to summarize old messages, keep summary + recent
   *    Pros: Retains key information
   *    Cons: Adds latency and cost
   *
   * 3. SELECTIVE RETENTION:
   *    Keep system prompt + first message + recent messages
   *    Pros: Preserves context setup
   *    Cons: More complex logic
   *
   * ALGORITHM:
   * 1. Start from the NEWEST message
   * 2. Add messages while within token budget
   * 3. Stop when adding next message would exceed limit
   * 4. Return messages in chronological order
   *
   * EXAMPLE:
   * Messages: [M1, M2, M3, M4, M5] (M5 is newest)
   * Token budget: 100
   * If M5=30, M4=40, M3=50 tokens:
   * - Add M5 (total: 30) ✓
   * - Add M4 (total: 70) ✓
   * - Add M3 (total: 120) ✗ exceeds budget
   * Result: [M4, M5]
   *
   * @param maxTokens Maximum token budget for returned messages
   * @return List of recent messages fitting within the token limit
   */
  public List<Message> getRecentMessages(int maxTokens) {

    if (messages.isEmpty() || maxTokens <= 0) {
      return Collections.emptyList();
    }

    int currentTokens = 0;
    Deque<Message> buffer = new ArrayDeque<>();

    for (int i = messages.size() - 1; i >= 0; i--) {
      TokenizedMessage tm = messages.get(i);
      if (currentTokens + tm.tokenCount() > maxTokens) {
        break;
      }
      buffer.addFirst(tm.message());
      currentTokens += tm.tokenCount();
    }

    return new ArrayList<>(buffer);
  }




  /**
   * @return Total number of messages in the conversation
   */
  public int getMessageCount() {
    return messages.size();
  }


}
