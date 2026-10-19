package com.media.api.form;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoneProcessSubtitleForm {
    private Long videoId;
    private Integer state;
    private String reason;
    private String language;
    private String fileUrl;
}
