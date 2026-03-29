package com.media.api.service;

import com.media.api.constant.BaseConstant;
import com.media.api.dto.ApiMessageDto;
import com.media.api.dto.UploadFileDto;
import com.media.api.form.UpdateVideoForm;
import com.media.api.form.UploadBase64Form;
import com.media.api.form.UploadFileForm;
import com.media.api.service.minio.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;

@Service
@Slf4j
public class BaseApiService {
    protected static final String[] UPLOAD_TYPES = new String[]{"LOGO", "AVATAR", "IMAGE", "VIDEO", "SYSTEM"};

    @Value("${file.upload-dir}")
    private String rootDirectory;

    @Autowired
    private FFprobeService ffprobeService;

    @Autowired
    private FfmpegService ffmpegService;

    @Autowired
    private MinioService minioService;

    /**
     * return file path
     * - General/
     * Video, Media,...
     * - Tenant/
     * - TenantId
     * Video, Media...
     *
     * @param uploadFileForm
     * @return
     */
    public ApiMessageDto<UploadFileDto> storeFile(UploadFileForm uploadFileForm, Long tenantId) {
        // Normalize file name
        ApiMessageDto<UploadFileDto> apiMessageDto = new ApiMessageDto<>();
        try {
            boolean contains = Arrays.stream(UPLOAD_TYPES).anyMatch(uploadFileForm.getType()::equalsIgnoreCase);
            if (!contains) {
                apiMessageDto.setResult(false);
                apiMessageDto.setMessage("Type is required in AVATAR or LOGO or SYSTEM");
                return apiMessageDto;
            }
            String fileName = StringUtils.cleanPath(uploadFileForm.getFile().getOriginalFilename());
            String ext = FilenameUtils.getExtension(fileName);
            //upload to uploadFolder/TYPE/id
            String finalFile = uploadFileForm.getType() + "_" + RandomStringUtils.randomAlphanumeric(10) + "." + ext;
            String typeFolder = File.separator + uploadFileForm.getType();
            Path fileStorageLocation;
            String tenantFolder = "";
            if (tenantId != null) {
                //upload to uploadFolder/tenantFolder/TYPE/id
                tenantFolder = File.separator + tenantId;
                fileStorageLocation = Paths.get(rootDirectory + BaseConstant.DIRECTORY_TENANT + tenantFolder + typeFolder).toAbsolutePath().normalize();
            } else {
                fileStorageLocation = Paths.get(rootDirectory + BaseConstant.DIRECTORY_GENERAL + typeFolder).toAbsolutePath().normalize();
            }
            Files.createDirectories(fileStorageLocation);
            Path targetLocation = fileStorageLocation.resolve(finalFile);
            log.info("Stored target: " + targetLocation.toString());
            Files.copy(uploadFileForm.getFile().getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            UploadFileDto uploadFileDto = new UploadFileDto();
            uploadFileDto.setFilePath(tenantFolder + typeFolder + File.separator + finalFile);
            apiMessageDto.setData(uploadFileDto);
            apiMessageDto.setMessage("Upload file success");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            apiMessageDto.setResult(false);
            apiMessageDto.setMessage(e.getMessage());
        }
        return apiMessageDto;
    }

    public ApiMessageDto<UploadFileDto> storeFileByBase64(UploadBase64Form uploadBase64Form, Long tenantId) {
        // Normalize file name
        ApiMessageDto<UploadFileDto> apiMessageDto = new ApiMessageDto<>();
        try {
            boolean contains = Arrays.stream(UPLOAD_TYPES).anyMatch(uploadBase64Form.getType()::equalsIgnoreCase);
            if (!contains) {
                apiMessageDto.setResult(false);
                apiMessageDto.setMessage("Type is required in AVATAR or LOGO");
                return apiMessageDto;
            }
            String ext = "png";
            String finalFile = (uploadBase64Form.getApp() != null ? uploadBase64Form.getApp() + "_" : "") + uploadBase64Form.getType() + "_" + RandomStringUtils.randomAlphanumeric(10) + "." + ext;
            //upload to uploadFolder/TYPE/id
            String typeFolder = File.separator + uploadBase64Form.getType();
            Path fileStorageLocation;
            String tenantFolder = "";
            if (tenantId != null) {
                //upload to uploadFolder/restaurantFolder/TYPE/id
                tenantFolder = File.separator + tenantId;
                fileStorageLocation = Paths.get(rootDirectory + BaseConstant.DIRECTORY_TENANT + tenantFolder + typeFolder).toAbsolutePath().normalize();
            } else {
                fileStorageLocation = Paths.get(rootDirectory + BaseConstant.DIRECTORY_GENERAL + typeFolder).toAbsolutePath().normalize();
            }
            Files.createDirectories(fileStorageLocation);
            convertBase64ToImage(uploadBase64Form.getBase64Image(), fileStorageLocation.toString() + File.separator + finalFile);
            UploadFileDto uploadFileDto = new UploadFileDto();
            uploadFileDto.setFilePath(tenantFolder + typeFolder + File.separator + finalFile);
            apiMessageDto.setData(uploadFileDto);
            apiMessageDto.setMessage("Upload file success");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            apiMessageDto.setResult(false);
            apiMessageDto.setMessage("" + e.getMessage());
        }
        return apiMessageDto;
    }

    public void convertBase64ToImage(String base64String, String outputPath) throws IOException {
        try {
            base64String = base64String.replace("data:image/png;base64,", "").replace("data:image/jpeg;base64,", "").replace("data:image/webp;base64,", "").trim();
            // Decode the Base64 string to bytes
            byte[] imageBytes = Base64.getDecoder().decode(base64String);

            // Create a FileOutputStream to write the image to a file
            try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
                FileCopyUtils.copy(imageBytes, outputStream);
                //outputStream.write(imageBytes);
            }
        } catch (IOException e) {
            throw new IOException("Error converting Base64 to image: " + e.getMessage());
        }
    }

