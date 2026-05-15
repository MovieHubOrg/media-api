package com.media.api.form;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAudioForm {
    private Long videoId;
    private String audioUrl;
    private Integer audioState;
    private String reason;
}
