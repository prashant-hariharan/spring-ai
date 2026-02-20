package com.prashant.ai_chat_bot.config;

import com.prashant.ai_chat_bot.utils.PromptReaderUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(AIProviderProperties.class)
public class MultiModelConfig {

  private final ResourceLoader resourceLoader;

  public MultiModelConfig(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }


  @Bean("openai")
  @Primary
  public ChatClient openAIChatClient(OpenAiChatModel openAiChatModel,
      @Value("${spring.ai.openai.chat.options.max-tokens:0}") Integer openAiMaxTokens) {
    String systemPrompt = loadSystemPrompt("classpath:prompts/openai-system.txt", openAiMaxTokens);
    return ChatClient.builder(openAiChatModel)
        .defaultSystem(systemPrompt)
        .build();
  }

  @Bean("gemini")
  public ChatClient geminiChatClient(AIProviderProperties properties) {
    AIProviderProperties.Provider provider = requireProvider(properties, "gemini");
    Integer maxTokens = provider.getMaxTokens();
    String systemPrompt = loadSystemPrompt("classpath:prompts/gemini-system.txt", maxTokens);
    return ChatClient.builder(createOpenAiCompatibleModel(properties, "gemini"))
        .defaultSystem(systemPrompt)
        .build();
  }

  @Bean("ollama")
  public ChatClient ollamaChatClient(AIProviderProperties properties) {
    AIProviderProperties.Provider provider = requireProvider(properties, "ollama");
    Integer maxTokens = provider.getMaxTokens();
    String systemPrompt = loadSystemPrompt("classpath:prompts/ollama-system.txt", maxTokens);
    return ChatClient.builder(createOpenAiCompatibleModel(properties, "ollama"))
        .defaultSystem(systemPrompt)
        .build();
  }

  @Bean("groq")
  public ChatClient groqChatClient(AIProviderProperties properties) {
    AIProviderProperties.Provider provider = requireProvider(properties, "groq");
    Integer maxTokens = provider.getMaxTokens();
    String systemPrompt = loadSystemPrompt("classpath:prompts/groq-system.txt", maxTokens);
    return ChatClient.builder(createOpenAiCompatibleModel(properties, "groq"))
      .defaultSystem(systemPrompt)
      .build();
  }



  private OpenAiChatModel createOpenAiCompatibleModel(
    AIProviderProperties properties,
    String providerName) {
    //get provider properties based on model name
    AIProviderProperties.Provider provider = requireProvider(properties, providerName);

    OpenAiApi openAiApi = OpenAiApi.builder()
      .apiKey(provider.getApiKey())
      .baseUrl(provider.getBaseUrl())
      .completionsPath(provider.getCompletionPath())
      .build();

    OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder().model(provider.getModel());

    //optional params for temperature and max tokens
    Optional.ofNullable(provider.getTemperature())
      .ifPresent(optionsBuilder::temperature);

    Optional.ofNullable(provider.getMaxTokens())
      .ifPresent(optionsBuilder::maxTokens);

    return OpenAiChatModel.builder()
      .openAiApi(openAiApi)
      .defaultOptions(optionsBuilder.build())
      .build();
  }


  private AIProviderProperties.Provider requireProvider(AIProviderProperties properties, String providerName) {
    if (properties.getProviders() == null || !properties.getProviders().containsKey(providerName)) {
      throw new IllegalStateException("Missing spring.ai.providers." + providerName + " configuration");
    }
    return properties.getProviders().get(providerName);
  }



  private String loadSystemPrompt(String location, Integer maxTokens) {
    String prompt = PromptReaderUtil.getPrompt(resourceLoader,location);
    if (maxTokens != null && maxTokens > 0) {
      prompt = prompt.replace("${MAX_TOKENS}", maxTokens.toString());
    }
    return prompt;
  }
}
