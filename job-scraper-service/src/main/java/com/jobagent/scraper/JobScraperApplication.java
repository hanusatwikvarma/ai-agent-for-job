package com.jobagent.scraper;

import com.jobagent.scraper.config.ScraperProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ScraperProperties.class)
public class JobScraperApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobScraperApplication.class, args);
    }
}