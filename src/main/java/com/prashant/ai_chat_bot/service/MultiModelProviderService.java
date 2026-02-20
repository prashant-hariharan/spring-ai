package com.prashant.ai_chat_bot.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@Getter
@Slf4j
@AllArgsConstructor
public class MultiModelProviderService {

  private final Map<String, ChatClient> chatClients;

  public ChatClient getChatClient(String aiProvider) {
    String provider = StringUtils.hasText(aiProvider)
      ? aiProvider
      : "ollama";

    ChatClient client = chatClients.getOrDefault(provider, chatClients.get("ollama"));

    log.info("Chat client selected for provider: {}", provider);

    return client;
  }
}
