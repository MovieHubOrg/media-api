package com.media.api.dto;

import lombok.Data;

@Data
public class ServerConfigDto {
    private Long id;
    private Integer serverNumber;
    private String name;
    private String hostname;
    private String ip;
    private Integer port;
    private Integer status;
}
