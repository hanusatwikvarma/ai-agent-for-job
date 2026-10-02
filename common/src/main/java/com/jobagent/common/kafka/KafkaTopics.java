package com.jobagent.common.kafka;

public final class KafkaTopics {

    public static final String RAW_JOBS = "raw-jobs";
    public static final String MATCHED_JOBS = "matched-jobs";
    public static final String APPLICATION_EVENTS = "application-events";
    public static final String NOTIFICATION_EVENTS = "notification-events";

    private KafkaTopics() {
    }
}