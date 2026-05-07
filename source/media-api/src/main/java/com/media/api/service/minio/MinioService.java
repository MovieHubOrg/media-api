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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

    public InputStream downloadFile(String bucket, String folder, String fileName) throws Exception {
        String objectPath = folder + "/" + fileName;
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectPath)
                        .build()
        );
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
