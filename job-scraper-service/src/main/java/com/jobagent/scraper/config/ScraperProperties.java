package com.jobagent.scraper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "scraper")
public class ScraperProperties {

    private List<String> keywords = List.of("java", "spring boot", "backend");
    private List<String> locations = List.of("remote", "bangalore");
    private int maxResultsPerQuery = 25;
    private Duration requestTimeout = Duration.ofSeconds(15);
    private String userAgent =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0 Safari/537.36";

    private Sources sources = new Sources();
    private Dedup dedup = new Dedup();

    @Data
    public static class Sources {
        private boolean mockEnabled = true;
        private boolean naukriEnabled = false;
        private boolean linkedinEnabled = false;
    }

    @Data
    public static class Dedup {
        private Duration ttl = Duration.ofDays(14);
        private String keyPrefix = "jobagent:seen:";
    }
}