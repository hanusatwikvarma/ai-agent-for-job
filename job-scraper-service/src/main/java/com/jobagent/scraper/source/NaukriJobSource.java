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

@Slf4j
@Component
@RequiredArgsConstructor
public class NaukriJobSource implements JobSource {

    private static final String SEARCH_URL = "https://www.naukri.com/%s-jobs-in-%s";
    private final ScraperProperties properties;

    @Override
    public JobPortal portal() {
        return JobPortal.NAUKRI;
    }

    @Override
    public boolean isEnabled() {
        return properties.getSources().isNaukriEnabled();
    }

    @Override
    public List<JobDto> fetch(String keyword, String location, int limit) {
        String url = SEARCH_URL.formatted(
                URLEncoder.encode(keyword.replace(" ", "-"), StandardCharsets.UTF_8),
                URLEncoder.encode(location.replace(" ", "-"), StandardCharsets.UTF_8));

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(properties.getUserAgent())
                    .timeout((int) properties.getRequestTimeout().toMillis())
                    .referrer("https://www.google.com")
                    .get();

            Elements cards = doc.select("div.srp-jobtuple-wrapper, article.jobTuple");
            if (cards.isEmpty()) {
                log.warn("Naukri returned 0 cards for keyword={} location={}. " +
                        "The DOM likely changed or the request was blocked. " +
                        "Falling back to no results.", keyword, location);
                return Collections.emptyList();
            }

            List<JobDto> out = new ArrayList<>();
            for (Element card : cards) {
                if (out.size() >= limit) break;
                out.add(parseCard(card));
            }
            return out;

        } catch (IOException e) {
            log.warn("Naukri scrape failed for {} / {}: {}", keyword, location, e.getMessage());
            return Collections.emptyList();
        }
    }

    private JobDto parseCard(Element card) {
        String title = text(card, "a.title, a.jobTitle");
        String company = text(card, "a.comp-name, a.companyName");
        String location = text(card, "span.locWdth, li.location");
        String experience = text(card, "span.expwdth, li.experience");
        String salary = text(card, "span.sal, li.salary");
        String applyUrl = card.selectFirst("a.title, a.jobTitle") != null
                ? card.selectFirst("a.title, a.jobTitle").absUrl("href")
                : null;
        String externalId = applyUrl == null ? null
                : applyUrl.replaceAll(".*/", "").split("\\?")[0];

        return JobDto.builder()
                .externalId("NAUKRI-" + externalId)
                .portal(JobPortal.NAUKRI)
                .title(title)
                .company(company)
                .location(location)
                .experienceRange(experience)
                .salaryRange(salary)
                .applyUrl(applyUrl)
                .employmentType(EmploymentType.UNKNOWN)
                .scrapedAt(Instant.now())
                .build();
    }

    private static String text(Element parent, String selector) {
        Element el = parent.selectFirst(selector);
        return el == null ? null : el.text().trim();
    }
}