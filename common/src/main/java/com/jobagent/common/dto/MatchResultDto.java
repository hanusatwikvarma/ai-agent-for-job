package com.jobagent.common.dto;

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
public class MatchResultDto {
    private String jobExternalId;
    private int fitScore;
    private List<String> strengths;
    private List<String> gaps;
    private String recommendation;
    private String rationale;
    private Instant scoredAt;
}