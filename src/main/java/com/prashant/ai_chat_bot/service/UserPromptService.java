package com.prashant.ai_chat_bot.service;

import com.prashant.ai_chat_bot.utils.PromptReaderUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class UserPromptService {
  private final ResourceLoader resourceLoader;

  public Prompt createCodeReviewPrompt(String language, String code, String businessRequirements) {
    String prompt = PromptReaderUtil.getPrompt(resourceLoader, "classpath:/prompts/code-review.txt");
    String renderedPrompt = applyPlaceholders(prompt, Map.of(
      "language", language,
      "code", code,
      "businessRequirements", businessRequirements
    ));
    return new Prompt(renderedPrompt);
  }

  public Prompt createTicketAnalysisPrompt(String ticketText) {
    String prompt = PromptReaderUtil.getPrompt(resourceLoader, "classpath:/prompts/ticket-analysis.txt");
    String renderedPrompt = applyPlaceholders(prompt, Map.of(
      "ticketText", ticketText
    ));
    return new Prompt(renderedPrompt);
  }

  public Prompt createBespokeResponsePrompt(String category, String keyIssues) {
    String prompt = PromptReaderUtil.getPrompt(resourceLoader, "classpath:/prompts/bespoke-responses.txt");
    String renderedPrompt = applyPlaceholders(prompt, Map.of(
      "category", category,
      "issues", keyIssues
    ));
    return new Prompt(renderedPrompt);
  }

  private String applyPlaceholders(String template, Map<String, Object> values) {
    String rendered = template;
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      String replacement = entry.getValue() == null ? "" : entry.getValue().toString();
      rendered = rendered.replace("{" + entry.getKey() + "}", replacement);
    }
    return rendered;
  }
}
