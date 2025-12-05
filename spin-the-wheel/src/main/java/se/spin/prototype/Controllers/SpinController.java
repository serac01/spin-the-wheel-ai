package se.spin.prototype.Controllers;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import se.spin.prototype.services.FirestoreService;
import se.spin.prototype.services.HuggingFaceService;
import se.spin.prototype.Beans.CompareScenariosRequest;
import se.spin.prototype.Beans.GenderEnum;
import se.spin.prototype.Beans.GeneratedTextSources;
import se.spin.prototype.Beans.SpinArguments;

import java.io.IOException;
import java.io.InputStream;
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
        sources.add("huggingface:AI-Sweden-Models/gpt-sw3-40b");
        generatedTextSources.setSources(sources);
        return generatedTextSources;
    }

    @PostMapping("/image")
    public ResponseEntity<ByteArrayResource> postGeneratedImage(@RequestBody SpinArguments body) throws IOException {
        validateSpinArguments(body);

        String filename = "paula-rego.jpg";
        if(body.getGender().getId() == GenderEnum.MALE){
            filename = "van-gogh.jpg";
        }else if(body.getGender().getId() == GenderEnum.FEMALE){
            filename = "mona-lisa.jpg";
        }

        ClassPathResource resource = new ClassPathResource("mocks/" + filename);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Image asset not found");
        }

        byte[] data;
        try (InputStream is = resource.getInputStream()) {
            data = is.readAllBytes();
        }

        ByteArrayResource bodyResource = new ByteArrayResource(data);

        String contentType = java.net.URLConnection.guessContentTypeFromStream(new java.io.ByteArrayInputStream(data));
        if (contentType == null) contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
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

        GeneratedTextSources generatedTextSources = new GeneratedTextSources();
        generatedTextSources.setGeneratedText("This is a comparison between two stories. The first in "+arguments.getSpinArgumentsFirstStory().getCity()+", on "+arguments.getSpinArgumentsFirstStory().getYear()+" and it's about a "+arguments.getSpinArgumentsFirstStory().getGender().getDescription() +
                ". The second in "+arguments.getSpinArgumentsSecondStory().getCity()+", on "+arguments.getSpinArgumentsSecondStory().getYear()+" and it's about a "+arguments.getSpinArgumentsSecondStory().getGender().getDescription() +
                ". \nLorem ipsum dolor sit amet, consectetur adipiscing elit. Proin ut erat porttitor, condimentum purus id, lobortis ipsum. Nam efficitur malesuada urna, vitae porttitor augue faucibus vel. Phasellus vitae fringilla nibh, ut rhoncus diam. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Interdum et malesuada fames ac ante ipsum primis in faucibus. Mauris vestibulum in ex sed vehicula. Maecenas pulvinar massa ut bibendum posuere. Sed sit amet purus maximus, semper magna vel, finibus mauris. Praesent laoreet felis non cursus tempus. Donec feugiat semper consectetur. Praesent eget dui ornare, tempus nulla eget, bibendum magna.\n" +
                "\n" +
                "Interdum et malesuada fames ac ante ipsum primis in faucibus. Cras ligula ligula, luctus in magna quis, egestas iaculis mauris. Mauris lacinia hendrerit massa sed rutrum. Proin ut sem eget enim hendrerit euismod vel et elit. In iaculis eros mauris. In hac habitasse platea dictumst. Vestibulum et commodo sem. Cras mauris lacus, volutpat vel porttitor placerat, fringilla et libero. Curabitur volutpat magna sit amet risus consectetur commodo. Vivamus porta mollis libero a fermentum. Proin vestibulum vehicula tellus, porttitor pharetra lectus dictum eu. Curabitur ornare, urna non auctor congue, tellus nunc sollicitudin ante, eu faucibus nibh dolor nec urna. Vivamus eu lobortis arcu, vitae vestibulum massa. In pulvinar risus orci, sit amet luctus mi sollicitudin eget. Maecenas vel odio fermentum, blandit est in, dictum sem. Vivamus eu libero nunc.");
        List<String> sources = new ArrayList<>();
        sources.add("umea.se");
        sources.add("sweden.se");
        sources.add("malmo.se");
        sources.add("uppsala.se");
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
