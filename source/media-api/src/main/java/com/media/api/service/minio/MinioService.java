package com.media.api.service.minio;

import com.media.api.dto.ApiMessageDto;
import com.media.api.dto.UploadFileDto;
import com.media.api.form.UploadFileForm;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
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
}
