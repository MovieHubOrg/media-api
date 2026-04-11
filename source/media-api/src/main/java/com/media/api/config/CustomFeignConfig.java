package com.media.api.config;


import com.media.api.config.component.OAuth2FeignRequestInterceptor;
import feign.Contract;
import feign.Feign;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2ClientContext;

@Configuration
@ConditionalOnClass({Feign.class})
@Slf4j
public class CustomFeignConfig {

    @Bean
    @ConditionalOnBean(OAuth2ClientContext.class)
    public RequestInterceptor oauth2FeignRequestInterceptor() {
        return new OAuth2FeignRequestInterceptor();
    }

    @Bean
    public Contract feignContract() {
        return new SpringMvcContract();
    }

//    @Bean
//    public Retryer retryer(){
//        return new CustomRetryer(TimeUnit.MINUTES.toMillis(5L),2000);
//    }

}
