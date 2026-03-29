package com.media.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
@Service
public class FFprobeService {
    @Value("${ffprobe.path}")
    private String ffprobePath;

    public int getBitrate(String inputPath) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                ffprobePath, "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=bit_rate",
                "-of", "default=noprint_wrappers=1:nokey=1",
                inputPath
        );
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine();
            process.waitFor();
            if (output == null || output.trim().isEmpty() || output.trim().equalsIgnoreCase("N/A")) {
                return 5_000_000; // fallback bitrate default
            }
            return Integer.parseInt(output.trim());
        }
    }

    public Dimension getResolution(String inputPath) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                ffprobePath, "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=s=x:p=0",
                inputPath
        );
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine();
            process.waitFor();
            String[] parts = output.trim().split("x");
            return new Dimension(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    public boolean hasAudio(String inputPath) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                ffprobePath, "-v", "error",
                "-select_streams", "a:0",
                "-show_entries", "stream=codec_type",
                "-of", "default=noprint_wrappers=1:nokey=1",
                inputPath
        );
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine();
            process.waitFor();
            return "audio".equalsIgnoreCase(output.trim());
        }
    }

    public long getDuration(String inputPath) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                ffprobePath, "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                inputPath
        );
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine();
            process.waitFor();
            if (output == null || output.trim().isEmpty()) {
                return 0L;
            }
            double duration = Double.parseDouble(output.trim());
            return (long) duration;
        }
    }
}
