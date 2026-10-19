package com.media.api.form;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubTitleRequestForm {
    private Long videoId;
    private String fileUrl;
    private Integer serverNumber;
}
