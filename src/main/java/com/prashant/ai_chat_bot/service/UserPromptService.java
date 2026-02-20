package com.prashant.ai_chat_bot.service;

import com.prashant.ai_chat_bot.utils.PromptReaderUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class UserPromptService {
  private final ResourceLoader resourceLoader;

  public Prompt createCodeReviewPrompt(String language, String code, String businessRequirements) {
    //read from code-review.txt and substitute language, code and business requirements

      String prompt = PromptReaderUtil.getPrompt(resourceLoader,"classpath:/prompts/code-review.txt");
      PromptTemplate promptTemplate = new PromptTemplate(prompt);
      Map<String,Object> substitutionVariables = Map.of(
        "language",language,
        "code",code,
        "businessRequirements",businessRequirements
      );
      return promptTemplate.create(substitutionVariables);


  }

  public Prompt createTicketAnalysisPrompt(String ticketText) {
        String prompt = PromptReaderUtil.getPrompt(resourceLoader,"classpath:/prompts/ticket-analysis.txt");
        PromptTemplate promptTemplate = new PromptTemplate(prompt);
        Map<String,Object> substitutionVariables = Map.of(
          "ticketText",ticketText
        );
        return promptTemplate.create(substitutionVariables);

    }

  public Prompt createBespokeResponsePrompt(String category, String keyIssues) {
    String prompt = PromptReaderUtil.getPrompt(resourceLoader,"classpath:/prompts/bespoke-responses.txt");
    PromptTemplate promptTemplate = new PromptTemplate(prompt);
    Map<String,Object> substitutionVariables = Map.of(
      "category",category,
      "issues",keyIssues
    );
    return promptTemplate.create(substitutionVariables);

  }
}
