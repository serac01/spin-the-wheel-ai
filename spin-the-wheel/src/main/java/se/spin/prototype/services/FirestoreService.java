package se.spin.prototype.services;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import se.spin.prototype.Beans.Gender;
import se.spin.prototype.util.EnvUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
public class FirestoreService {
    private static final Logger log = LoggerFactory.getLogger(FirestoreService.class);
    private static final String COLLECTION_NAME = "stories";

    private final Firestore firestore;

    public FirestoreService() {
        this.firestore = initFirestore();
    }

    public Optional<String> fetchSeedText(String city, Integer year, Gender gender) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("city", city)
                    .whereEqualTo("year", year)
                    .whereEqualTo("gender", gender != null ? gender.getId() : null)
                    .limit(1);

            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot snapshot = future.get();
            if (snapshot.isEmpty()) {
                return Optional.empty();
            }
            QueryDocumentSnapshot doc = snapshot.getDocuments().getFirst();
            Object text = doc.get("text");
            return text != null ? Optional.of(text.toString()) : Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Interrupted while querying Firestore", e);
        } catch (ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to query Firestore", e);
        }
    }

    private Firestore initFirestore() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions.Builder builder = FirebaseOptions.builder();

                String credentialsPath = EnvUtil.get("GOOGLE_APPLICATION_CREDENTIALS");
                if (credentialsPath != null && !credentialsPath.isBlank()) {
                    builder.setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)));
                } else {
                    builder.setCredentials(GoogleCredentials.getApplicationDefault());
                }

                String projectId = EnvUtil.get("FIREBASE_PROJECT_ID");
                if (projectId != null && !projectId.isBlank()) {
                    builder.setProjectId(projectId);
                }

                FirebaseOptions options = builder.build();
                FirebaseApp.initializeApp(options);
            }
            return FirestoreClient.getFirestore();
        } catch (IOException e) {
            log.error("Failed to initialize Firestore", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not initialize Firestore", e);
        }
    }
}
