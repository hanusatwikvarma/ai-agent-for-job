package com.jobagent.scraper.source;

import com.jobagent.common.dto.JobDto;
import com.jobagent.common.enums.JobPortal;

import java.util.List;

public interface JobSource {

    JobPortal portal();

    boolean isEnabled();

    List<JobDto> fetch(String keyword, String location, int limit);
}