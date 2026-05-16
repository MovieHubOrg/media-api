package com.media.api.service;

import com.media.api.component.ServerConfigHolder;
import com.media.api.constant.BaseConstant;
import com.media.api.dto.ServerConfigDto;
import com.media.api.exception.BadRequestException;
import com.media.api.form.*;
import com.media.api.service.minio.MinioService;
import com.media.api.service.rabbit.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static com.media.api.constant.BaseConstant.*;

@Service
@Slf4j
public class VideoService {
    @Value("${rabbitmq.app}")
    private String appName;

    @Value("${rabbitmq.update.video.queue}")
    private String updateVideoQueue;

    @Value("${rabbitmq.subtitle.requests}")
    private String subtitleRequestsQueue;

    @Value("${server.number}")
    private Integer serverNumber;

    @Value("${file.upload-dir}")
    private String rootDir;

    @Autowired
    private BaseApiService baseApiService;

    @Autowired
    private MinioService minioService;

    @Autowired
    private RabbitService rabbitService;

    private ServerConfigDto validateServerActive() {
        ServerConfigDto serverConfig = ServerConfigHolder.getServerConfig();
        if (serverConfig == null || !BaseConstant.STATUS_ACTIVE.equals(serverConfig.getStatus())) {
            throw new BadRequestException("Server status is not active");
        }
        return serverConfig;
    }

    public void processConvertMessage(ConvertVideoForm convertVideoForm) {
        ServerConfigDto serverConfig = validateServerActive();
        Long videoId = convertVideoForm.getId();
        String videoPath = convertVideoForm.getContent();

        log.info("Start converting video ID: {}", videoId);
        UpdateVideoForm result = baseApiService.convertToHLS(videoId, videoPath);
        result.setServerNumber(serverConfig.getServerNumber());
        rabbitService.handleSendMsg(appName, updateVideoQueue, result, BaseConstant.CMD_DONE_CONVERT_VIDEO);
        log.info("End converting video ID: {}", videoId);
    }

    public void processConvertAudioMessage(ConvertAudioForm convertAudioForm) {
        validateServerActive();
        Long videoId = convertAudioForm.getVideoId();

        log.info("Start converting m3u8 to audio for video ID: {}", videoId);
        UpdateAudioForm result = baseApiService.convertM3u8ToAudio(videoId);
        rabbitService.handleSendMsg(appName, updateVideoQueue, result, CMD_DONE_CONVERT_AUDIO);
        log.info("End converting m3u8 to audio for video ID: {}, audioUrl: {}", videoId, result.getAudioUrl());

        Path audioPath = getAudioFileByVideoId(videoId);
        String fileUrl = minioService.uploadGeneratedAudioForSubtitle(videoId, audioPath);
        SubTitleRequestForm subtitlePayload = new SubTitleRequestForm(videoId, fileUrl, serverNumber);
        rabbitService.handleSendMsg(appName, subtitleRequestsQueue, subtitlePayload, CMD_PROCESS_SUBTITLE);
        log.info("Sent subtitle request for videoId={}, fileUrl={}", videoId, fileUrl);
    }

