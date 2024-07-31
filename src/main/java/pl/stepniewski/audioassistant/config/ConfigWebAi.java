package pl.stepniewski.audioassistant.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ConfigWebAi {

    @Value("${openai.api.url}")
    private String url;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.chat.model}")
    private String model;

    @Value("${openai.api.chat.temperature}")
    private double temperature;

    @Bean
    public WebClient whisperWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .build();
    }
}
