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

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Standard Hugging Face Inference API
    private static final String BASE_API_URL = "https://router.huggingface.co/models/";
    
    // Use working models - these are available on the free inference API
    private static final String TEXT_MODEL = "mistralai/Mistral-7B-Instruct-v0.2";
    private static final String IMAGE_MODEL = "stabilityai/stable-diffusion-2-1";


    public String generateStory(SpinArguments arguments, String seedText) {

        StringBuilder sb = new StringBuilder();

        sb.append("Use Swedish. Write a short, vivid story set in ").append(arguments.getCity())
            .append(" around year ").append(arguments.getYear())
            .append(" about a ").append(arguments.getGender())
            .append("\n\nContext: ").append(seedText)
            .append("\n\nReturn only the story text.");

        Map<String, Object> payload = new HashMap<>();
        payload.put("inputs", sb.toString());

        Map<String, Object> params = new HashMap<>();
        params.put("max_new_tokens", 256);
        params.put("temperature", 0.7);
        params.put("top_p", 0.9);
        params.put("return_full_text", false); // Only return generated text, not the prompt

        payload.put("parameters", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(EnvUtil.get("HUGGINGFACE_API_TOKEN"));
        
        try {

            ResponseEntity<String> response = restTemplate.exchange(
                BASE_API_URL + TEXT_MODEL,
                HttpMethod.POST, 
                new HttpEntity<>(payload, headers), 
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("HuggingFace call failed: status {} body {}", response.getStatusCode(), response.getBody());
                throw new ResponseStatusException(response.getStatusCode(), "HuggingFace generation failed");
            }

            return extractText(response.getBody());

        } catch (org.springframework.web.client.HttpStatusCodeException ex) {

            String body = ex.getResponseBodyAsString();
            log.error("HuggingFace text error: status {} body {}", ex.getStatusCode(), body);
            throw new ResponseStatusException(ex.getStatusCode(), "HuggingFace text error: " + body);

        }
    }

    public ImageResult generateImage(SpinArguments arguments) {

        String imagePrompt = "A cinematic illustration in the style of an art poster, set in " + arguments.getCity()
            + " around year " + arguments.getYear()
            + ". Main subject: " + (arguments.getGender() != null ? arguments.getGender().getDescription() : "person")
            + ". Vivid colors, high detail, dramatic lighting.";

        Map<String, Object> payload = new HashMap<>();
        payload.put("inputs", imagePrompt);

        Map<String, Object> params = new HashMap<>();
        params.put("num_inference_steps", 30);
        params.put("guidance_scale", 7.5);

        payload.put("parameters", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(EnvUtil.get("HUGGINGFACE_API_TOKEN"));

        try {

            // Image models return binary data directly
            ResponseEntity<byte[]> response = restTemplate.exchange(
                BASE_API_URL + IMAGE_MODEL,
                HttpMethod.POST,  
                new HttpEntity<>(payload, headers), 
                byte[].class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("HuggingFace image call failed: status {} body length {}", 
                    response.getStatusCode(), response.getBody() != null ? response.getBody().length : 0);
                throw new ResponseStatusException(response.getStatusCode(), "HuggingFace image generation failed");
            }

            // Check if response is actually an error (JSON) or image data
            byte[] responseBody = response.getBody();
            MediaType contentType = response.getHeaders().getContentType();

            // If Content-Type is application/json, it's probably an error
            if (contentType != null && contentType.includes(MediaType.APPLICATION_JSON)) {
                String errorBody = new String(responseBody);
                log.error("HuggingFace returned JSON instead of image: {}", errorBody);
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, 
                    "HuggingFace returned error: " + errorBody);
            }

            // Assume PNG if no content type specified
            MediaType imageType = (contentType != null && contentType.getType().equals("image")) 
                ? contentType 
                : MediaType.IMAGE_PNG;

            return new ImageResult(responseBody, imageType);

        } catch (org.springframework.web.client.HttpStatusCodeException ex) {

            String body = ex.getResponseBodyAsString();
            log.error("HuggingFace image error: status {} body {}", ex.getStatusCode(), body);
            throw new ResponseStatusException(ex.getStatusCode(), "HuggingFace image error: " + body);

        }
    }

    private String extractText(String body) {

        try {

            JsonNode node = objectMapper.readTree(body);

            // Standard HF format: [{"generated_text": "..."}]
            if (node.isArray() && node.size() > 0) {

                JsonNode first = node.get(0);

                if (first.has("generated_text")) {
                    return first.get("generated_text").asText();
                }
                
            }

            // Alternative format: {"generated_text": "..."}
            if (node.isObject() && node.has("generated_text")) {
                return node.get("generated_text").asText();
            }

            // Fallback: raw text
            if (node.isTextual()) {
                return node.asText();
            }

            log.error("Unexpected HuggingFace response: {}", body);

            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, 
                "Unexpected HuggingFace response format");

        } catch (IOException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to parse HuggingFace response", e);
        }
    }

    public record ImageResult(byte[] data, MediaType contentType) {}
}