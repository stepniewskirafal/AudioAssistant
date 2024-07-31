package pl.stepniewski.audioassistant.utilities;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class FileUtils {

    public static final String TEXT_RESOURCES_PATH = "src/main/resources/text";
    public static final String AUDIO_RESOURCES_PATH = "src/main/resources/audio";

    public FileUtils() {
        try {
            Files.createDirectories(Paths.get(TEXT_RESOURCES_PATH));
        } catch (IOException e) {
            log.error("Error creating directories", e);
            throw new RuntimeException("Error creating directory", e);
        }
    }

    public void writeTextToFile(String textData, String fileName) {
        Path directory = Paths.get(TEXT_RESOURCES_PATH);
        Path filePath = directory.resolve(Paths.get(fileName).getFileName().toString());
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
            Files.writeString(filePath, textData, StandardOpenOption.CREATE_NEW);
            log.info("Saved {} to {}", fileName, TEXT_RESOURCES_PATH);
        } catch (IOException e) {
            log.error("Error writing text to file: {}", fileName, e);
            throw new UncheckedIOException("Error writing text to file", e);
        }
    }

    public void writeWordDocument(Map<String, String> data) {
        try (XWPFDocument document = new XWPFDocument()) {
            data.forEach((key, value) -> {
                String title = transformKeyToTitle(key);
                addTitleToWordDocument(document, title);
                addTextToWordDocument(document, value);
            });
            writeDocumentToFile(document);
            log.info("Word document created successfully");
        } catch (IOException e) {
            log.error("Error creating Word document", e);
            throw new UncheckedIOException(e);
        }
    }

    private String transformKeyToTitle(String key) {
        return Stream.of(key.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private void addTitleToWordDocument(XWPFDocument document, String title) {
        XWPFRun titleRun = document.createParagraph().createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setText(title);
    }

    private void addTextToWordDocument(XWPFDocument document, String text) {
        Pattern pattern = Pattern.compile("^\\d+\\. +.*");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            XWPFNumbering numbering = document.createNumbering();
            BigInteger numId = numbering.addNum(numbering.getAbstractNumID(BigInteger.valueOf(1)));
            try (Stream<String> lines = text.lines()) {
                lines.forEach(line -> {
                    XWPFParagraph paragraph = document.createParagraph();
                    paragraph.setNumID(numId);
                    paragraph.createRun().setText(line.substring(line.indexOf(".") + 1).trim());
                });
            } catch (Exception e) {
                log.error("Error processing numbered list in Word document", e);
                throw new RuntimeException(e);
            }
        } else {
            XWPFRun textRun = document.createParagraph().createRun();
            textRun.setText(text);
        }
    }

    private void writeDocumentToFile(XWPFDocument document) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(TEXT_RESOURCES_PATH + "/meeting_minutes.docx")) {
            document.write(fos);
            log.info("Word document saved to {}", TEXT_RESOURCES_PATH + "/meeting_minutes.docx");
        }
    }
}
