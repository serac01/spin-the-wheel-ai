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
    // Use router endpoint with wait_for_model to avoid cold-start 404s.
    private static final String HF_ROUTER = "https://router.huggingface.co";
    // Defaults to public models; can be overridden via env if you have gated/private access.
    // Pick widely available public models to avoid 404s from gated models; can override via env HF_TEXT_MODEL / HF_IMAGE_MODEL.
    private static final String DEFAULT_TEXT_MODEL = "HuggingFaceH4/zephyr-7b-beta"; // widely accessible on router
    private static final String DEFAULT_IMAGE_MODEL = "prompthero/openjourney-v4"; // SD1.5 derivative that is usually served

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

        String url = buildModelUrl(EnvUtil.get("HF_TEXT_MODEL"), DEFAULT_TEXT_MODEL);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
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
        String token = EnvUtil.get("HUGGINGFACE_API_TOKEN");
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "HUGGINGFACE_API_TOKEN is not configured");
        }

        String prompt = buildImagePrompt(arguments);

        Map<String, Object> payload = new HashMap<>();
        payload.put("inputs", prompt);
        Map<String, Object> params = new HashMap<>();
        params.put("num_inference_steps", 30);
        params.put("guidance_scale", 7.0);
        payload.put("parameters", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        // Accept any image format; the router/model can return jpeg/png/webp depending on the backend.
        headers.setAccept(java.util.List.of(MediaType.ALL));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        String url = buildModelUrl(EnvUtil.get("HF_IMAGE_MODEL"), DEFAULT_IMAGE_MODEL);
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("HuggingFace image call failed: status {} body {}", response.getStatusCode(), response.getBody());
                throw new ResponseStatusException(response.getStatusCode(), "HuggingFace image generation failed");
            }

            MediaType contentType = response.getHeaders().getContentType();
            if (contentType == null || !contentType.getType().equalsIgnoreCase("image")) {
                // Guard against JSON error bodies or empty content-type slips.
                log.error("HuggingFace image response not an image: content-type {}", contentType);
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "HuggingFace returned non-image payload");
            }

            return new ImageResult(response.getBody(), contentType);
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("HuggingFace image error: status {} body {}", ex.getStatusCode(), body);
            throw new ResponseStatusException(ex.getStatusCode(), "HuggingFace image error: " + body);
        }
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

    private String buildImagePrompt(SpinArguments args) {
        return "A cinematic illustration in the style of an art poster, set in " + args.getCity()
                + " around year " + args.getYear()
                + ". Main subject: " + (args.getGender() != null ? args.getGender().getDescription() : "person")
                + ". Vivid colors, high detail, dramatic lighting.";
    }

    private String extractText(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);

            // Common HF router shape: [{"generated_text": "..."}]
            if (node.isArray() && node.size() > 0) {
                JsonNode first = node.get(0);
                if (first.has("generated_text")) {
                    return first.get("generated_text").asText();
                }
            }

            // Some backends return {"generated_text": "..."}
            if (node.isObject() && node.has("generated_text")) {
                return node.get("generated_text").asText();
            }

            // Fallback: raw string body (already text)
            if (node.isTextual()) {
                return node.asText();
            }

            log.error("Unexpected HuggingFace response: {}", body);
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected HuggingFace response format");
        } catch (IOException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse HuggingFace response", e);
        }
    }

    private String buildModelUrl(String overrideModel, String defaultModel) {
        String model = (overrideModel != null && !overrideModel.isBlank()) ? overrideModel : defaultModel;
        return HF_ROUTER + "/models/" + model + "?wait_for_model=true";
    }

    public record ImageResult(byte[] data, MediaType contentType) {}
}
