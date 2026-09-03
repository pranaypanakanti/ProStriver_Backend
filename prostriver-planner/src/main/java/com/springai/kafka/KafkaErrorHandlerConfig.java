package com.springai.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.springai.study_planner.job.JobStatus;
import com.springai.study_planner.job.StudyPlanJobRepository;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.time.Instant;

@Configuration
public class KafkaErrorHandlerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlerConfig.class);
    private static final String DLT_TOPIC = "study-plan-jobs.DLT";

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, StudyPlanJobMessage> kafkaTemplate,
                                            StudyPlanJobRepository jobRepository) {

        DeadLetterPublishingRecoverer dltRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(DLT_TOPIC, 0)
        );

        ConsumerRecordRecoverer recoverer = (record, ex) -> {
            dltRecoverer.accept(record, ex);

            Object value = record.value();
            if (value instanceof StudyPlanJobMessage message) {
                try {
                    jobRepository.findByJobId(message.getJobId()).ifPresent(job -> {
                        job.setStatus(JobStatus.FAILED);
                        job.setUpdatedAt(Instant.now());
                        jobRepository.save(job);
                    });
                } catch (Exception mongoEx) {
                    log.error("DLT recovery: failed to mark job {} FAILED in Mongo: {}",
                            message.getJobId(), mongoEx.getMessage());
                }
            } else {
                log.error("DLT recovery: record value was not a StudyPlanJobMessage — could not update job status");
            }
        };

        ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(2);
        backoff.setInitialInterval(2_000);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(10_000);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);

        handler.addNotRetryableExceptions(
                JsonProcessingException.class,
                IllegalArgumentException.class
        );

        return handler;
    }
}