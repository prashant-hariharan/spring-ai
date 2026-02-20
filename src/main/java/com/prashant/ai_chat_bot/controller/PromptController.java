package com.prashant.ai_chat_bot.controller;

import com.prashant.ai_chat_bot.model.CodeReviewDTO;
import com.prashant.ai_chat_bot.model.TicketAnalysis;
import com.prashant.ai_chat_bot.service.MultiModelProviderService;
import com.prashant.ai_chat_bot.service.UserPromptService;
import com.prashant.ai_chat_bot.utils.InputSanitizer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prompts")
@AllArgsConstructor
@Slf4j
public class PromptController {

  private final MultiModelProviderService multiModelProviderService;
  private final UserPromptService userPromptService;

  @PostMapping("/analyze-code")
  public String analyzeCode(
    @RequestHeader(value = "ai-provider", required = false, defaultValue = "ollama") String aiProvider,
    @RequestBody CodeReviewDTO codeReviewDTO) {
    codeReviewDTO.sanitizeInput();
    Prompt prompt = userPromptService.createCodeReviewPrompt(codeReviewDTO.getLanguage(), codeReviewDTO.getCode(), codeReviewDTO.getBusinessRequirements());

    return multiModelProviderService.getChatClient(aiProvider)
      .prompt()
      .user(prompt.getContents())
      .call()
      .content();
  }

  @PostMapping("/analyze-ticket")
  public TicketAnalysis analyzeTicket(
    @RequestHeader(value = "ai-provider", required = false, defaultValue = "ollama") String aiProvider,
    @RequestBody String ticketText) {
    ticketText = InputSanitizer.sanitize(ticketText);
    Prompt prompt = userPromptService.createTicketAnalysisPrompt(ticketText);
    return multiModelProviderService.getChatClient(aiProvider)
      .prompt()
      .user(prompt.getContents())
      .call()
      .entity(TicketAnalysis.class);

  }
}
