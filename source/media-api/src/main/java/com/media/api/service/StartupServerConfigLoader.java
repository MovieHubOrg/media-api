package com.media.api.service;

import com.media.api.component.ServerConfigHolder;
import com.media.api.dto.ApiMessageDto;
import com.media.api.dto.ServerConfigDto;
import com.media.api.service.feign.FeignServerConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StartupServerConfigLoader {
    @Autowired
    private FeignServerConfigService feignServerConfigService;

    @Value("${server.number}")
    private Integer serverNumber;

    @Value("${movie.internal.api.key}")
    private String movieInternalApiKey;

    public StartupServerConfigLoader(FeignServerConfigService feignServerConfigService) {
        this.feignServerConfigService = feignServerConfigService;
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 5000, multiplier = 2.0)
    )
    public void loadServerConfig() {
        try {
            log.warn("Loading ServerConfig for serverNumber={} ...", serverNumber);

            ApiMessageDto<ServerConfigDto> response = feignServerConfigService.getByServerNumber(serverNumber, movieInternalApiKey);

            if (response != null && Boolean.TRUE.equals(response.getResult()) && response.getData() != null) {
                ServerConfigHolder.setServerConfig(response.getData());
                log.warn("Loaded ServerConfig: serverNumber={}, serverName={}",
                        response.getData().getServerNumber(),
                        response.getData().getName());
                return;
            }

            throw new RuntimeException("Failed to load ServerConfig: response is null or unsuccessful");
        } catch (Exception e) {
            log.warn("Load ServerConfig failed, will retry. serverNumber={}, error={}", serverNumber, e.getMessage());
            throw e;
        }
    }

    @Recover
    public void recover(Exception e) {
        log.error("Application startup failed after retries: {}", e.getMessage(), e);
        throw new RuntimeException("Application startup failed: Unable to load ServerConfig after retries", e);
    }
}
