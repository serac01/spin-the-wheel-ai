package se.spin.prototype.Controllers;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;
import se.spin.prototype.services.FirestoreService;
import se.spin.prototype.services.HuggingFaceService;
import se.spin.prototype.Beans.CompareScenariosRequest;
import se.spin.prototype.Beans.GeneratedTextSources;
import se.spin.prototype.Beans.SpinArguments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        String story = huggingFaceService.generateStory(
            arguments, 
            firestoreService.fetchSeedText(
                arguments.getCity(), 
                arguments.getYear(), 
                arguments.getGender()
            ).orElse("No matching Firestore seed; use the provided context to craft a new story.")
        );

        GeneratedTextSources generatedTextSources = new GeneratedTextSources();
        generatedTextSources.setGeneratedText(story);

        List<String> sources = new ArrayList<>();
        sources.add("firestore:stories");
        sources.add("huggingface:AI-Sweden-Models/Llama-3-8B");

        generatedTextSources.setSources(sources);

        return generatedTextSources;
    }

    @PostMapping(value = "/story/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter postGeneratedTextStream(@RequestBody SpinArguments arguments) {
        validateSpinArguments(arguments);

        String seed = firestoreService.fetchSeedText(
            arguments.getCity(),
            arguments.getYear(),
            arguments.getGender()
        ).orElse("No matching Firestore seed; use the provided context to craft a new story.");

        SseEmitter emitter = new SseEmitter(0L);

        huggingFaceService.streamStory(arguments, seed)
            .doOnNext(chunk -> {
                try {
                    emitter.send(SseEmitter.event().data(chunk));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            })
            .doOnComplete(emitter::complete)
            .doOnError(emitter::completeWithError)
            .subscribe();

        return emitter;
    }

    @PostMapping("/image")
    public ResponseEntity<ByteArrayResource> postGeneratedImage(@RequestBody SpinArguments body) throws IOException {

        validateSpinArguments(body);

        var imageResult = huggingFaceService.generateImage(body);

        return ResponseEntity.ok()
                .contentType(imageResult.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"story-image.png\"")
                .body(new ByteArrayResource(imageResult.data()));
    }

    @PostMapping("/compare-scenarios")
    public GeneratedTextSources postCompareScenarios(@RequestBody CompareScenariosRequest arguments) {

        if (arguments == null || arguments.getSpinArgumentsFirstStory() == null || arguments.getSpinArgumentsSecondStory() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both scenarios are required");
        }

        validateSpinArguments(arguments.getSpinArgumentsFirstStory());
        validateSpinArguments(arguments.getSpinArgumentsSecondStory());

        SpinArguments firstArgs = arguments.getSpinArgumentsFirstStory();
        SpinArguments secondArgs = arguments.getSpinArgumentsSecondStory();

        String firstStory = huggingFaceService.generateStory(
            firstArgs, 
            firestoreService.fetchSeedText(
                firstArgs.getCity(), 
                firstArgs.getYear(), 
                firstArgs.getGender()
            )
            .orElse("No matching Firestore seed; use the provided context to craft a new story.")
        );
        String secondStory = huggingFaceService.generateStory(
            secondArgs, 
            firestoreService.fetchSeedText(
                secondArgs.getCity(), 
                secondArgs.getYear(), 
                secondArgs.getGender()
            )
            .orElse("No matching Firestore seed; use the provided context to craft a new story.")
        );

        String comparison = huggingFaceService.compareStories(firstArgs, secondArgs, firstStory, secondStory);

        GeneratedTextSources generatedTextSources = new GeneratedTextSources();
        generatedTextSources.setGeneratedText(comparison);

        List<String> sources = new ArrayList<>();
        sources.add("firestore:stories");
        sources.add("huggingface:AI-Sweden-Models/Llama-3-8B");

        generatedTextSources.setSources(sources);

        return generatedTextSources;
    }

    @PostMapping(value = "/compare-scenarios/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter postCompareScenariosStream(@RequestBody CompareScenariosRequest arguments) {

        if (arguments == null || arguments.getSpinArgumentsFirstStory() == null || arguments.getSpinArgumentsSecondStory() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both scenarios are required");
        }

        validateSpinArguments(arguments.getSpinArgumentsFirstStory());
        validateSpinArguments(arguments.getSpinArgumentsSecondStory());

        SpinArguments firstArgs = arguments.getSpinArgumentsFirstStory();
        SpinArguments secondArgs = arguments.getSpinArgumentsSecondStory();

        String firstStory = huggingFaceService.generateStory(
            firstArgs,
            firestoreService.fetchSeedText(
                firstArgs.getCity(),
                firstArgs.getYear(),
                firstArgs.getGender()
            )
            .orElse("No matching Firestore seed; use the provided context to craft a new story.")
        );
        String secondStory = huggingFaceService.generateStory(
            secondArgs,
            firestoreService.fetchSeedText(
                secondArgs.getCity(),
                secondArgs.getYear(),
                secondArgs.getGender()
            )
            .orElse("No matching Firestore seed; use the provided context to craft a new story.")
        );

        SseEmitter emitter = new SseEmitter(0L);

        huggingFaceService.streamCompareStories(firstArgs, secondArgs, firstStory, secondStory)
            .doOnNext(chunk -> {
                try {
                    emitter.send(SseEmitter.event().data(chunk));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            })
            .doOnComplete(emitter::complete)
            .doOnError(emitter::completeWithError)
            .subscribe();

        return emitter;
    }

    private void validateSpinArguments(SpinArguments arguments) {
        if (arguments == null || arguments.getCity() == null || arguments.getYear() == null || arguments.getGender() == null || arguments.getGender().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "City, year and gender are required");
        }
    }
}
