package com.media.api.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class VideoCleanScheduler {
    @Value("${file.upload-dir}")
    private String uploadBaseDir;

    private static final int MAX_DAYS = 1;

    @Scheduled(cron = "0 0 12 * * *")
    public void cleanOldVideoFolders() {
        String rootPath = Paths.get(uploadBaseDir, "LIBRARY", "tmp").toString();

        log.warn("Start cleaning old video folders in: {}", rootPath);

        File root = new File(rootPath);
        if (!root.exists() || !root.isDirectory()) {
            log.warn("Root folder does not exist or is not a directory: {}", rootPath);
            return;
        }

        File[] folders = root.listFiles(File::isDirectory);
        if (folders == null || folders.length == 0) {
            log.warn("No folders found in {}", rootPath);
            return;
        }

        LocalDate today = LocalDate.now();

        for (File folder : folders) {
            String folderName = folder.getName();
            try {
                LocalDate folderDate = LocalDate.parse(folderName, DateTimeFormatter.ofPattern("ddMMyyyy"));
                long daysBetween = ChronoUnit.DAYS.between(folderDate, today);

                if (daysBetween > MAX_DAYS) {
                    FileUtils.deleteDirectory(folder);
                    log.warn("Deleted folder {} ({} days old)", folderName, daysBetween);
                }
            } catch (Exception e) {
                log.warn("Skip invalid folder: {}", folderName);
            }
        }

        log.warn("Video folder cleanup completed.");
    }
}
