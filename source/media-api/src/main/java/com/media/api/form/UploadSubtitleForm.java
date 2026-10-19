package com.media.api.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class UploadSubtitleForm {
    @NotNull(message = "videoId is required")
    @ApiModelProperty(name = "videoId", required = true)
    private Long videoId;

    @NotNull(message = "file is required")
    @ApiModelProperty(name = "file", required = true)
    private MultipartFile file;
}
