package com.prashant.ai_chat_bot.service;

import com.prashant.ai_chat_bot.model.ConversationHistory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing conversation history and state.
 *
 * KEY CONCEPT: Conversation Memory Management
 *
 * LLMs are STATELESS - they don't remember previous messages in a conversation.
 * This service acts as the "memory" layer, storing conversation history so we
 * can replay it with each new AI request.
 *
 * RESPONSIBILITIES:
 * 1. Store conversations (in-memory, keyed by conversationId)
 * 2. Add user and assistant messages to conversations
 * 3. Retrieve messages (all or within token limits)
 * 4. Manage conversation lifecycle (create, clear, list)
 *
 * STORAGE STRATEGY: In-Memory (ConcurrentHashMap)
 *
 * PROS:
 * - Fast access (O(1) lookup)
 * - Simple implementation
 * - Good for development/prototyping
 *
 * CONS:
 * - Data lost on server restart
 * - Doesn't scale horizontally (each server has its own memory)
 * - Memory grows with active conversations
 *
 * PRODUCTION ALTERNATIVES:
 * - Redis: Fast, supports TTL, horizontally scalable
 * - PostgreSQL/MongoDB: Persistent, queryable
 * - Spring AI's built-in ChatMemory implementations
 *
 * @author HungryCoders
 */
@Service
public class ConversationService {


  private final Map<Integer, ConversationHistory> conversations = new ConcurrentHashMap<>();


  private static final int DEFAULT_TOKEN_LIMIT = 4000;


  public ConversationHistory getConversation(Integer conversationId) {
    return conversations.computeIfAbsent(
      conversationId,
      id -> new ConversationHistory(id)  // Lambda: only called if key missing
    );
  }

  /**
   * Adds a user message to the conversation history.
   *
   * SPRING AI MESSAGE TYPES:
   *
   * - UserMessage: What the human said
   *   → Sent to AI as: { "role": "user", "content": "..." }
   *
   * - AssistantMessage: What the AI replied
   *   → Sent to AI as: { "role": "assistant", "content": "..." }
   *
   * - SystemMessage: Instructions for the AI (not shown to user)
   *   → Sent to AI as: { "role": "system", "content": "..." }
   *
   * The Message interface is Spring AI's abstraction that gets
   * converted to the appropriate format for each AI provider.
   *
   * @param conversationId Which conversation to add the message to
   * @param content The user's message text
   */
  public void addUserMessage(Integer conversationId, String content) {
    ConversationHistory history = getConversation(conversationId);
    history.addMessage(new UserMessage(content));
  }

  /**
   * Adds an AI assistant message to the conversation history.
   *
   * This is called AFTER receiving a response from the AI.
   * Storing the assistant's response ensures the AI "remembers"
   * what it said in subsequent interactions.
   *
   * CONVERSATION FLOW:
   * 1. User sends message → addUserMessage()
   * 2. Send history to AI → AI generates response
   * 3. Store AI response → addAssistantMessage()
   * 4. Next user message → Repeat with updated history
   *
   * @param conversationId Which conversation to add the message to
   * @param content The AI's response text
   */
  public void addAssistantMessage(Integer conversationId, String content) {
    ConversationHistory history = getConversation(conversationId);
    history.addMessage(new AssistantMessage(content));
  }

  public int updateTotalTokenCount(Integer conversationId,int totalTokenCount){
    ConversationHistory history = getConversation(conversationId);
    history.updateResponseToken(totalTokenCount);
    return history.getTotalTokensConsumedInConversation();
  }



  /**
   * Retrieves recent messages within a specified token budget.
   *
   * KEY CONCEPT: Sliding Window for Context Management
   *
   * When sending conversation history to the AI:
   * - Too few messages: AI loses important context
   * - Too many messages: Exceeds context window, increases cost/latency
   *
   * This method returns the most recent messages that fit within
   * the specified token limit, ensuring optimal context for the AI.
   *
   * EXAMPLE:
   * Conversation has 50 messages (10,000 tokens total)
   * maxTokens = 4000
   * Result: Most recent ~20 messages that fit in 4000 tokens
   *
   * @param conversationId Which conversation to retrieve from
   * @param maxTokens Maximum token budget for returned messages
   * @return List of recent messages within token limit
   */
  public List<Message> getRecentMessages(Integer conversationId, int maxTokens) {
    ConversationHistory history = getConversation(conversationId);
    return history.getRecentMessages(maxTokens);
  }

  public List<Message> getRecentMessages(Integer conversationId) {
    return getRecentMessages(conversationId, DEFAULT_TOKEN_LIMIT);
  }


  public void clearConversation(Integer conversationId) {
    conversations.remove(conversationId);
  }



  //find recent conversation id
  public Integer generateConversationId() {
    if (conversations == null || conversations.isEmpty()) {
      return 1;
    }

    int nextId = conversations.keySet()
      .stream()
      .mapToInt(Integer::intValue)
      .max()
      .orElse(0) + 1;

    return nextId == 0 ? 1 : nextId;
  }

  public Integer getTokenCountForHistory(Integer conversationId){
    ConversationHistory history = getConversation(conversationId);
    return history.getTotalInputTokens();
  }
}
