package de.tkunkel.game.artifactsmmo;

import org.springframework.stereotype.Component;

@Component
public class Config {
    public static String API_TOKEN;

    public Config() {
        if (System.getenv("API_TOKEN") == null) {
            throw new RuntimeException("API_TOKEN not set");
        }
        Config.API_TOKEN = System.getenv("API_TOKEN");
    }
}
