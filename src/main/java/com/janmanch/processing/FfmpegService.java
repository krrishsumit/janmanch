package com.janmanch.processing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Thin wrapper around the ffmpeg and ffprobe command line tools. */
@Service
public class FfmpegService {

    private static final Logger log = LoggerFactory.getLogger(FfmpegService.class);
    private static final long TIMEOUT_SECONDS = 120;

    /** Video length in seconds, or empty if ffprobe is missing or the file is unreadable. */
    public Optional<Integer> probeDurationSeconds(Path video) {
        Optional<String> output = run(List.of("ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                video.toString()));
        try {
            return output.map(String::trim).filter(s -> !s.isEmpty())
                    .map(s -> (int) Math.round(Double.parseDouble(s)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /** Saves a 640px-wide JPEG frame taken one second into the video. */
    public boolean createThumbnail(Path video, Path thumbnail) {
        return run(List.of("ffmpeg", "-y", "-ss", "1", "-i", video.toString(),
                "-frames:v", "1", "-vf", "scale=640:-2", thumbnail.toString())).isPresent();
    }

    /** Runs a command; returns its output on success, empty on any failure. */
    private Optional<String> run(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("{} timed out", command.get(0));
                return Optional.empty();
            }
            if (process.exitValue() != 0) {
                log.warn("{} failed: {}", command.get(0), output);
                return Optional.empty();
            }
            return Optional.of(output);
        } catch (IOException ex) {
            log.warn("{} could not be started. Is FFmpeg installed?", command.get(0));
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
