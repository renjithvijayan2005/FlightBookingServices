package com.example.booking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictOnStartup implements ApplicationRunner {

    private final CacheManager cacheManager;

    @Override
    public void run(ApplicationArguments args) {
        var cache = cacheManager.getCache("airports");
        if (cache != null) {
            cache.clear();
            log.info("Cleared 'airports' cache on startup");
        }
    }
}
