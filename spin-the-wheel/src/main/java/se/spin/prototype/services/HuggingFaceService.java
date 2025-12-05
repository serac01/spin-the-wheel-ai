package se.spin.prototype.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import se.spin.prototype.Beans.SpinArguments;
import se.spin.prototype.util.EnvUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class HuggingFaceService {
    private static final Logger log = LoggerFactory.getLogger(HuggingFaceService.class);
    private static final String MODEL_URL = "https://api-inference.huggingface.co/models/AI-Sweden-Models/Llama-3-8B";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateStory(SpinArguments arguments, String seedText) {
        String token = EnvUtil.get("HUGGINGFACE_API_TOKEN");
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "HUGGINGFACE_API_TOKEN is not configured");
        }

        String prompt = buildPrompt(arguments, seedText);

        Map<String, Object> payload = new HashMap<>();
        payload.put("inputs", prompt);
        Map<String, Object> params = new HashMap<>();
        params.put("max_new_tokens", 256);
        params.put("temperature", 0.7);
        params.put("top_p", 0.9);
        payload.put("parameters", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(MODEL_URL, HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.error("HuggingFace call failed: status {} body {}", response.getStatusCode(), response.getBody());
            throw new ResponseStatusException(response.getStatusCode(), "HuggingFace generation failed");
        }

        return extractText(response.getBody());
    }

    private String buildPrompt(SpinArguments args, String seedText) {
        StringBuilder sb = new StringBuilder();
        sb.append("Use Swedish. Write a short, vivid story set in ")
                .append(args.getCity())
                .append(" around year ")
                .append(args.getYear())
                .append(" about a ")
                .append(args.getGender() != null ? args.getGender().getDescription() : "person")
                .append(".\n\nContext snippet from Firestore: \"")
                .append(seedText)
                .append("\"\n\nReturn only the story text.");
        return sb.toString();
    }

    private String extractText(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.isArray() && node.size() > 0) {
                JsonNode first = node.get(0);
                if (first.has("generated_text")) {
                    return first.get("generated_text").asText();
                }
            }
            // Some HF responses return {"error":...}
            log.error("Unexpected HuggingFace response: {}", body);
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected HuggingFace response format");
        } catch (IOException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse HuggingFace response", e);
        }
    }
}
