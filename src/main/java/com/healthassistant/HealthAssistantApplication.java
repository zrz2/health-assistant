package com.healthassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync
public class HealthAssistantApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(HealthAssistantApplication.class, args);
    }

    private static void loadDotenv() {
        Path envFile = Paths.get(System.getProperty("user.dir"), ".env");
        if (!Files.exists(envFile)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(envFile)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    System.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
        }
    }
}
