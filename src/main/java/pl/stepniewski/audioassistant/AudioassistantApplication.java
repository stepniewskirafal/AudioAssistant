package pl.stepniewski.audioassistant;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import pl.stepniewski.audioassistant.whisper.WhisperService;

@SpringBootApplication
public class AudioassistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(AudioassistantApplication.class, args);
	}
	@Bean
	public CommandLineRunner commandLineRunner(ChatLanguageModel chatLanguageModel, WhisperService whisperTutorial) {
		return args -> {
			whisperTutorial.processMinutes("Ronald_Reagan_First_Inaugural");
		};
	}
}
