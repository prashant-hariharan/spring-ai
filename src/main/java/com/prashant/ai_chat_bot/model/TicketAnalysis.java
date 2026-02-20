package com.prashant.ai_chat_bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketAnalysis {
  private String category;
  private String priority;
  private String sentiment;
  private String summary;
  private String suggestedResolution;
  private int estimatedResolutionTime;
}
