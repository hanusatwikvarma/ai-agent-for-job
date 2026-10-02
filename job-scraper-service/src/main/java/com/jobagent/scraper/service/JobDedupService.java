package com.jobagent.scraper.service;

import com.jobagent.scraper.config.ScraperProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobDedupService {

    private final StringRedisTemplate redis;
    private final ScraperProperties properties;

    public boolean markSeenIfAbsent(String externalId) {
        String key = properties.getDedup().getKeyPrefix() + externalId;
        Boolean set = redis.opsForValue().setIfAbsent(key, "1", properties.getDedup().getTtl());
        return Boolean.TRUE.equals(set);
    }
}