package com.prostriver.help;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the help endpoint, bound from {@code prostriver.help}.
 *
 * <p>Thresholds are expressed as cosine similarity, where higher is closer, so they read the way
 * they are reasoned about. pgvector returns distance, and the repository converts once.
 *
 * <p>Note that {@code prostriver.help.reindex} is deliberately not a field here. It is read by
 * {@code @ConditionalOnProperty} on {@link HelpIndexCommand} before any binding happens, and unknown
 * fields are ignored by default, so passing it on the command line does not break this binding.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "prostriver.help")
public class HelpProperties {

    /** At or above this similarity, the model is allowed to answer from the matched document. */
    private double answerThreshold;

    /** Below this similarity, the question is treated as nothing to do with the documentation. */
    private double scopeThreshold;

    private int rateLimitPerHour;

    private int cacheTtlDays;

    private int maxQuestionChars;
}
