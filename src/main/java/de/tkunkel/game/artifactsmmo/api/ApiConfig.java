package de.tkunkel.game.artifactsmmo.api;

import de.tkunkel.game.artifactsmmo.Config;
import de.tkunkel.game.artifactsmmo.ServerDetailsApiWrapper;
import de.tkunkel.games.artifactsmmo.ApiClient;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.time.Duration;

@Configuration
public class ApiConfig {
    private final Config config;

    public ApiConfig(Config config) {
        this.config = config;
    }

    @Bean
    public ApiClient createApiClient() {
        ApiClient apiClient = new ApiClient();
        // apiClient.setDebugging(true);
        apiClient.setBearerToken(config.token());
        apiClient.setBasePath("https://api.artifactsmmo.com");
        configureTimeouts(apiClient);
        return apiClient;
    }

    private static void configureTimeouts(ApiClient apiClient) {
        OkHttpClient httpClient = null;
        Method setter = null;

        for (Method method : apiClient.getClass()
                                      .getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType()
                                                         .equals(OkHttpClient.class)) {
                try {
                    httpClient = (OkHttpClient) method.invoke(apiClient);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(OkHttpClient.class)) {
                setter = method;
            }
        }

        if (httpClient == null || setter == null) {
            throw new IllegalStateException("Unable to configure OkHttp timeouts for ApiClient");
        }

        OkHttpClient tunedClient = httpClient.newBuilder()
                                             .connectTimeout(Duration.ofSeconds(300))
                                             .readTimeout(Duration.ofSeconds(300))
                                             .writeTimeout(Duration.ofSeconds(3000))
                                             .callTimeout(Duration.ofSeconds(300))
                                             .build()
                ;

        try {
            setter.invoke(apiClient, tunedClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