    public void processDoneSubtitleMessage(DoneProcessSubtitleForm form) {
        Long videoId = form.getVideoId();
        if (Objects.equals(form.getState(), VIDEO_LIBRARY_STATE_ERROR)) {
            log.warn("CMD_DONE_PROCESS_SUBTITLE: convert error, videoId={}, fileUrl={}, language={}, reason={}",
                    videoId, form.getFileUrl(), form.getLanguage(), form.getReason());
            minioService.deleteGeneratedAudioForSubtitle(videoId);
            rabbitService.handleSendMsg(appName, updateVideoQueue, form, CMD_DONE_PROCESS_SUBTITLE);
            return;
        }

        String fileUrl = minioService.normalizeObjectFileUrl(form.getFileUrl().trim());
        Path libraryDir = Paths.get(rootDir, DIRECTORY_LIBRARY, videoId.toString()).toAbsolutePath().normalize();

        int lastSlash = fileUrl.lastIndexOf('/');
        String fileName = (lastSlash >= 0 && lastSlash < fileUrl.length() - 1)
                ? fileUrl.substring(lastSlash + 1)
                : fileUrl;
        if (fileName.isBlank()) {
            fileName = form.getLanguage().trim() + ".vtt";
        }

        Path destPath = libraryDir.resolve(fileName).normalize();
        if (!destPath.startsWith(libraryDir)) {
            throw new BadRequestException("Invalid subtitle file name: " + fileName);
        }

        log.info("CMD_DONE_PROCESS_SUBTITLE: downloading videoId={}, fileUrl={}, localPath={}", videoId, fileUrl, destPath);
        try {
            minioService.downloadObjectByFileUrl(fileUrl, destPath.toFile());
        } catch (Exception e) {
            log.error("CMD_DONE_PROCESS_SUBTITLE: MinIO download failed for videoId={}, fileUrl={}", videoId, fileUrl, e);
            form.setState(VIDEO_LIBRARY_STATE_ERROR);
            form.setReason(REASON_DOWNLOAD_VTT_FILE_FAILED);
            rabbitService.handleSendMsg(appName, updateVideoQueue, form, CMD_DONE_PROCESS_SUBTITLE);
            minioService.deleteGeneratedAudioForSubtitle(videoId);
            return;
        }

        form.setFileUrl(String.format("/%s/%s/%s", DIRECTORY_LIBRARY, videoId, destPath.getFileName()));
        rabbitService.handleSendMsg(appName, updateVideoQueue, form, CMD_DONE_PROCESS_SUBTITLE);
        log.info("CMD_DONE_PROCESS_SUBTITLE: sent update to updateVideoQueue for videoId={}", videoId);

        minioService.deleteObjectByFileUrl(fileUrl);
        minioService.deleteGeneratedAudioForSubtitle(videoId);
        log.info("CMD_DONE_PROCESS_SUBTITLE: requested MinIO cleanup for videoId={}, vtt={}", videoId, fileUrl);
    }

    public void processDeleteSubtitleMessage(DeleteSubtitleForm data) {
        if (data == null || data.getVideoId() == null || data.getFileUrl() == null || data.getFileUrl().isBlank()) {
            throw new BadRequestException("videoId and fileUrl are required to delete subtitle");
        }

        Long videoId = data.getVideoId();
        Path subtitlePath = Paths.get(rootDir, data.getFileUrl()).toAbsolutePath().normalize();
        Path libraryDir = Paths.get(rootDir, DIRECTORY_LIBRARY, videoId.toString()).toAbsolutePath().normalize();
        if (!subtitlePath.startsWith(libraryDir)) {
            throw new BadRequestException("Invalid fileUrl: path is outside the allowed directory");
        }

        try {
            if (Files.deleteIfExists(subtitlePath)) {
                log.info("CMD_DELETE_SUBTITLE: deleted subtitle, videoId={}, fileUrl={}, localPath={}",
                        videoId, data.getFileUrl(), subtitlePath);
            } else {
                log.warn("CMD_DELETE_SUBTITLE: subtitle not found, videoId={}, fileUrl={}, localPath={}",
                        videoId, data.getFileUrl(), subtitlePath);
            }
        } catch (Exception e) {
            log.error("CMD_DELETE_SUBTITLE: failed to delete subtitle, videoId={}, fileUrl={}, localPath={}",
                    videoId, data.getFileUrl(), subtitlePath, e);
            throw new BadRequestException("Delete subtitle failed: " + e.getMessage());
        }
    }

    private Path getAudioFileByVideoId(Long videoId) {
        Path rootPath = Paths.get(rootDir).toAbsolutePath().normalize();

        Path audioPath = rootPath
                .resolve(DIRECTORY_LIBRARY)
                .resolve(videoId.toString())
                .resolve(AUDIO_FILE_NAME)
                .normalize();

        if (!audioPath.startsWith(rootPath)) {
            throw new BadRequestException("Invalid audio path for videoId: " + videoId);
        }

        if (!Files.isRegularFile(audioPath)) {
            throw new BadRequestException("audio.wav not found after conversion for videoId: " + videoId);
        }
        return audioPath;
    }
}
