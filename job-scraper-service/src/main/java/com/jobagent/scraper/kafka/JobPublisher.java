package com.jobagent.scraper.kafka;

import com.jobagent.common.dto.JobDto;
import com.jobagent.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobPublisher {

    private final KafkaTemplate<String, JobDto> template;

    public void publish(JobDto job) {
        template.send(KafkaTopics.RAW_JOBS, job.getExternalId(), job)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish job {}: {}", job.getExternalId(), ex.getMessage());
                    } else {
                        log.debug("Published job {} to {} partition {}", job.getExternalId(),
                                result.getRecordMetadata().topic(), result.getRecordMetadata().partition());
                    }
                });
    }
}