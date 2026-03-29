package com.media.api.form.rabbit;

import lombok.Data;

@Data
public class BaseSendMsgForm<T> {
    private String cmd;
    private String subCmd;
    private String app;
    private T data;
    private String responseCode;
    private String token;
    private String tenantId;
}
