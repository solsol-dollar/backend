package com.shinhan.eclipse.service.app.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    private final String credentialsPath;

    public FirebaseConfig(@Value("${firebase.credentials-path}") String credentialsPath) {
        this.credentialsPath = credentialsPath;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        FirebaseApp app;
        if (FirebaseApp.getApps().isEmpty()) {
            try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                app = FirebaseApp.initializeApp(options);
            }
        } else {
            app = FirebaseApp.getInstance();
        }
        return FirebaseMessaging.getInstance(app);
    }
}
