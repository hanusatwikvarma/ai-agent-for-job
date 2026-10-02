package com.jobagent.scraper.service;

import com.jobagent.common.dto.JobDto;
import com.jobagent.scraper.config.ScraperProperties;
import com.jobagent.scraper.kafka.JobPublisher;
import com.jobagent.scraper.source.JobSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobScraperService {

    private final List<JobSource> sources;
    private final JobDedupService dedup;
    private final JobPublisher publisher;
    private final ScraperProperties properties;

    public ScrapeSummary runOnce() {
        int fetched = 0;
        int published = 0;
        int skipped = 0;

        for (JobSource source : sources) {
            if (!source.isEnabled()) continue;

            for (String keyword : properties.getKeywords()) {
                for (String location : properties.getLocations()) {
                    List<JobDto> jobs;
                    try {
                        jobs = source.fetch(keyword, location, properties.getMaxResultsPerQuery());
                    } catch (RuntimeException ex) {
                        log.warn("Source {} failed for {}/{}: {}",
                                source.portal(), keyword, location, ex.getMessage());
                        continue;
                    }

                    for (JobDto job : jobs) {
                        fetched++;
                        if (job.getExternalId() == null) {
                            skipped++;
                            continue;
                        }
                        if (dedup.markSeenIfAbsent(job.getExternalId())) {
                            publisher.publish(job);
                            published++;
                        } else {
                            skipped++;
                        }
                    }
                }
            }
        }

        ScrapeSummary summary = new ScrapeSummary(fetched, published, skipped);
        log.info("Scrape complete: {}", summary);
        return summary;
    }

    public record ScrapeSummary(int fetched, int published, int skipped) {}
}