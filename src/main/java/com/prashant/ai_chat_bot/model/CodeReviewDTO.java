package com.prashant.ai_chat_bot.model;

import com.prashant.ai_chat_bot.utils.InputSanitizer;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class CodeReviewDTO {
  private String code;
  private String language;
  private String businessRequirements;

  public void sanitizeInput(){
    this.code = InputSanitizer.sanitize(this.code);
    this.language = InputSanitizer.sanitize(this.language);
    this.businessRequirements = StringUtils.hasText(businessRequirements)
      ? InputSanitizer.sanitize(this.businessRequirements) : "No business requirements provided. Review based on technical quality of the code only.";
  }
}
