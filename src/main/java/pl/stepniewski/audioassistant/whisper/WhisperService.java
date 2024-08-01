package pl.stepniewski.audioassistant.whisper;

import pl.stepniewski.audioassistant.ai.Prompts;
import pl.stepniewski.audioassistant.utilities.FileUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WhisperService {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private WhisperTranscribe whisperTranscribe;

    @Autowired
    private FileUtils fileUtils;

    public void processMinutes(String audioFileName) {
        log.info("Starting to process meeting minutes");
        // Transcribe audio, or load transcription if it already exists
        String transcription = getTranscription(audioFileName);

        Map<String, String> promptMap = Map.ofEntries(
                Map.entry("summarize", Prompts.SUMMARIZE_PROMPT),
                Map.entry("key_points", Prompts.KEY_POINTS_PROMPT),
                Map.entry("action_items", Prompts.ACTION_ITEMS_PROMPT),
                Map.entry("sentiment", Prompts.SENTIMENT_PROMPT)
        );

        ConcurrentMap<String, String> responseMap = promptMap.entrySet().parallelStream()
                .peek(e -> log.info("Processing {}", e.getKey()))
                .collect(Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                e -> getResponse(e.getValue(), transcription)
                        )
                );

        responseMap.forEach((name, response) -> {
            log.info("Writing response to file: {}.txt", name);
            fileUtils.writeTextToFile(response, name + ".txt");
        });
        fileUtils.writeWordDocument(responseMap);
        log.info("Finished processing minutes");
    }

    public String getResponse(String systemMessage, String userMessage) {
        log.debug("Generating response for prompt: {}", userMessage);
        final Response<AiMessage> modelResponse = chatLanguageModel.generate(
                SystemMessage.from(systemMessage),
                UserMessage.from(userMessage));

        final String text = modelResponse.content().text();
        return text;
    }

    public String getTranscription(String fileName) {
        Path transcriptionFilePath = Paths.get(FileUtils.TEXT_RESOURCES_PATH, fileName + ".txt");
        Path audioFilePath = Paths.get(FileUtils.AUDIO_RESOURCES_PATH, fileName + ".wav");

        if (Files.exists(transcriptionFilePath)) {
            log.info("Transcription file exists: {}", transcriptionFilePath);
            try {
                return Files.readString(transcriptionFilePath);
            } catch (IOException e) {
                log.error("Error reading transcription file: {}", e.getMessage(), e);
            }
        } else {
            log.info("Transcription file does not exist, starting transcription for: {}", audioFilePath);
            return whisperTranscribe.transcribe(audioFilePath.toString());
        }
        return "";
    }
}