    public String changePathWithTenant(String path, Long tenantId) throws IOException {
        if (org.apache.commons.lang3.StringUtils.isBlank(path) || path.contains(File.separator + tenantId + File.separator)) {
            return null;
        }
        String pathFolder = path.substring(0, path.lastIndexOf(File.separator));
        String newPath = File.separator + tenantId + pathFolder;
        String fileName = path.substring(path.lastIndexOf(File.separator) + 1);
        Path targetLocation = Paths.get(rootDirectory + BaseConstant.DIRECTORY_TENANT + newPath).toAbsolutePath().normalize().resolve(fileName);
        Path sourceLocation = Paths.get(rootDirectory + BaseConstant.DIRECTORY_TENANT + pathFolder).toAbsolutePath().normalize().resolve(fileName);
        Files.createDirectories(targetLocation);
        Resource resource = loadFileAsResource(path.substring(1, path.lastIndexOf(File.separator)), sourceLocation.getFileName().toString(), true, false);
        if (resource != null) {
            Files.move(sourceLocation, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return newPath + File.separator + fileName;
        }
        return null;
    }

    public void deleteFile(String filePath, String tenantId) {
        File file;
        if (tenantId != null) {
            file = new File(rootDirectory + BaseConstant.DIRECTORY_TENANT + filePath);
        } else {
            file = new File(rootDirectory + BaseConstant.DIRECTORY_GENERAL + filePath);
        }

        System.out.println("======> file path: " + file.getAbsolutePath());
        if (file.exists()) {
            file.delete();
        }
    }

    public Resource loadFileAsResource(String folder, String fileName, boolean tenantLocation, boolean isLibrary) {
        String directory = rootDirectory + BaseConstant.DIRECTORY_GENERAL;
        if (tenantLocation) {
            directory = rootDirectory + BaseConstant.DIRECTORY_TENANT;
        }
        if (isLibrary) {
            directory = rootDirectory;
        }
        System.out.println("User.home: " + System.getProperty("spring.config.location"));
        System.out.println("get file: " + folder + "/" + fileName + ", path: " + directory);
        try {
            Path fileStorageLocation = Paths.get(directory + File.separator + folder).toAbsolutePath().normalize();
            Path fP = fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(fP.toUri());
            if (resource.exists()) {
                return resource;
            }
        } catch (MalformedURLException ex) {
            //log.error(ex.getMessage(), ex);
            System.out.println("Error get file: " + folder + "/" + fileName + ", path: " + directory);

        }
        return null;
    }

    public UpdateVideoForm convertToHLS(Long videoId, String videoPath) {
        UpdateVideoForm data = new UpdateVideoForm();
        data.setId(videoId);
        data.setContent(videoPath);
        data.setRelativeContentPath(videoPath);
        data.setState(BaseConstant.VIDEO_LIBRARY_STATE_ERROR);

        File tempInputFile = null;

        try {
            // ============ PHASE 1: PARSE & VALIDATE MINIO PATH ============
            // videoPath format: "/moviehub/tmp/VIDEO_abc.mp4"
            String stripped = videoPath.startsWith("/") ? videoPath.substring(1) : videoPath;
            String[] parts = stripped.split("/", 3);
            if (parts.length != 3) {
                log.error("Invalid videoPath format: {}", videoPath);
                return data;
            }

            String minioBucket = parts[0]; // "moviehub"
            String minioFolder = parts[1]; // "tmp"
            String minioFileName = parts[2]; // "VIDEO_abc.mp4"

            if (!"mp4".equalsIgnoreCase(FilenameUtils.getExtension(minioFileName))) {
                log.error("File is not mp4: {}", minioFileName);
                return data;
            }

            // ============ PHASE 2: DOWNLOAD FROM MINIO TO LOCAL TEMP ============
            Path tmpDir = Paths.get(rootDirectory, "LIBRARY", "tmp").toAbsolutePath().normalize();
            Files.createDirectories(tmpDir);
            tempInputFile = tmpDir.resolve(minioFileName).toFile();

            try (InputStream inputStream = minioService.downloadFile(minioBucket, minioFolder, minioFileName)) {
                Files.copy(inputStream, tempInputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Downloaded temp file from MinIO: {}", tempInputFile.getAbsolutePath());

            String inputPath = tempInputFile.getAbsolutePath();

            // ============ PHASE 3: ANALYZE VIDEO ============
            int bitrate = ffprobeService.getBitrate(inputPath);
            Dimension resolution = ffprobeService.getResolution(inputPath);
            boolean hasAudio = ffprobeService.hasAudio(inputPath);
            long duration = ffprobeService.getDuration(inputPath);
            log.info("Video info — bitrate: {}, resolution: {}, hasAudio: {}, duration: {}s",
                    bitrate, resolution, hasAudio, duration);

            // ============ PHASE 4: PREPARE HLS OUTPUT ============
            String libraryFolder = "/LIBRARY/" + videoId;
            Path outputFolder = Paths.get(rootDirectory + libraryFolder).toAbsolutePath().normalize();
            Files.createDirectories(outputFolder);

            Map<String, Integer> bitrateMap = Map.of(
                    "720", Math.min(bitrate, 5_000_000),
                    "1080", Math.min(bitrate, 8_000_000),
                    "1440", Math.min(bitrate, 16_000_000),
                    "original", bitrate
            );

            FfmpegService.QualityTarget qualityTarget;
            if (resolution.height > 1440) qualityTarget = FfmpegService.QualityTarget.ORIGINAL;
            else if (resolution.height > 1080) qualityTarget = FfmpegService.QualityTarget.P1440;
            else if (resolution.height > 720) qualityTarget = FfmpegService.QualityTarget.P1080;
            else qualityTarget = FfmpegService.QualityTarget.P720;

            String outputSegmentPath = outputFolder + "/%v/fileSequence%d.ts";
            String outputPlaylistPath = outputFolder + "/%v/prog_index.m3u8";
            String masterPlaylistPath = outputFolder + "/master.m3u8";

            // ============ PHASE 5: RUN FFMPEG ============
            List<String> command = ffmpegService.generateFfmpegCommand(
                    inputPath, outputPlaylistPath, outputSegmentPath,
                    bitrateMap, resolution.width, resolution.height,
                    hasAudio, qualityTarget, masterPlaylistPath
            );
            log.info("FFmpeg command: {}", command);

            Process process = new ProcessBuilder(command).start();

            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                boolean hasError = false;
                String line;
                while ((line = errorReader.readLine()) != null) {
                    log.warn("[FFmpeg] {}", line);
                    String lower = line.toLowerCase();
                    if (lower.contains("error") || lower.contains("failed") || lower.contains("invalid")) {
                        hasError = true;
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0 || hasError) {
                    log.error("FFmpeg failed. Exit code: {}", exitCode);
                    return data;
                }
            }
            log.info("FFmpeg finished successfully.");

            // ============ PHASE 6: GENERATE THUMBNAILS, SPRITE & VTT ============
            Path thumbnailFolder = outputFolder.resolve("thumbnails");
            Files.createDirectories(thumbnailFolder);

            ffmpegService.generateThumbnail(inputPath, thumbnailFolder.resolve("thumb_%06d.jpg").toString());
            ffmpegService.generateSprite(outputFolder);
            ffmpegService.generateVttFile(thumbnailFolder.toFile(), outputFolder.resolve("thumbnails.vtt").toFile());

            log.info("Cleaning up thumbnail images...");
            Files.walk(thumbnailFolder)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            // ============ PHASE 7: RETURN RESULT ============
            data.setRelativeContentPath(libraryFolder + "/master.m3u8");
            data.setContent(libraryFolder + "/master.m3u8");
            data.setDuration(duration);
            data.setSpriteUrl(libraryFolder + "/sprite.jpg");
            data.setVttUrl(libraryFolder + "/thumbnails.vtt");
            data.setState(BaseConstant.VIDEO_LIBRARY_STATE_READY);

        } catch (IOException e) {
            log.error("[convertToHLS] IO error", e);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.error("[convertToHLS] Interrupted", e);
        } finally {
            if (tempInputFile != null && tempInputFile.exists()) {
                boolean deleted = tempInputFile.delete();
                if (deleted) log.info("Deleted temp file: {}", tempInputFile.getAbsolutePath());
                else log.warn("Cannot delete temp file: {}", tempInputFile.getAbsolutePath());
            }
        }

        return data;
    }
}
