package com.media.api.component;

import com.media.api.dto.ServerConfigDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class ServerConfigHolder {
    @Setter
    @Getter
    private static ServerConfigDto serverConfig;

}
