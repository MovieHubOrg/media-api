package com.media.api.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoneTranslateSubtitleForm {
    private Long videoId;
    private Long subtitleId;
    private Integer state;
    private String reason;
    private String language;
    private String fileUrl;
}
