package pl.stepniewski.audioassistant.whisper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.stepniewski.audioassistant.utilities.FileUtils;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Slf4j
@Service
public class WhisperTranscribe {

    @Value("${openai.api.audio.model}")
    private String model;

    @Value("${file.max.allowed.size}")
    private int maxAllowedSize;

    @Value("${file.max.chunk.size}")
    private int maxChunkSize;

    @Value("${word.list}")
    private String keyWordList;

    @Autowired
    private WavFileSplitter wavFileSplitter;

    @Autowired
    private WebClient whisperWebClient;

    @Autowired
    private FileUtils fileUtils;

    private Mono<String> transcribeChunk(String prompt, File chunkFile) {
        log.info("Transcribing {}", chunkFile.getName());

        prompt += keyWordList;

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new PathResource(chunkFile.toPath()));
        bodyBuilder.part("model", model);
        bodyBuilder.part("response_format", "text");
        bodyBuilder.part("language", "en");
        bodyBuilder.part("prompt", prompt);

        return whisperWebClient.post()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(bodyBuilder.build())
                .retrieve()
                .bodyToMono(String.class);
    }

    public String transcribe(String fileName) {
        log.info("Transcribing {}", fileName);
        File file = new File(fileName);

        List<String> transcriptions = new ArrayList<>();
        Queue<String> lastTwoTranscriptions = new LinkedList<>();
        String prompt = "";

        if (file.length() <= maxAllowedSize) {
            String transcription = transcribeChunk(keyWordList, file).block();
            transcriptions.add(transcription);
        } else {
            List<File> chunks = wavFileSplitter.splitWavFileIntoChunks(file);
            for (File chunk : chunks) {
                String transcription = transcribeChunk(prompt, chunk).block();
                transcriptions.add(transcription);

                if (lastTwoTranscriptions.size() == 2) {
                    lastTwoTranscriptions.poll();
                }
                lastTwoTranscriptions.offer(transcription);

                prompt = String.join(" ", lastTwoTranscriptions);

                if (!chunk.delete()) {
                    log.warn("Failed to delete {}", chunk.getName());
                }
            }
        }

        String transcription = String.join(" ", transcriptions);
        String fileNameWithoutPath = fileName.substring(fileName.lastIndexOf("/") + 1);
        fileUtils.writeTextToFile(transcription, fileNameWithoutPath.replace(".wav", ".txt"));
        return transcription;
    }

}
