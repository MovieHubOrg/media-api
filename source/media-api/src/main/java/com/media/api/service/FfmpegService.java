package com.media.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FfmpegService {
    public enum QualityTarget {
        P720, P1080, P1440, ORIGINAL
    }

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    public List<String> generateFfmpegCommand(String inputPath, String outputPath, String outputSegmentPath,
                                              Map<String, Integer> bitrateMap, int width, int height,
                                              boolean hasAudio, QualityTarget target, String masterPlaylistPath) {
        List<String> levels = new ArrayList<>();

        // Xác định các mức chất lượng
        switch (target) {
            case P720:
                levels.add("720");
                break;
            case P1080:
                levels.addAll(List.of("720", "1080"));
                break;
            case P1440:
                levels.addAll(List.of("720", "1080", "1440"));
                break;
            case ORIGINAL:
                levels.addAll(List.of("720", "1080", "1440", "original"));
                break;
        }

        List<String> command = new ArrayList<>();

        command.add(ffmpegPath);
        command.add("-y");
        command.add("-loglevel");
        command.add("error");
//        command.add("info");
        command.add("-i");
        command.add(inputPath);
        command.add("-preset");
        command.add("fast");
        command.add("-g");
        command.add("48");
        command.add("-crf");
        command.add("23");
        command.add("-sc_threshold");
        command.add("0");

        StringBuilder varMap = new StringBuilder();

        for (int i = 0; i < levels.size(); i++) {
            String level = levels.get(i);
            String levelName = level.equals("original") ? "original" : level + "p";
            int targetHeight = level.equals("original") ? height : Integer.parseInt(level);
            int targetWidth = getEvenWidth(height, width, targetHeight);
            int bitrate = bitrateMap.getOrDefault(level, 5000000);

            // Map video và audio (chung cho mọi chất lượng)
            command.add("-map");
            command.add("0:v:0");
            if (hasAudio) {
                command.add("-map");
                command.add("0:a:0");
            }

            // Video encoding config
            command.add("-s:v:" + i);
            command.add(targetWidth + "x" + targetHeight);
            command.add("-c:v:" + i);
            command.add("libx264");

            int bitrateKbps = bitrate / 1000;
            int maxrateKbps = (int) (bitrateKbps * 1.07);
            int bufsizeKbps = bitrateKbps * 2;

            command.add("-b:v:" + i);
            command.add(bitrateKbps + "k");
            command.add("-maxrate:v:" + i);
            command.add(maxrateKbps + "k");
            command.add("-bufsize:v:" + i);
            command.add(bufsizeKbps + "k");

            // Audio encoding config
            if (hasAudio) {
                int audioBitrate;
                if (i == 0) {
                    audioBitrate = 192;
                } else if (i == 1) {
                    audioBitrate = 128;
                } else {
                    audioBitrate = 96;
                }

                command.add("-c:a:" + i);
                command.add("aac");
                command.add("-b:a:" + i);
                command.add(audioBitrate + "k");
                command.add("-ac");
                command.add("2");
            }

            // var_stream_map entry
            if (hasAudio) {
                varMap.append(String.format("v:%d,a:%d,name:%s ", i, i, levelName));
            } else {
                varMap.append(String.format("v:%d,name:%s ", i, levelName));
            }
        }

        // HLS options
        command.add("-var_stream_map");
        command.add(varMap.toString().trim());
        command.add("-master_pl_name");
        command.add("master.m3u8");
        command.add("-f");
        command.add("hls");
        command.add("-hls_time");
        command.add("15");
        command.add("-hls_list_size");
        command.add("0");
        command.add("-hls_flags");
        command.add("independent_segments");
        command.add("-hls_segment_type");
        command.add("mpegts");
        command.add("-hls_segment_filename");
        command.add(outputSegmentPath);
        command.add(outputPath);

        return command;
    }

    public void generateThumbnail(String inputPath, String thumbnailOutputPattern) throws IOException, InterruptedException {
//        ffmpegPath, "-i", inputPath, "-vf", "fps=1,scale=iw/2:-1", outputDir + "/thumb_%06d.jpg"
        log.warn("Generating thumbnails...");

        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-loglevel");
        command.add("error");
        command.add("-i");
        command.add(inputPath);
        command.add("-vf");
        command.add("fps=1,scale=512:-1"); // iw = input width, chia đôi; -1 giữ tỉ lệ chiều cao
        command.add("-q:v");
        command.add("2");
        command.add(thumbnailOutputPattern); // example: /path/to/thumbnails/thumb_%06d.jpg
        log.warn("command: {}", command);

        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();

        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.warn("[FFmpeg ERROR] {}", line);
                }
            } catch (IOException e) {
                log.error("Error reading FFmpeg stderr", e);
            }
        });
        errorThread.start();

        int exitCode = process.waitFor();
        errorThread.join();

        if (exitCode != 0) {
            log.error("FFmpeg exited with error. Exit code: {}", exitCode);
        } else {
            log.warn("FFmpeg finished successfully.");
        }
    }

    public void generateSprite(Path outputFolder) throws IOException, InterruptedException {
        log.warn("Generating sprite from thumbnails...");

        Path thumbnailFolder = outputFolder.resolve("thumbnails");
        Path spriteOutputPath = outputFolder.resolve("sprite.jpg"); // Lưu sprite tại outputFolder, KHÔNG phải thumbnails

        // 1. Đếm số lượng thumbnail
        File[] thumbnails = thumbnailFolder.toFile().listFiles((dir, name) -> name.matches("thumb_\\d{6}\\.jpg"));
        if (thumbnails == null || thumbnails.length == 0) {
            throw new RuntimeException("No thumbnails found to create sprite.");
        }

        int total = thumbnails.length;
        int columns = 10;
        int rows = (int) Math.ceil(total / (double) columns);

        // 2. Tạo lệnh FFmpeg
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);              // Ví dụ: "ffmpeg"
        command.add("-y");                    // Ghi đè nếu có
        command.add("-loglevel");
        command.add("error");
        command.add("-framerate");
        command.add("1");
        command.add("-i");
        command.add("thumb_%06d.jpg");        // Input: các ảnh thumbnail trong thư mục hiện tại
        command.add("-filter_complex");
        command.add("tile=" + columns + "x" + rows);
        command.add("-frames:v");
        command.add("1");
        command.add("-update");
        command.add("1");

        // Ghi ra file sprite.jpg ở thư mục cha (outputFolder), vì working dir là thumbnails
        Path relativeSpritePath = thumbnailFolder.relativize(spriteOutputPath);
        command.add(relativeSpritePath.toString().replace("\\", "/")); // Dùng đường dẫn tương đối hoặc ../sprite.jpg
        log.warn("command: {}", command);
        // 3. Chạy FFmpeg tại thư mục thumbnails
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(thumbnailFolder.toFile());
        builder.redirectErrorStream(true);

        Process process = builder.start();

        // 4. Log output FFmpeg
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            boolean hasError = false;
            while ((line = reader.readLine()) != null) {
                log.warn("[FFmpeg SPRITE ERROR] {}", line);
                String lowerLine = line.toLowerCase();
                if (lowerLine.contains("error") || lowerLine.contains("failed") || lowerLine.contains("invalid")) {
                    hasError = true;
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0 || hasError) {
                log.error("Failed to generate sprite. Exit code: {}", exitCode);
                throw new RuntimeException("Sprite generation failed. Exit code: " + exitCode);
            }
        }
        log.info("Sprite generated at: {}", spriteOutputPath);
    }

    public void generateVttFile(File thumbnailFolder, File vttFile) throws IOException {
        log.warn("Generating VTT file...");
        int columns = 10;
        int secondsPerThumb = 1;

        File[] thumbs = thumbnailFolder.listFiles((dir, name) -> name.matches("thumb_\\d{6}\\.jpg"));
        if (thumbs == null || thumbs.length == 0) {
            throw new IOException("No thumbnails found to generate VTT.");
        }
        BufferedImage img = ImageIO.read(thumbs[0]);
        int tileHeight = img.getHeight();
        int tileWidth = img.getWidth();

        // Sắp xếp theo số thứ tự trong tên file: thumb_001.jpg → 1
        Arrays.sort(thumbs, Comparator.comparingInt(f -> {
            Matcher m = Pattern.compile("thumb_(\\d{6})\\.jpg").matcher(f.getName());
            return m.find() ? Integer.parseInt(m.group(1)) : 0;
        }));

        try (PrintWriter writer = new PrintWriter(new FileWriter(vttFile))) {
            writer.println("WEBVTT\n");

            for (int i = 0; i < thumbs.length; i++) {
                int x = (i % columns) * tileWidth;
                int y = (i / columns) * tileHeight;

                int startSec = i * secondsPerThumb + 1;
                int endSec = startSec + secondsPerThumb;

                String start = formatTime(startSec);
                String end = formatTime(endSec);

                writer.println(start + " --> " + end);
                writer.println("sprite.jpg#xywh=" + x + "," + y + "," + tileWidth + "," + tileHeight);
                writer.println();
            }
        }

        log.info("VTT file generated successfully: {}", vttFile.getAbsolutePath());
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d.000", hours, minutes, seconds);
    }

    private int getEvenWidth(int originalHeight, int originalWidth, int targetHeight) {
        int computedWidth = (targetHeight * originalWidth) / originalHeight;
        return computedWidth % 2 == 0 ? computedWidth : computedWidth + 1;
    }
}
