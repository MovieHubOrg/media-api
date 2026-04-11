package com.media.api;

import com.media.api.component.AuditorAwareImpl;
import com.media.api.component.ServerConfigHolder;
import com.media.api.dto.ApiMessageDto;
import com.media.api.dto.ServerConfigDto;
import com.media.api.service.StartupServerConfigLoader;
import com.media.api.service.feign.FeignServerConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableFeignClients
public class MediaServiceApplication extends BaseApplication implements ApplicationRunner {
    @Autowired
    private StartupServerConfigLoader startupServerConfigLoader;

    @Bean
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.out.println("Spring boot application running in UTC timezone :" + new Date());
    }

    @Override
    public void run(ApplicationArguments args) {
        startupServerConfigLoader.loadServerConfig();
    }

    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }
}
