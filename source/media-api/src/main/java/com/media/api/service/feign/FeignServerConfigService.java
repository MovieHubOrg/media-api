package com.media.api.service.feign;

import com.media.api.config.CustomFeignConfig;
import com.media.api.dto.ApiMessageDto;
import com.media.api.dto.ServerConfigDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "server-config-svr", url = "${movie.internal.base.url}", configuration = CustomFeignConfig.class)
public interface FeignServerConfigService {
    @GetMapping(value = "/v1/server-config/internal/get-by-server-number/{serverNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiMessageDto<ServerConfigDto> getByServerNumber(
            @PathVariable("serverNumber") Integer serverNumber,
            @RequestHeader(FeignConstant.HEADER_X_API_KEY) String apiKey);
}
