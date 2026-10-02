package com.jobagent.scraper.source;

import com.jobagent.common.dto.JobDto;
import com.jobagent.common.enums.EmploymentType;
import com.jobagent.common.enums.JobPortal;
import com.jobagent.scraper.config.ScraperProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class MockJobSource implements JobSource {

    private final ScraperProperties properties;

    @Override
    public JobPortal portal() {
        return JobPortal.MOCK;
    }

    @Override
    public boolean isEnabled() {
        return properties.getSources().isMockEnabled();
    }

    @Override
    public List<JobDto> fetch(String keyword, String location, int limit) {
        int n = Math.min(limit, 5);
        return IntStream.range(0, n)
                .mapToObj(i -> buildSample(keyword, location, i))
                .toList();
    }

    private JobDto buildSample(String keyword, String location, int idx) {
        String id = "MOCK-" + UUID.randomUUID();
        int years = ThreadLocalRandom.current().nextInt(1, 8);
        return JobDto.builder()
                .externalId(id)
                .portal(JobPortal.MOCK)
                .title(capitalize(keyword) + " Engineer " + (idx + 1))
                .company("Acme Corp " + (char) ('A' + idx))
                .location(location)
                .description(("We are hiring a %s engineer in %s. " +
                        "You will design, build, and ship services at scale. " +
                        "Required: %s, REST APIs, SQL, cloud. Nice to have: Kafka, Kubernetes.")
                        .formatted(keyword, location, keyword))
                .applyUrl("https://example.com/apply/" + id)
                .experienceRange(years + "-" + (years + 2) + " years")
                .salaryRange("$" + (80 + idx * 10) + "k-$" + (120 + idx * 10) + "k")
                .employmentType(EmploymentType.FULL_TIME)
                .skills(List.of(keyword, "REST", "SQL", "Docker"))
                .postedAt(Instant.now().minusSeconds(3600L * ThreadLocalRandom.current().nextInt(1, 48)))
                .scrapedAt(Instant.now())
                .build();
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}