package com.aiworkforce.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String provider = "gemini";
    private String model = "gemini-2.5-flash";
    private Gemini gemini = new Gemini();
    private OpenAi openai = new OpenAi();
    private Rag rag = new Rag();

    @Data
    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-2.5-flash";
        private int timeoutSeconds = 30;
    }

    @Data
    public static class OpenAi {
        private String apiKey = "";
        private String model = "gemini-2.5-flash";
        private int timeoutSeconds = 30;
    }

    @Data
    public static class Rag {
        private boolean enabled = true;
        private int maxContextCharacters = 4000;
        private int maxTasks = 12;
        private int maxPreviousInsights = 3;
    }
}