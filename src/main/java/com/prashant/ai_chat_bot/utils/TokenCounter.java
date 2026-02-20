package com.prashant.ai_chat_bot.utils;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;

public class TokenCounter {

  private static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
  private static final Encoding encoding = registry.getEncoding("cl100k_base")
    .orElseThrow(() -> new IllegalStateException("Encoding not found"));

  public static int countTokens(String text) {
    return encoding.countTokens(text);
  }
}

