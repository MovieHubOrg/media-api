package com.media.api.service.rabbit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.api.constant.BaseConstant;
import com.media.api.form.ConvertVideoForm;
import com.media.api.form.DeleteFolderForm;
import com.media.api.form.DeleteListFileForm;
import com.media.api.form.UpdateVideoForm;
import com.media.api.form.rabbit.BaseSendMsgForm;
import com.media.api.service.BaseApiService;
import com.rabbitmq.client.Channel;
import io.netty.util.concurrent.CompleteFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class RabbitMQListener {
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${rabbitmq.app}")
    private String appName;

    @Value("${rabbitmq.media.queue}")
    private String mediaQueue;

    @Value("${rabbitmq.convert.video.queue}")
    private String convertVideoQueue;

    @Value("${rabbitmq.update.video.queue}")
    private String updateVideoQueue;

    @Autowired
    private BaseApiService baseApiService;

    @Autowired
    private RabbitService rabbitService;

    @Value("${file.upload-dir}")
    private String rootDir;

    @Autowired
    @Qualifier("convertExecutor")
    private Executor convertExecutor;

    @RabbitListener(queues = "${rabbitmq.media.queue}")
    public void receiveMessage(String message) {
        try {
            System.out.println("======> Received message from " + mediaQueue + ": " + message);
            BaseSendMsgForm<DeleteFolderForm> baseMessageForm = objectMapper.readValue(message, new TypeReference<>() {
            });
            String cmd = baseMessageForm.getCmd();
            DeleteFolderForm data = baseMessageForm.getData();
            List<Path> folderPaths = new ArrayList<>();
            switch (cmd) {
                case BaseConstant.CMD_DELETE_TENANT:
                    // delete /uploads/tenant/{businessId}
                    folderPaths.add(Paths.get(rootDir, "tenant", data.getId().toString()));

                    // delete /uploads/LIBRARY/{tenantId}
                    folderPaths.add(Paths.get(rootDir, "LIBRARY", baseMessageForm.getTenantId()));
                    break;
                case BaseConstant.CMD_DELETE_VIDEO:
                    // delete /uploads/LIBRARY/{videoId}
                    folderPaths.add(Paths.get(rootDir, "LIBRARY", data.getId().toString()));
                    break;
                default:
                    log.warn("Unknown or invalid command: {}", cmd);
                    break;
            }

            for (Path path : folderPaths) {
                if (path != null && path.normalize().startsWith(Paths.get(rootDir).normalize())) {
                    File folder = path.toFile();
                    if (folder.exists()) {
                        FileUtils.deleteDirectory(folder);
                        log.info("Deleted folder: {}", path);
                    } else {
                        log.warn("Folder not found: {}", path);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.convert.video.queue}", containerFactory = "convertQueueFactory")
    public void receiveMessageFromConvertQueue(Message amqpMessage, Channel channel) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        try {
            String message = new String(amqpMessage.getBody(), StandardCharsets.UTF_8);
            BaseSendMsgForm<ConvertVideoForm> baseMessageForm = objectMapper.readValue(message, new TypeReference<>() {
            });
            System.out.println("======> Received message from " + convertVideoQueue + ": " + message);

            if (!BaseConstant.CMD_CONVERT_VIDEO.equals(baseMessageForm.getCmd())) {
                log.warn("Invalid message or missing tenantId");
                channel.basicAck(deliveryTag, false); // ACK để không retry
                return;
            }

            Long videoId = baseMessageForm.getData().getId();
            String videoPath = baseMessageForm.getData().getContent();

            CompletableFuture
                    .runAsync(() -> convert(videoId, videoPath), convertExecutor)
                    .whenComplete((ok, ex) -> {
                        try {
                            if (ex == null) {
                                channel.basicAck(deliveryTag, false);
                            } else {
                                log.error("Error in long task", ex);
                                channel.basicNack(deliveryTag, false, true);
                            }
                        } catch (IOException e) {
                            log.error("Error when sending nack", ex);
                        }

                    });
        } catch (Exception e) {
            log.error("Error parsing or scheduling message", e);
            try {
                channel.basicNack(deliveryTag, false, true); // Retry message
            } catch (IOException ex) {
                log.error("Error when sending nack", ex);
            }
        }
    }

    private void convert(Long videoId, String videoPath) {
        try {
            log.warn("Start converting video ID: {}", videoId);
            UpdateVideoForm result = baseApiService.convertToHLS(videoId, videoPath);
            rabbitService.handleSendMsg(
                    appName,
                    updateVideoQueue,
                    result,
                    BaseConstant.CMD_DONE_CONVERT_VIDEO,
                    null,
                    null,
                    null,
                    null
            );
            log.warn("End converting video ID: {}", videoId);
        } catch (Exception e) {
            log.error("Error converting video ID: {}", videoId, e);
        }
    }
}
