package com.prashant.ai_chat_bot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketAnalysis {
  private String category;
  private TicketPriority priority;
  private String sentiment;
  private String summary;
  private String suggestedResolution;
  private int estimatedResolutionTime;
  private String keyIssues;

  public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    @JsonCreator
    public static TicketPriority fromValue(String value) {
      if (value == null || value.isBlank()) {
        return null;
      }

      String normalized = value
        .trim()
        .toUpperCase(Locale.ROOT)
        .replace("-", "_")
        .replace(" ", "_");

      return switch (normalized) {
        case "LOW", "P3" -> LOW;
        case "MEDIUM", "NORMAL", "P2" -> MEDIUM;
        case "HIGH", "P1" -> HIGH;
        case "URGENT", "CRITICAL", "BLOCKER", "SEV1", "SEV_1" -> URGENT;
        default -> throw new IllegalArgumentException(
          "Unsupported ticket priority: " + value
        );
      };
    }
  }
}
