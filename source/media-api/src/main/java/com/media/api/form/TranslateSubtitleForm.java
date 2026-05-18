package com.media.api.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranslateSubtitleForm {
    private Long videoId;
    private Long subtitleId;
    private String sourceLang;
    private String destLang;
    private String fileUrl;
}
