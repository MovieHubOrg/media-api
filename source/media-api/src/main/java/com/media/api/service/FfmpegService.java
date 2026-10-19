package com.media.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
        command.add("-y");                    // Ghi đè file output nếu đã tồn tại
        command.add("-loglevel");
        command.add("error");                 // Chỉ log lỗi
//        command.add("info");
        command.add("-i");
        command.add(inputPath);               // File input
        command.add("-preset");
        command.add("fast");                  // Tốc độ encode nhanh, đánh đổi nén
        command.add("-g");
        command.add("48");                    // Khoảng cách giữa 2 keyframe (GOP size)
        command.add("-crf");
        command.add("23");                    // Mức chất lượng video (số nhỏ hơn = chất lượng cao hơn)
        command.add("-sc_threshold");
        command.add("0");                     // Tắt phát hiện đổi cảnh để giữ GOP cố định (phục vụ HLS)

        StringBuilder varMap = new StringBuilder();

        for (int i = 0; i < levels.size(); i++) {
            String level = levels.get(i);
            String levelName = level.equals("original") ? "original" : level + "p";
            int targetHeight = level.equals("original") ? height : Integer.parseInt(level);
            int targetWidth = getEvenWidth(height, width, targetHeight);
            int bitrate = bitrateMap.getOrDefault(level, 5000000);

            // Map video và audio (chung cho mọi chất lượng)
            command.add("-map");
            command.add("0:v:0");                 // Chọn stream video số 0 của input
            if (hasAudio) {
                command.add("-map");
                command.add("0:a:0");             // Chọn stream audio số 0 của input
            }

            // Video encoding config
            command.add("-s:v:" + i);
            command.add(targetWidth + "x" + targetHeight); // Độ phân giải output cho level thứ i
            command.add("-c:v:" + i);
            command.add("libx264");                        // Codec video H.264

            int bitrateKbps = bitrate / 1000;
            int maxrateKbps = (int) (bitrateKbps * 1.07);
            int bufsizeKbps = bitrateKbps * 2;

            command.add("-b:v:" + i);
            command.add(bitrateKbps + "k");        // Bitrate video mục tiêu
            command.add("-maxrate:v:" + i);
            command.add(maxrateKbps + "k");        // Bitrate tối đa (cho phép vượt nhẹ so với target)
            command.add("-bufsize:v:" + i);
            command.add(bufsizeKbps + "k");        // Kích thước buffer cho VBV rate control

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
                command.add("aac");                    // Codec audio AAC
                command.add("-b:a:" + i);
                command.add(audioBitrate + "k");       // Bitrate audio theo level
                command.add("-ac");
                command.add("2");                      // Số kênh audio (stereo)
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
        command.add(varMap.toString().trim());   // Danh sách các variant stream (video/audio theo từng level)
        command.add("-master_pl_name");
        command.add("master.m3u8");               // Tên file playlist tổng (master)
        command.add("-f");
        command.add("hls");                       // Định dạng output là HLS
        command.add("-hls_time");
        command.add("15");                        // Độ dài mỗi segment (giây)
        command.add("-hls_list_size");
        command.add("0");                         // 0 = giữ lại toàn bộ segment trong playlist (VOD)
        command.add("-hls_flags");
        command.add("independent_segments");      // Mỗi segment có thể decode độc lập
        command.add("-hls_segment_type");
        command.add("mpegts");                    // Định dạng container cho từng segment
        command.add("-hls_segment_filename");
        command.add(outputSegmentPath);           // Pattern đặt tên file segment
        command.add(outputPath);                  // Đường dẫn file playlist theo level

        return command;
    }

    public int generateThumbnail(String inputPath, String thumbnailOutputPattern, long durationSeconds) throws IOException, InterruptedException {
//        ffmpegPath, "-i", inputPath, "-vf", "fps=1,scale=iw/2:-1", outputDir + "/thumb_%06d.jpg"
        log.warn("Generating thumbnails...");

        int secondsPerThumb;
        if (durationSeconds <= 600) { // 10p
            secondsPerThumb = 2;
        } else if (durationSeconds <= 1800) { // 30p
            secondsPerThumb = 5;
        } else {
            secondsPerThumb = 10;
        }

        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-loglevel");
        command.add("error");                 // Chỉ log lỗi
        command.add("-i");
        command.add(inputPath);               // File video input
        command.add("-vf");
        command.add("fps=1/" + secondsPerThumb + ",scale=320:-2"); // Lấy 1 frame mỗi secondsPerThumb giây, resize width=320, -2 giữ tỉ lệ chiều cao
        command.add("-q:v");
        command.add("3");                     // Chất lượng ảnh JPEG (số nhỏ hơn = chất lượng cao hơn)
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
        return secondsPerThumb;
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
        command.add("error");                 // Chỉ log lỗi
        command.add("-framerate");
        command.add("1");                     // Đọc lần lượt các ảnh input với tốc độ 1 ảnh/giây
        command.add("-i");
        command.add("thumb_%06d.jpg");        // Input: các ảnh thumbnail trong thư mục hiện tại
        command.add("-filter_complex");
        command.add("tile=" + columns + "x" + rows); // Ghép tất cả ảnh thành lưới columns x rows
        command.add("-frames:v");
        command.add("1");                     // Chỉ xuất 1 frame (ảnh sprite duy nhất)
        command.add("-update");
        command.add("1");                     // Cho phép ghi đè liên tục lên cùng 1 file output ảnh

        // Ghi ra file sprite.jpg ở thư mục cha (outputFolder), vì working dir là thumbnails
        Path relativeSpritePath = thumbnailFolder.relativize(spriteOutputPath);
        command.add(relativeSpritePath.toString().replace("\\", "/")); // Dùng đường dẫn tương đối hoặc ../sprite.jpg
        log.warn("command: {}", command);

        // 3. Chạy FFmpeg tại thư mục thumbnails
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(thumbnailFolder.toFile());
        builder.redirectErrorStream(false);

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

    public void generateVttFile(File thumbnailFolder, File vttFile, int secondsPerThumb) throws IOException {
        log.warn("Generating VTT file...");
        int columns = 10;

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

                int startSec = i * secondsPerThumb;
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

    public void convertM3u8ToAudio(String inputM3u8Path, String outputAudioPath) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");                    // Ghi đè file output nếu đã tồn tại
        command.add("-loglevel");
        command.add("error");                 // Chỉ log lỗi

        if (inputM3u8Path.startsWith("https://")) {
            command.add("-allowed_extensions");
            command.add("ALL");               // Cho phép mọi phần mở rộng file trong playlist HLS
            command.add("-protocol_whitelist");
            command.add("file,http,https,tcp,tls,crypto"); // Các protocol được phép khi đọc m3u8 từ URL
        }

        command.add("-i");
        command.add(inputM3u8Path);            // File/URL playlist m3u8 input
        command.add("-vn");                    // Bỏ qua stream video, chỉ lấy audio
        command.add("-ar");
        command.add("16000");                  // Tần số lấy mẫu audio (Hz)
        command.add("-ac");
        command.add("1");                      // Số kênh audio (mono)
        command.add("-c:a");
        command.add("pcm_s16le");              // Codec audio PCM 16-bit little-endian (WAV)
        command.add(outputAudioPath);          // Đường dẫn file audio output
        log.info("Converting m3u8 to audio with command: {}", command);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);
        Process process = processBuilder.start();
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            boolean hasError = false;
            String line;
            while ((line = errorReader.readLine()) != null) {
                log.warn("[FFmpeg AUDIO] {}", line);
                String lower = line.toLowerCase();
                if (lower.contains("error") || lower.contains("failed") || lower.contains("invalid")) {
                    hasError = true;
                }
            }

            boolean finished = process.waitFor(2, TimeUnit.HOURS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("FFmpeg audio conversion timed out after 2 hours.");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0 || hasError) {
                throw new RuntimeException("FFmpeg audio conversion failed. Exit code: " + exitCode);
            }
        }
        log.info("Audio file generated successfully: {}", outputAudioPath);
    }
}
