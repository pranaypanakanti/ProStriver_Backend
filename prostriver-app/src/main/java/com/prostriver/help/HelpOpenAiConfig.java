package com.prostriver.help;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * The help feature's own ChatClient, built explicitly from the OpenAI chat model.
 *
 * <p>Two model providers are on the classpath, so this deliberately does not inject an unqualified
 * {@code ChatClient.Builder}. It takes {@link OpenAiChatModel} by concrete type, which cannot
 * resolve to the Gemini side no matter what else registers later.
 *
 * <p>Nothing here touches {@code GeminiKeyManager}. The study plan pipeline keeps building its own
 * rotating clients by hand and is unaffected.
 *
 * <p>This also registers {@link HelpProperties}, because {@code com.prostriver.config.PropertiesConfig}
 * is not a file this feature is allowed to modify.
 */
@Configuration
@Profile("api")
@EnableConfigurationProperties(HelpProperties.class)
public class HelpOpenAiConfig {

    /**
     * Named so {@code @Qualifier("helpChatClient")} in {@link HelpService} resolves to exactly this
     * bean and never to an autoconfigured one.
     */
    @Bean
    public ChatClient helpChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
