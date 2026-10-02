package com.jobagent.common.dto;

import com.jobagent.common.enums.EmploymentType;
import com.jobagent.common.enums.JobPortal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private String externalId;
    private JobPortal portal;
    private String title;
    private String company;
    private String location;
    private String description;
    private String applyUrl;
    private String experienceRange;
    private String salaryRange;
    private EmploymentType employmentType;
    private List<String> skills;
    private Instant postedAt;
    private Instant scrapedAt;
}