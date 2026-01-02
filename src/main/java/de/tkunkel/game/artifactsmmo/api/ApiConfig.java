package de.tkunkel.game.artifactsmmo.api;

import de.tkunkel.game.artifactsmmo.Config;
import de.tkunkel.game.artifactsmmo.ServerDetailsApiWrapper;
import de.tkunkel.games.artifactsmmo.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfig {
    private final Config config;

    public ApiConfig(Config config) {
        this.config = config;
    }

    @Bean
    public ApiClient createApiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBearerToken(config.token());
        apiClient.setBasePath("https://api.artifactsmmo.com");
        return apiClient;
    }

    @Bean
    public CharactersApiWrapper charactersApiWrapper() {
        return new CharactersApiWrapper(createApiClient());
    }

    @Bean
    public MyCharactersApiWrapper myCharactersApiWrapper() {
        return new MyCharactersApiWrapper(createApiClient());
    }

    @Bean
    public MyAccountApiWrapper myAccountApiWrapper() {
        return new MyAccountApiWrapper(createApiClient());
    }

    @Bean
    public ServerDetailsApiWrapper serverDetailsApiWrapper() {
        return new ServerDetailsApiWrapper(createApiClient());
    }
}
