package pl.stepniewski.audioassistant.whisper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WavFileSplitter {

    @Value("${file.max.chunk.size}")
    private int maxChunkSize;

    public List<File> splitWavFileIntoChunks(File sourceWavFile) {
        log.info("Starting to split WAV file: {}", sourceWavFile.getAbsolutePath());
        List<File> chunks = new ArrayList<>();
        int chunkCounter = 1;

        validateSourceFile(sourceWavFile);

        try (var inputStream = AudioSystem.getAudioInputStream(sourceWavFile)) {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(sourceWavFile);
            AudioFormat format = fileFormat.getFormat();
            long totalFrames = inputStream.getFrameLength(); // Total frames in the source wav file
            long framesPerChunk = calculateFramesPerChunk(format);

            byte[] buffer = new byte[(int) (framesPerChunk * format.getFrameSize())];

            while (totalFrames > 0) {
                long framesInThisFile = Math.min(totalFrames, framesPerChunk);
                int bytesRead = inputStream.read(buffer, 0, (int) (framesInThisFile * format.getFrameSize()));
                if (bytesRead > 0) {
                    File chunkFile = createChunkFile(sourceWavFile, chunkCounter);
                    writeChunkFile(buffer, bytesRead, format, framesInThisFile, chunkFile);
                    chunks.add(chunkFile);
                    chunkCounter++;
                }
                totalFrames -= framesInThisFile;
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            log.error("Error occurred while splitting the WAV file: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        log.info("Finished splitting WAV file into {} chunks", chunks.size());
        return chunks;
    }

    private void validateSourceFile(File sourceWavFile) {
        if (!sourceWavFile.exists()) {
            log.error("Source file not found at: {}", sourceWavFile.getAbsolutePath());
            throw new IllegalArgumentException("Source file not found at: " + sourceWavFile.getAbsolutePath());
        }
    }

    private long calculateFramesPerChunk(AudioFormat format) {
        int frameSize = format.getFrameSize(); // Number of bytes in each frame
        return maxChunkSize / frameSize;
    }

    private File createChunkFile(File sourceWavFile, int chunkCounter) {
        return new File(sourceWavFile.getAbsolutePath().replace(".wav", "-%d.wav".formatted(chunkCounter)));
    }

    private void writeChunkFile(byte[] buffer, int bytesRead, AudioFormat format, long framesInThisFile, File chunkFile) throws IOException {
        try (var partStream = new AudioInputStream(
                new ByteArrayInputStream(buffer, 0, bytesRead),
                format,
                framesInThisFile)) {
            AudioSystem.write(partStream, AudioFileFormat.Type.WAVE, chunkFile);
            log.info("Created chunk file: {}", chunkFile.getAbsolutePath());
        }
    }
}

