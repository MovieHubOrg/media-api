package com.media.api.dto;

import lombok.Data;

@Data
public class UploadVideoDto {
    private String originalUrl;
    private String hlsUrl;
}
