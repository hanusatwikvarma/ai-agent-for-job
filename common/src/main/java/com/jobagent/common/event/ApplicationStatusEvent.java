package com.jobagent.common.event;

import com.jobagent.common.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusEvent {
    private String applicationId;
    private String jobExternalId;
    private String jobTitle;
    private String company;
    private ApplicationStatus previousStatus;
    private ApplicationStatus newStatus;
    private String userEmail;
    private Instant changedAt;
}