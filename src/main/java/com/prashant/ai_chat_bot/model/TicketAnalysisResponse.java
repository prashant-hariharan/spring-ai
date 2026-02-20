package com.prashant.ai_chat_bot.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TicketAnalysisResponse {
  private TicketAnalysis ticketAnalysis;
  private List<BespokeResponse> bespokeResponses;
}
