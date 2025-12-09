package se.spin.prototype.Controllers;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import se.spin.prototype.services.FirestoreService;
import se.spin.prototype.services.HuggingFaceService;
import se.spin.prototype.Beans.CompareScenariosRequest;
import se.spin.prototype.Beans.GeneratedTextSources;
import se.spin.prototype.Beans.SpinArguments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/spin")
public class SpinController {

    private final FirestoreService firestoreService;
    private final HuggingFaceService huggingFaceService;

    public SpinController(FirestoreService firestoreService, HuggingFaceService huggingFaceService) {
        this.firestoreService = firestoreService;
        this.huggingFaceService = huggingFaceService;
    }

    @PostMapping("/story")
    public GeneratedTextSources postGeneratedText(@RequestBody SpinArguments arguments) {
        validateSpinArguments(arguments);

        Optional<String> seed = firestoreService.fetchSeedText(arguments.getCity(), arguments.getYear(), arguments.getGender());
        String seedText = seed.orElse("No matching Firestore seed; use the provided context to craft a new story.");
        String story = huggingFaceService.generateStory(arguments, seedText);

        GeneratedTextSources generatedTextSources = new GeneratedTextSources();
        generatedTextSources.setGeneratedText(story);
        List<String> sources = new ArrayList<>();
        sources.add("firestore:stories");
        sources.add("huggingface:AI-Sweden-Models/Llama-3-8B");
        generatedTextSources.setSources(sources);
        return generatedTextSources;
    }

    @PostMapping("/image")
    public ResponseEntity<ByteArrayResource> postGeneratedImage(@RequestBody SpinArguments body) throws IOException {
        validateSpinArguments(body);

        var imageResult = huggingFaceService.generateImage(body);
        ByteArrayResource bodyResource = new ByteArrayResource(imageResult.data());

        return ResponseEntity.ok()
                .contentType(imageResult.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"story-image.png\"")
                .body(bodyResource);
    }

    @PostMapping("/compare-scenarios")
    public GeneratedTextSources postCompareScenarios(@RequestBody CompareScenariosRequest arguments) {
        if (arguments == null
                || arguments.getSpinArgumentsFirstStory() == null
                || arguments.getSpinArgumentsSecondStory() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both scenarios are required");
        }
        validateSpinArguments(arguments.getSpinArgumentsFirstStory());
        validateSpinArguments(arguments.getSpinArgumentsSecondStory());

        SpinArguments firstArgs = arguments.getSpinArgumentsFirstStory();
        SpinArguments secondArgs = arguments.getSpinArgumentsSecondStory();

        String firstSeed = firestoreService.fetchSeedText(firstArgs.getCity(), firstArgs.getYear(), firstArgs.getGender())
            .orElse("No matching Firestore seed; use the provided context to craft a new story.");
        String secondSeed = firestoreService.fetchSeedText(secondArgs.getCity(), secondArgs.getYear(), secondArgs.getGender())
            .orElse("No matching Firestore seed; use the provided context to craft a new story.");

        String firstStory = huggingFaceService.generateStory(firstArgs, firstSeed);
        String secondStory = huggingFaceService.generateStory(secondArgs, secondSeed);

        StringBuilder comparison = new StringBuilder();
        comparison.append("Story 1 (" + firstArgs.getCity() + ", " + firstArgs.getYear() + "):\n")
            .append(firstStory)
            .append("\n\nStory 2 (" + secondArgs.getCity() + ", " + secondArgs.getYear() + "):\n")
            .append(secondStory);

        GeneratedTextSources generatedTextSources = new GeneratedTextSources();
        generatedTextSources.setGeneratedText(comparison.toString());
        List<String> sources = new ArrayList<>();
        sources.add("firestore:stories");
        sources.add("huggingface:text-model");
        generatedTextSources.setSources(sources);
        return generatedTextSources;
    }

    private void validateSpinArguments(SpinArguments arguments) {
        if (arguments == null
                || arguments.getCity() == null
                || arguments.getYear() == null
                || arguments.getGender() == null
                || arguments.getGender().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "City, year and gender are required");
        }
    }
}
