package com.shinhan.eclipse.service.app.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.secret-name:}")
    private String secretName;

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        FirebaseApp app;
        if (FirebaseApp.getApps().isEmpty()) {
            try (InputStream stream = resolveCredentials()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();
                app = FirebaseApp.initializeApp(options);
            }
        } else {
            app = FirebaseApp.getInstance();
        }
        return FirebaseMessaging.getInstance(app);
    }

    private InputStream resolveCredentials() throws IOException {
        if (secretName != null && !secretName.isBlank()) {
            try (SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(Region.AP_NORTHEAST_2)
                    .build()) {
                String json = client.getSecretValue(
                        GetSecretValueRequest.builder().secretId(secretName).build()
                ).secretString();
                return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            }
        }
        return new FileInputStream(credentialsPath);
    }
}
