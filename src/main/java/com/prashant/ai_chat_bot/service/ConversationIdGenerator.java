package com.prashant.ai_chat_bot.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ConversationIdGenerator {

  private final AtomicInteger sequence = new AtomicInteger(1);

  public Integer nextId() {
    return sequence.getAndIncrement();
  }
}
