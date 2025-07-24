package pt.isel.cn;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import java.io.IOException;

public class DatabaseUtil {

    private DatabaseUtil() {
    }

    static Firestore getDB(String dbName) {
        try {
            // Service account private key
            GoogleCredentials credentials =
                    GoogleCredentials.getApplicationDefault();
            // Firestore options, database name and credentials
            FirestoreOptions options = FirestoreOptions
                    .newBuilder().setDatabaseId(dbName).setCredentials(credentials)
                    .build();
            return options.getService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Firestore database", e);
        }
    }
}
