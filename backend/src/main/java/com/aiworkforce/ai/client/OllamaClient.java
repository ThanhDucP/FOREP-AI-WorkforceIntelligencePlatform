package com.aiworkforce.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OllamaClient {
    
    // In a real scenario, this would use RestTemplate or WebClient to call http://localhost:11434/api/generate
    
    public String generateInsight(String prompt) {
        log.info("Calling Ollama with prompt: {}", prompt);
        
        // Mock Response for MVP
        if (prompt.contains("HIGH risk")) {
            return "This employee is under extreme pressure and shows multiple overdue tasks. They are at high risk of burnout. Recommendation: Schedule a 1-on-1 immediately to redistribute their workload.";
        } else if (prompt.contains("MEDIUM risk")) {
            return "The employee is managing fine but starting to accumulate some delay. They might be approaching their capacity limit. Recommendation: Monitor their next tasks and avoid assigning critical priority items this week.";
        } else {
            return "The employee is performing optimally with a balanced workload. No signs of burnout detected. Recommendation: Acknowledge their consistent output.";
        }
    }
}
