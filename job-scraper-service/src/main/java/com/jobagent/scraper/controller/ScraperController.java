package com.jobagent.scraper.controller;

import com.jobagent.scraper.service.JobScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scraper")
@RequiredArgsConstructor
public class ScraperController {

    private final JobScraperService scraperService;

    @PostMapping("/run")
    public ResponseEntity<JobScraperService.ScrapeSummary> runNow() {
        return ResponseEntity.ok(scraperService.runOnce());
    }
}