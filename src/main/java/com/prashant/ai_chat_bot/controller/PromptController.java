package com.prashant.ai_chat_bot.controller;

import com.prashant.ai_chat_bot.model.BespokeResponse;
import com.prashant.ai_chat_bot.model.CodeReviewDTO;
import com.prashant.ai_chat_bot.model.TicketAnalysis;
import com.prashant.ai_chat_bot.model.TicketAnalysisResponse;
import com.prashant.ai_chat_bot.service.MultiModelProviderService;
import com.prashant.ai_chat_bot.service.UserPromptService;
import com.prashant.ai_chat_bot.utils.AIProviderConstants;
import com.prashant.ai_chat_bot.utils.InputSanitizer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prompts")
@AllArgsConstructor
@Slf4j
public class PromptController {

  private final MultiModelProviderService multiModelProviderService;
  private final UserPromptService userPromptService;

  @PostMapping("/analyze-code")
  public String analyzeCode(
    @RequestHeader(value = AIProviderConstants.AI_PROVIDER_HEADER, required = false, defaultValue = AIProviderConstants.OLLAMA) String aiProvider,
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
  public TicketAnalysisResponse analyzeTicket(
    @RequestHeader(value = AIProviderConstants.AI_PROVIDER_HEADER, required = false, defaultValue = AIProviderConstants.OLLAMA) String aiProvider,
    @RequestBody String ticketText) {
    ticketText = InputSanitizer.sanitize(ticketText);
    Prompt prompt = userPromptService.createTicketAnalysisPrompt(ticketText);
    TicketAnalysis ticketAnalysis =  multiModelProviderService.getChatClient(aiProvider)
      .prompt()
      .user(prompt.getContents())
      .call()
      .entity(TicketAnalysis.class);

    TicketAnalysisResponse response = TicketAnalysisResponse.builder().ticketAnalysis(ticketAnalysis).build();

    List<TicketAnalysis.TicketPriority> bespokeResponseTrigger = List.of(TicketAnalysis.TicketPriority.HIGH, TicketAnalysis.TicketPriority.URGENT);
    if(bespokeResponseTrigger.contains(ticketAnalysis.getPriority())){
      Prompt bespokeResponsePrompt = userPromptService
        .createBespokeResponsePrompt(ticketAnalysis.getCategory(), ticketAnalysis.getKeyIssues());
      //irrespective of what was the original provider, use gemini for bespoke response
      List<BespokeResponse> bespokeResponses = multiModelProviderService.getChatClient(AIProviderConstants.GEMINI)
        .prompt()
        .user(bespokeResponsePrompt.getContents())
        .call()
        .entity(new ParameterizedTypeReference<List<BespokeResponse>>() {});
      
        if(!CollectionUtils.isEmpty(bespokeResponses)){
          response.setBespokeResponses(bespokeResponses);
        }
    }

    return response;

  }
}
