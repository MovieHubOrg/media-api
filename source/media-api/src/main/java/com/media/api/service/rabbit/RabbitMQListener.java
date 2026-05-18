package com.media.api.service.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.api.component.ServerConfigHolder;
import com.media.api.constant.BaseConstant;
import com.media.api.dto.ServerConfigDto;
import com.media.api.form.ConvertAudioForm;
import com.media.api.form.ConvertVideoForm;
import com.media.api.form.DeleteFolderForm;
import com.media.api.form.DeleteSubtitleForm;
import com.media.api.form.DoneProcessSubtitleForm;
import com.media.api.form.DoneTranslateSubtitleForm;
import com.media.api.form.TranslateSubtitleForm;
import com.media.api.form.rabbit.BaseSendMsgForm;
import com.media.api.service.VideoService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RabbitMQListener {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VideoService videoService;

    @Value("${rabbitmq.streaming.server-queue}")
    private String streamingQueue;

    @Value("${rabbitmq.convert.video.queue}")
    private String convertVideoQueue;

    @Value("${rabbitmq.convert.video.server-queue}")
    private String convertVideoServerQueue;

    @Value("${file.upload-dir}")
    private String rootDir;

    @RabbitListener(queues = "${rabbitmq.streaming.server-queue}")
    public void receiveMessage(String message) {
        try {
            System.out.println("======> Received message from " + streamingQueue + ": " + message);
            BaseSendMsgForm<JsonNode> baseMessageForm = objectMapper.readValue(message, new TypeReference<>() {
            });
            String cmd = baseMessageForm.getCmd();
            switch (cmd) {
                case BaseConstant.CMD_DELETE_VIDEO:
                    List<Path> folderPaths = new ArrayList<>();
                    // delete /uploads/LIBRARY/{videoId}
                    DeleteFolderForm deleteFolderForm = objectMapper.treeToValue(baseMessageForm.getData(), DeleteFolderForm.class);
                    folderPaths.add(Paths.get(rootDir, "LIBRARY", deleteFolderForm.getId().toString()));
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
                    break;
                case BaseConstant.CMD_DELETE_SUBTITLE:
                    DeleteSubtitleForm deleteSubtitleForm = objectMapper.treeToValue(baseMessageForm.getData(), DeleteSubtitleForm.class);
                    videoService.processDeleteSubtitleMessage(deleteSubtitleForm);
                    break;
                case BaseConstant.CMD_UPDATE_SERVER_CONFIG:
                    ServerConfigDto serverConfigDto = objectMapper.treeToValue(baseMessageForm.getData(), ServerConfigDto.class);
                    ServerConfigHolder.setServerConfig(serverConfigDto);
                    break;
                default:
                    log.warn("Unknown or invalid command: {}", cmd);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(
            queues = {"${rabbitmq.convert.video.queue}", "${rabbitmq.convert.video.server-queue}"},
            containerFactory = "convertQueueFactory"
    )
    public void receiveMessageFromConvertQueues(Message amqpMessage, Channel channel) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String sourceQueue = amqpMessage.getMessageProperties().getConsumerQueue();
        try {
            String message = new String(amqpMessage.getBody(), StandardCharsets.UTF_8);
            BaseSendMsgForm<JsonNode> baseMessageForm = objectMapper.readValue(message, new TypeReference<>() {
            });
            log.warn("Received message from {}: {}", sourceQueue, message);
            if (convertVideoQueue.equals(sourceQueue)) {
                processConvertQueueCommand(baseMessageForm);
            } else if (convertVideoServerQueue.equals(sourceQueue)) {
                processServerConvertQueueCommand(baseMessageForm, sourceQueue);
            } else {
                log.warn("Unknown source queue '{}', ack and discard", sourceQueue);
            }
            channel.basicAck(deliveryTag, false);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            log.error("Invalid message in {}, nack without requeue", sourceQueue, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("Error sending nack", ex);
            }
        } catch (Exception e) {
            log.error("Error processing message from {}, nack with requeue", sourceQueue, e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("Error sending nack", ex);
            }
        }
    }

    private void processConvertQueueCommand(BaseSendMsgForm<JsonNode> baseMessageForm) throws JsonProcessingException {
        if (!BaseConstant.CMD_CONVERT_VIDEO.equals(baseMessageForm.getCmd())) {
            throw new IllegalArgumentException("Unsupported cmd in convert queue: " + baseMessageForm.getCmd());
        }

        ConvertVideoForm convertVideoForm = objectMapper.treeToValue(baseMessageForm.getData(), ConvertVideoForm.class);
        videoService.processConvertMessage(convertVideoForm);
    }

    private void processServerConvertQueueCommand(BaseSendMsgForm<JsonNode> baseMessageForm, String sourceQueue) throws JsonProcessingException {
        String cmd = baseMessageForm.getCmd();
        switch (cmd) {
            case BaseConstant.CMD_CONVERT_VIDEO:
                ConvertVideoForm convertVideoForm = objectMapper.treeToValue(baseMessageForm.getData(), ConvertVideoForm.class);
                videoService.processConvertMessage(convertVideoForm);
                break;
            case BaseConstant.CMD_CONVERT_AUDIO:
                ConvertAudioForm convertAudioForm = objectMapper.treeToValue(baseMessageForm.getData(), ConvertAudioForm.class);
                videoService.processConvertAudioMessage(convertAudioForm);
                break;
            case BaseConstant.CMD_DONE_PROCESS_SUBTITLE:
                DoneProcessSubtitleForm doneProcessSubtitleForm = objectMapper.treeToValue(baseMessageForm.getData(), DoneProcessSubtitleForm.class);
                videoService.processDoneSubtitleMessage(doneProcessSubtitleForm);
                break;
            case BaseConstant.CMD_TRANSLATE_SUBTITLE:
                TranslateSubtitleForm translateSubtitleData = objectMapper.treeToValue(baseMessageForm.getData(), TranslateSubtitleForm.class);
                videoService.processTranslateSubtitleMessage(translateSubtitleData);
                break;
            case BaseConstant.CMD_DONE_TRANSLATE_SUBTITLE:
                DoneTranslateSubtitleForm doneTranslateSubtitleForm = objectMapper.treeToValue(baseMessageForm.getData(), DoneTranslateSubtitleForm.class);
                videoService.processDoneTranslateSubtitleMessage(doneTranslateSubtitleForm);
                break;
            default:
                log.warn("Unknown cmd '{}' in {}, ack and discard", cmd, sourceQueue);
                break;
        }
    }
}
