package se.spin.prototype.Controllers;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.spin.prototype.Beans.CompareScenariosRequest;
import se.spin.prototype.Beans.GenderEnum;
import se.spin.prototype.Beans.GeneratedTextSources;
import se.spin.prototype.Beans.SpinArguments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/spin")
public class SpinController {

    @PostMapping("/story")
    public GeneratedTextSources postGeneratedText(@RequestBody SpinArguments arguments) {
        GeneratedTextSources generatedTextSources = new GeneratedTextSources();
        generatedTextSources.setGeneratedText("This is a story in "+arguments.getCity()+", on "+arguments.getYear()+" and it's about a "+arguments.getGender().getDescription() +
                "\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Proin ut erat porttitor, condimentum purus id, lobortis ipsum. Nam efficitur malesuada urna, vitae porttitor augue faucibus vel. Phasellus vitae fringilla nibh, ut rhoncus diam. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Interdum et malesuada fames ac ante ipsum primis in faucibus. Mauris vestibulum in ex sed vehicula. Maecenas pulvinar massa ut bibendum posuere. Sed sit amet purus maximus, semper magna vel, finibus mauris. Praesent laoreet felis non cursus tempus. Donec feugiat semper consectetur. Praesent eget dui ornare, tempus nulla eget, bibendum magna.\n" +
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

    @PostMapping("/image")
    public ResponseEntity<ByteArrayResource> postGeneratedImage(@RequestBody SpinArguments body) throws IOException {
        Path filePath = Path.of("mocks", "paula-rego.jpg");
        if(body.getGender().getId() == GenderEnum.MALE){
            filePath = Path.of("mocks", "van-gogh.jpg");
        }else if(body.getGender().getId() == GenderEnum.FEMALE){
            filePath = Path.of("mocks", "mona-lisa.jpg");
        }
        byte[] data = Files.readAllBytes(filePath);

        ByteArrayResource resource = new ByteArrayResource(data);

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION)
                .body(resource);
    }

    @PostMapping("/compare-scenarios")
    public GeneratedTextSources postCompareScenarios(@RequestBody CompareScenariosRequest arguments) {
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

}
