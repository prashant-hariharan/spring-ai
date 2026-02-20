package com.prashant.ai_chat_bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.ai.chat.messages.Message;

@AllArgsConstructor
@Data
public final class TokenizedMessage {
  private final Message message;
  private final int tokenCount;
}
