package bughunteragent.config;

import bughunteragent.ai.BugHunterAgent;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AgentConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.base.url}")
    private String openAiBaseUrl;

    @Value("${openai.model.name}")
    private String openAiModelName;


    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .baseUrl(openAiBaseUrl)
                .modelName(openAiModelName)
                .timeout(Duration.ofMinutes(3))
                .maxRetries(3)
                .temperature(0.2)
                .build();
    }

    @Bean
    public BugHunterAgent bugHunterAgent(ChatModel chatModel, McpToolProvider mcpToolProvider) {
        return AiServices.builder(BugHunterAgent.class)
                .chatModel(chatModel)
                .toolProvider(mcpToolProvider)
                .build();
    }
}