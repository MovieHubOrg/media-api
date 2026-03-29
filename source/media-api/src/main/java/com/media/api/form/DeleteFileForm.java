package com.media.api.form;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class DeleteFileForm {
    @NotBlank(message = "filePath cannot be null")
    private String filePath;
}
