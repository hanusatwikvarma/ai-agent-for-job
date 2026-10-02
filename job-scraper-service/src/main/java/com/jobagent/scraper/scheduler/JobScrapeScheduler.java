package com.jobagent.scraper.scheduler;

import com.jobagent.scraper.service.JobScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScrapeScheduler {

    private final JobScraperService scraperService;

    /**
     * Runs the full scrape sweep on the cron defined in config.
     * Default: every 3 hours (0 0 * / 3 * * *).
     */
    @Scheduled(cron = "${scraper.cron:0 0 */3 * * *}", zone = "${scraper.timezone:UTC}")
    public void scheduledScrape() {
        log.info("Scheduled scrape triggered");
        scraperService.runOnce();
    }
}