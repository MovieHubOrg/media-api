package com.media.api.controller;

import com.media.api.constant.BaseConstant;
import com.media.api.dto.ApiMessageDto;
import com.media.api.dto.UploadFileDto;
import com.media.api.form.DeleteFileForm;
import com.media.api.form.DeleteListFileForm;
import com.media.api.form.UploadBase64Form;
import com.media.api.form.UploadFileForm;
import com.media.api.jwt.BaseJwt;
import com.media.api.service.BaseApiService;
import com.media.api.service.impl.UserServiceImpl;
import com.media.api.service.minio.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/file")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class FileController extends ABasicController {
    @Autowired
    BaseApiService baseApiService;

    @Autowired
    UserServiceImpl userService;

    @Autowired
    private MinioService minioService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<UploadFileDto> upload(@Valid UploadFileForm uploadFileForm, BindingResult bindingResult) {
        BaseJwt jwt = getSessionFromToken();
        if (jwt == null || jwt.getUserKind() == null) {
            ApiMessageDto<UploadFileDto> result = new ApiMessageDto<>();
            result.setResult(false);
            result.setMessage("Not valid additional data");
            return result;
        }
        if (getSessionFromToken().getUserKind().equals(BaseConstant.USER_KIND_ADMIN) || getSessionFromToken().getUserKind().equals(BaseConstant.USER_KIND_CUSTOMER)) {
            return baseApiService.storeFile(uploadFileForm, null);
        }
        if (userService.tenantId.get() == null) {
            ApiMessageDto<UploadFileDto> result = new ApiMessageDto<>();
            result.setResult(false);
            result.setMessage("Not found tenant id");
            return result;
        }
        return baseApiService.storeFile(uploadFileForm, Long.valueOf(userService.tenantId.get()));
    }

    @PostMapping(value = "/upload-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<UploadFileDto> uploadVideo(@Valid UploadFileForm uploadFileForm, BindingResult bindingResult) {
        BaseJwt jwt = getSessionFromToken();
        if (jwt == null || jwt.getUserKind() == null) {
            ApiMessageDto<UploadFileDto> result = new ApiMessageDto<>();
            result.setResult(false);
            result.setMessage("Not valid additional data");
            return result;
        }
        return minioService.storeVideo(uploadFileForm);
    }

    @GetMapping("/download-video/{bucket}/{folder}/{fileName:.+}")
    public ResponseEntity<Resource> downloadVideo(
            @PathVariable String bucket,
            @PathVariable String folder,
            @PathVariable String fileName,
            HttpServletRequest request) {
        try {
            InputStream inputStream = minioService.downloadFile(bucket, folder, fileName);

            String contentType = request.getServletContext().getMimeType(fileName);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            InputStreamResource resource = new InputStreamResource(inputStream);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading file from MinIO: {}/{}/{}", bucket, folder, fileName, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/upload-base64", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<UploadFileDto> uploadForBase64(@Valid @RequestBody UploadBase64Form uploadBase64Form, BindingResult bindingResult) {
        BaseJwt jwt = getSessionFromToken();
        if (jwt == null || jwt.getUserKind() == null) {
            ApiMessageDto<UploadFileDto> result = new ApiMessageDto<>();
            result.setResult(false);
            result.setMessage("Not valid additional data");
            return result;
        }

        if (getSessionFromToken().getUserKind().equals(BaseConstant.USER_KIND_ADMIN) || getSessionFromToken().getUserKind().equals(BaseConstant.USER_KIND_CUSTOMER)) {
            return baseApiService.storeFileByBase64(uploadBase64Form, null);
        }
        if (userService.tenantId.get() == null) {
            ApiMessageDto<UploadFileDto> result = new ApiMessageDto<>();
            result.setResult(false);
            result.setMessage("Not found tenant id");
            return result;
        }
        return baseApiService.storeFileByBase64(uploadBase64Form, Long.valueOf(userService.tenantId.get()));
    }

    @GetMapping("/public-download/**")
    @Cacheable("images")
    public ResponseEntity<Resource> publicDownload(HttpServletRequest request) throws FileNotFoundException {
        String uri = request.getRequestURI();
        String basePath = "/public-download/";
        String path = uri.substring(uri.indexOf(basePath) + basePath.length());

        int lastSlash = path.lastIndexOf('/');
        String folder = path.substring(0, lastSlash);
        String fileName = path.substring(lastSlash + 1);

        return getResource(folder, fileName, request, false, true);
    }

    @GetMapping("/download/{folder}/{fileName:.+}")
    @Cacheable("images")
    public ResponseEntity<Resource> downloadFile(@PathVariable String folder, @PathVariable String fileName, HttpServletRequest request) throws FileNotFoundException {
        return getResource(folder, fileName, request, false, false);
    }

    // /LIBRARY/{videoId}/{fileName}
    @GetMapping("/download-video-resource/{folder}/{subFolder}/{fileName:.+}")
    public ResponseEntity<Resource> downloadVideoResource(
            @PathVariable String folder,
            @PathVariable String subFolder,
            @PathVariable String fileName,
            HttpServletRequest request) {
        // {folder}/{subFolder}
        String fullFolder = Paths.get(folder, subFolder).toString();
        return getResource(fullFolder, fileName, request, false, true);
    }

    // /LIBRARY/{videoId}/{resolution}/{fileName}
    @GetMapping("/download-video-resource/{folder}/{subFolder}/{subLevel}/{fileName:.+}")
    public ResponseEntity<Resource> downloadVideoResourceSubLevel(
            @PathVariable String folder,
            @PathVariable String subFolder,
            @PathVariable String subLevel,
            @PathVariable String fileName,
            HttpServletRequest request) {
        // {folder}/{subFolder}
        String fullFolder = Paths.get(folder, subFolder, subLevel).toString();
        return getResource(fullFolder, fileName, request, false, true);
    }

    private ResponseEntity<Resource> getResource(String folder, String fileName, HttpServletRequest request, boolean tenantLocation, boolean isLibrary) {
        Resource resource = baseApiService.loadFileAsResource(folder, fileName, tenantLocation, isLibrary);
        if (resource == null || !resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .or(() -> MediaTypeFactory.getMediaType(fileName))
                .orElseGet(() -> {
                    try {
                        String detected = Files.probeContentType(resource.getFile().toPath());
                        return detected != null
                                ? MediaType.parseMediaType(detected)
                                : MediaType.APPLICATION_OCTET_STREAM;
                    } catch (IOException e) {
                        return MediaType.APPLICATION_OCTET_STREAM;
                    }
                });
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(7776000, TimeUnit.SECONDS))
                .contentType(mediaType)
                //.header(HttpHeaders.EXPIRES, expires)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping(value = "/delete-file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<String> deleteFile(@Valid @RequestBody DeleteFileForm form, BindingResult bindingResult) {
        String tenantId = userService.tenantId.get();
        baseApiService.deleteFile(form.getFilePath(), tenantId);
        return makeSuccessResponse("Delete file success");
    }

    @PostMapping(value = "/delete-list-file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<String> deleteListFile(@Valid @RequestBody DeleteListFileForm deleteListFileForm, BindingResult bindingResult) {
        ApiMessageDto<String> apiMessageDto = new ApiMessageDto<>();
        String tenantId = userService.tenantId.get();
        if (StringUtils.isBlank(tenantId)) {
            for (String filePath : deleteListFileForm.getFiles()) {
                baseApiService.deleteFile(filePath, null);
            }
            apiMessageDto.setMessage("Delete list file success");
            return apiMessageDto;
        }
        for (String filePath : deleteListFileForm.getFiles()) {
            baseApiService.deleteFile(filePath, userService.tenantId.get());
        }
        apiMessageDto.setMessage("Delete list file success");
        return apiMessageDto;
    }

}
