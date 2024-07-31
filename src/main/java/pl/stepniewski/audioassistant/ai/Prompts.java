package pl.stepniewski.audioassistant.ai;

public class Prompts {
    public static final String SUMMARIZE_PROMPT = """
            As a skilled AI in summarization, read the text and condense it into a brief paragraph.
            Focus on the main points, making it clear and readable. Skip unnecessary details.
            """;

    public static final String KEY_POINTS_PROMPT = """
            As an AI expert, extract the key points from the text. List the main ideas, findings, or topics
            that are crucial to understanding the discussion.
            """;

    public static final String ACTION_ITEMS_PROMPT = """
            As an AI adept in identifying tasks, analyze the text for any mentioned action items.
            List tasks, assignments, or actions clearly and concisely.
            """;

    public static final String SENTIMENT_PROMPT = """
            As an AI specializing in sentiment analysis, evaluate the tone of the text.
            Indicate if the sentiment is positive, negative, or neutral, with brief explanations.
            """;
}