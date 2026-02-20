package com.prashant.ai_chat_bot.config;

import com.prashant.ai_chat_bot.utils.AIProviderConstants;
import com.prashant.ai_chat_bot.utils.PromptReaderUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;

import java.util.Optional;

@Configuration
@EnableConfigurationProperties(AIProviderProperties.class)
public class MultiModelConfig {

  private final ResourceLoader resourceLoader;
  @Value("${app.ai.llm-logging.enabled:false}")
  private boolean llmLoggingEnabled;

  public MultiModelConfig(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }


  @Bean(AIProviderConstants.OPENAI)
  @Primary
  public ChatClient openAIChatClient(OpenAiChatModel openAiChatModel,
      @Value("${spring.ai.openai.chat.options.max-tokens:0}") Integer openAiMaxTokens) {
    String systemPrompt = loadSystemPrompt("classpath:prompts/openai-system.txt", openAiMaxTokens);
    return applyLoggingAdvisor(ChatClient.builder(openAiChatModel))
        .defaultSystem(systemPrompt)
        .build();
  }

  @Bean(AIProviderConstants.GEMINI)
  public ChatClient geminiChatClient(AIProviderProperties properties) {
    AIProviderProperties.Provider provider = requireProvider(properties, AIProviderConstants.GEMINI);
    Integer maxTokens = provider.getMaxTokens();
    String systemPrompt = loadSystemPrompt("classpath:prompts/gemini-system.txt", maxTokens);
    return applyLoggingAdvisor(
        ChatClient.builder(createOpenAiCompatibleModel(properties, AIProviderConstants.GEMINI))
      )
        .defaultSystem(systemPrompt)
        .build();
  }

  @Bean(AIProviderConstants.OLLAMA)
  public ChatClient ollamaChatClient(AIProviderProperties properties) {
    AIProviderProperties.Provider provider = requireProvider(properties, AIProviderConstants.OLLAMA);
    Integer maxTokens = provider.getMaxTokens();
    String systemPrompt = loadSystemPrompt("classpath:prompts/ollama-system.txt", maxTokens);
    return applyLoggingAdvisor(
        ChatClient.builder(createOpenAiCompatibleModel(properties, AIProviderConstants.OLLAMA))
      )
        .defaultSystem(systemPrompt)
        .build();
  }

  @Bean(AIProviderConstants.GROQ)
  public ChatClient groqChatClient(AIProviderProperties properties) {
    AIProviderProperties.Provider provider = requireProvider(properties, AIProviderConstants.GROQ);
    Integer maxTokens = provider.getMaxTokens();
    String systemPrompt = loadSystemPrompt("classpath:prompts/groq-system.txt", maxTokens);
    return applyLoggingAdvisor(
        ChatClient.builder(createOpenAiCompatibleModel(properties, AIProviderConstants.GROQ))
      )
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

  private ChatClient.Builder applyLoggingAdvisor(ChatClient.Builder builder) {
    if (llmLoggingEnabled) {
      return builder.defaultAdvisors(new SimpleLoggerAdvisor());
    }
    return builder;
  }
}
