package com.jobagent.scraper.source;

import com.jobagent.common.dto.JobDto;
import com.jobagent.common.enums.EmploymentType;
import com.jobagent.common.enums.JobPortal;
import com.jobagent.scraper.config.ScraperProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LinkedIn's public "guest" job search endpoint. Heavily rate-limited and
 * frequently blocked — prefer the official LinkedIn Jobs API or a paid
 * aggregator (RapidAPI/Adzuna/JSearch) for production.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkedInJobSource implements JobSource {

    private static final String GUEST_SEARCH =
            "https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search" +
                    "?keywords=%s&location=%s&start=0";

    private final ScraperProperties properties;

    @Override
    public JobPortal portal() {
        return JobPortal.LINKEDIN;
    }

    @Override
    public boolean isEnabled() {
        return properties.getSources().isLinkedinEnabled();
    }

    @Override
    public List<JobDto> fetch(String keyword, String location, int limit) {
        String url = GUEST_SEARCH.formatted(
                URLEncoder.encode(keyword, StandardCharsets.UTF_8),
                URLEncoder.encode(location, StandardCharsets.UTF_8));

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(properties.getUserAgent())
                    .timeout((int) properties.getRequestTimeout().toMillis())
                    .get();

            Elements cards = doc.select("li, div.base-card");
            if (cards.isEmpty()) {
                log.warn("LinkedIn returned 0 cards — likely blocked. keyword={} location={}", keyword, location);
                return Collections.emptyList();
            }

            List<JobDto> out = new ArrayList<>();
            for (Element card : cards) {
                if (out.size() >= limit) break;
                JobDto job = parseCard(card);
                if (job != null) out.add(job);
            }
            return out;

        } catch (IOException e) {
            log.warn("LinkedIn scrape failed for {} / {}: {}", keyword, location, e.getMessage());
            return Collections.emptyList();
        }
    }

    private JobDto parseCard(Element card) {
        Element titleEl = card.selectFirst("h3.base-search-card__title");
        Element companyEl = card.selectFirst("h4.base-search-card__subtitle");
        Element locEl = card.selectFirst("span.job-search-card__location");
        Element linkEl = card.selectFirst("a.base-card__full-link");

        if (titleEl == null || linkEl == null) return null;

        String applyUrl = linkEl.absUrl("href");
        String externalId = applyUrl.replaceAll(".*-(\\d+)(\\?.*)?$", "$1");

        return JobDto.builder()
                .externalId("LI-" + externalId)
                .portal(JobPortal.LINKEDIN)
                .title(titleEl.text().trim())
                .company(companyEl == null ? null : companyEl.text().trim())
                .location(locEl == null ? null : locEl.text().trim())
                .applyUrl(applyUrl)
                .employmentType(EmploymentType.UNKNOWN)
                .scrapedAt(Instant.now())
                .build();
    }
}