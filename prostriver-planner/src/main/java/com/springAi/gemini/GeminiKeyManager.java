package com.springAi.gemini;

import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Service
public class GeminiKeyManager {

    private static final Logger log = LoggerFactory.getLogger(GeminiKeyManager.class);
    private static final String COOLDOWN_PREFIX = "gemini:cooldown:";
    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final List<ChatClient> chatClients;
    private final StringRedisTemplate redis;
    private final AtomicInteger cursor = new AtomicInteger(0);

    public GeminiKeyManager(@Value("${gemini.api-keys}") List<String> apiKeys,
                            @Value("${gemini.model:gemini-2.5-flash}") String model,
                            StringRedisTemplate redis) {
        this.redis = redis;
        this.chatClients = apiKeys.stream()
                .map(key -> buildClient(key.trim(), model))
                .toList();
        log.info("Initialized {} Gemini clients for rotation", chatClients.size());
    }

    private ChatClient buildClient(String apiKey, String model) {
        GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                .genAiClient(Client.builder().apiKey(apiKey).build())
                .defaultOptions(GoogleGenAiChatOptions.builder().model(model).build())
                .build();
        return ChatClient.builder(chatModel).build();
    }

    public <T> T executeWithRotation(Function<ChatClient, T> call) {
        int n = chatClients.size();
        int start = Math.floorMod(cursor.getAndIncrement(), n);

        for (int offset = 0; offset < n; offset++) {
            int idx = (start + offset) % n;

            if (isOnCooldown(idx)) {
                continue;
            }
            try {
                return call.apply(chatClients.get(idx));
            } catch (Exception e) {
                if (isQuotaError(e)) {
                    log.warn("Gemini key {} hit its quota — cooling down 60s and rotating", idx);
                    markCooldown(idx);
                } else {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("All Gemini keys are exhausted or on cooldown");
    }

    private boolean isOnCooldown(int idx) {
        return Boolean.TRUE.equals(redis.hasKey(COOLDOWN_PREFIX + idx));
    }

    private void markCooldown(int idx) {
        redis.opsForValue().set(COOLDOWN_PREFIX + idx, "cooling", COOLDOWN);
    }

    private boolean isQuotaError(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String msg = t.getMessage();
            if (msg != null) {
                String m = msg.toUpperCase();
                if (m.contains("429") || m.contains("RESOURCE_EXHAUSTED") || m.contains("QUOTA")) {
                    return true;
                }
            }
        }
        return false;
    }
}