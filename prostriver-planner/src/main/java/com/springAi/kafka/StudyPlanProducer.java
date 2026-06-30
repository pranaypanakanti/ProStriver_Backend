package com.springAi.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class StudyPlanProducer {

    private static final Logger log = LoggerFactory.getLogger(StudyPlanProducer.class);
    private static final String TOPIC = "study-plan-jobs";

    private final KafkaTemplate<String, StudyPlanJobMessage> kafkaTemplate;

    public StudyPlanProducer(KafkaTemplate<String, StudyPlanJobMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String userId, String jobId) {
        StudyPlanJobMessage message = new StudyPlanJobMessage(jobId, userId);

        kafkaTemplate.send(TOPIC, userId, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish job {}: {}", jobId, ex.getMessage());
                    } else {
                        log.info("Published job {} → partition={} offset={}",
                                jobId,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}