package com.media.api.service.minio;

import com.media.api.dto.ApiMessageDto;
import com.media.api.dto.UploadFileDto;
import com.media.api.form.UploadFileForm;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@Slf4j
public class MinioService {
    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public ApiMessageDto<UploadFileDto> storeVideo(UploadFileForm uploadFileForm) {
        ApiMessageDto<UploadFileDto> apiMessageDto = new ApiMessageDto<>();
        try {
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(uploadFileForm.getFile().getOriginalFilename()));
            String ext = FilenameUtils.getExtension(fileName);
            String finalFile = "VIDEO_" + RandomStringUtils.randomAlphanumeric(10) + "." + ext;

            String objectPath = "tmp/" + finalFile;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectPath)
                            .stream(
                                    uploadFileForm.getFile().getInputStream(),
                                    uploadFileForm.getFile().getSize(),
                                    -1
                            )
                            .contentType(uploadFileForm.getFile().getContentType())
                            .build()
            );

            log.info("Stored video to MinIO: {}/{}", bucket, objectPath);

            UploadFileDto uploadFileDto = new UploadFileDto();
            uploadFileDto.setFilePath("/" + bucket + "/" + objectPath);
            // result: "/moviehub/tmp/VIDEO_Xk2pQa1bNm.mp4"

            apiMessageDto.setData(uploadFileDto);
            apiMessageDto.setMessage("Upload video successfully");

        } catch (Exception e) {
            log.error("Error saving video to MinIO", e);
            apiMessageDto.setResult(false);
            apiMessageDto.setMessage("Upload failed: " + e.getMessage());
        }

        return apiMessageDto;
    }

    /**
     * Upload locally generated audio.wav to the minio.
     * Object key: {@code subtitles/{videoId}/audio.wav}; returns {@code bucket/subtitles/...}.
     */
    public String uploadGeneratedAudioForSubtitle(Long videoId, Path localWavPath) {
        try {
            String objectPath = "subtitles/" + videoId + "/audio.wav";
            uploadToMinio(objectPath, localWavPath, "audio/wav");
            log.info("Uploaded generated audio to MinIO: {}/{}", bucket, objectPath);
            return bucket + "/" + objectPath;
        } catch (Exception e) {
            log.error("Error uploading generated audio to MinIO", e);
            throw new RuntimeException("Upload generated audio failed: " + e.getMessage(), e);
        }
    }

    private void uploadToMinio(String objectPath, Path localFilePath, String contentType) throws Exception {
        long fileSize = Files.size(localFilePath);
        try (InputStream inputStream = Files.newInputStream(localFilePath)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectPath)
                            .stream(inputStream, fileSize, -1)
                            .contentType(contentType)
                            .build()
            );
        }
    }

    public InputStream downloadFile(String bucket, String folder, String fileName) throws Exception {
        String objectPath = folder + "/" + fileName;
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectPath)
                        .build()
        );
    }

    /**
     * If {@code fileUrl} is already {@code {bucket}/...}, returns it trimmed (leading {@code /} stripped).
     * Otherwise prepends the configured {@link #bucket} so paths like {@code subtitles/{id}/en.vtt} work.
     */
    public String normalizeObjectFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return fileUrl;
        }
        String n = fileUrl.trim();
        if (n.startsWith("/")) {
            n = n.substring(1);
        }
        if (n.startsWith(bucket + "/")) {
            return n;
        }
        return bucket + "/" + n;
    }

    /**
     * Download an object using a URL path of the form {@code bucket/object/key}, e.g. {@code moviehub/subtitles/9477444028170241/en.vtt}.
     */
    public void downloadObjectByFileUrl(String fileUrl, File destinationFile) throws Exception {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("fileUrl is required");
        }
        String normalized = fileUrl.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int slash = normalized.indexOf('/');
        if (slash <= 0 || slash >= normalized.length() - 1) {
            throw new IllegalArgumentException("Invalid MinIO fileUrl (expected bucket/object): " + fileUrl);
        }
        String bucketName = normalized.substring(0, slash);
        String objectKey = normalized.substring(slash + 1);

        Files.createDirectories(destinationFile.getParentFile().toPath());

        try (InputStream in = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build())) {
            Files.copy(in, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Downloaded MinIO object {}/{} to {}", bucketName, objectKey, destinationFile.getAbsolutePath());
    }

    /**
     * Deletes an object using a URL path of the same form as {@link #downloadObjectByFileUrl},
     * e.g. {@code moviehub/subtitles/9477444028170241/en.vtt}.
     * Logs and swallows errors so callers can use this for best-effort cleanup.
     */
    public void deleteObjectByFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            log.warn("deleteObjectByFileUrl: skip, fileUrl is blank");
            return;
        }
        String normalized = fileUrl.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int slash = normalized.indexOf('/');
        if (slash <= 0 || slash >= normalized.length() - 1) {
            log.warn("deleteObjectByFileUrl: invalid fileUrl (expected bucket/object): {}", fileUrl);
            return;
        }
        String bucketName = normalized.substring(0, slash);
        String objectKey = normalized.substring(slash + 1);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            log.info("Deleted MinIO object {}/{}", bucketName, objectKey);
        } catch (Exception e) {
            log.error("Failed to delete MinIO object {}/{}: {}", bucketName, objectKey, e.getMessage(), e);
        }
    }

    /**
     * Deletes generated subtitle audio at {@code subtitles/{videoId}/audio.wav} in the configured bucket.
     */
    public void deleteGeneratedAudioForSubtitle(Long videoId) {
        if (videoId == null) {
            log.warn("deleteGeneratedAudioForSubtitle: skip, videoId is null");
            return;
        }
        deleteObjectByFileUrl(bucket + "/subtitles/" + videoId + "/audio.wav");
    }

    public void downloadWithRetry(String bucket, String folder, String fileName, File dest) throws Exception {
        int maxAttempts = 5;
        int attempt = 0;

        // Lấy file size từ MinIO trước
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucket)
                        .object(folder + "/" + fileName)
                        .build()
        );
        long totalSize = stat.size();
        log.info("MinIO file size: {} bytes", totalSize);

        // Nếu file local đã đủ size → skip download
        if (dest.exists() && dest.length() == totalSize) {
            log.info("File already downloaded, skipping: {}", dest.getAbsolutePath());
            return;
        }

        while (attempt < maxAttempts) {
            attempt++;
            long existingSize = dest.exists() ? dest.length() : 0;

            // Nếu existingSize >= totalSize thì không cần download nữa
            if (existingSize >= totalSize) {
                log.info("File already complete at attempt {}", attempt);
                return;
            }

            log.info("Download attempt {}/{}, resume from byte {}/{}",
                    attempt, maxAttempts, existingSize, totalSize);

            try {
                GetObjectArgs args = GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(folder + "/" + fileName)
                        .offset(existingSize)
                        .length(totalSize - existingSize) // ← thêm length
                        .build();

                try (InputStream is = minioClient.getObject(args);
                     OutputStream os = new FileOutputStream(dest, true)) {
                    byte[] buffer = new byte[8 * 1024 * 1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }

                log.warn("Download completed: {}", dest.getAbsolutePath());
                return;

            } catch (Exception e) {
                log.warn("Download attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
                if (attempt >= maxAttempts) {
                    throw new RuntimeException("Download failed after " + maxAttempts + " attempts", e);
                }
                Thread.sleep(5000L * attempt);
            }
        }
    }
}
