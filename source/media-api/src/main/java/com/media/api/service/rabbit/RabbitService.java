package com.media.api.service.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.api.form.rabbit.BaseSendMsgForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class RabbitService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RabbitSender rabbitSender;

    public <T> void handleSendMsg(String appName, String queueName, T data, String cmd, String subCmd, String responseCode, String token, String tenantId) {
        BaseSendMsgForm<T> form = new BaseSendMsgForm<>();
        form.setApp(appName);
        form.setCmd(cmd);
        form.setSubCmd(subCmd);
        form.setResponseCode(responseCode);
        form.setData(data);
        form.setToken(token);
        form.setTenantId(tenantId);
        String msg;
        try {
            msg = objectMapper.writeValueAsString(form);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // create queue if existed
        createQueueIfNotExist(queueName);

        // push msg
        rabbitSender.send(queueName, msg);
    }

    private void createQueueIfNotExist(String queueName) {
        rabbitSender.createQueueIfNotExist(queueName);
    }

}
