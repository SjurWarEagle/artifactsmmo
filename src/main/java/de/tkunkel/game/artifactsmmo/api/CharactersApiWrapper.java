package de.tkunkel.game.artifactsmmo.api;

import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.api.CharactersApi;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Service
public class CharactersApiWrapper {
    private static final long CACHE_DURATION_MS = 200;
    private final Map<String, CharacterCacheEntry> characterCache = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(CharactersApiWrapper.class.getName());
    public final CharactersApi charactersApi;

    public CharactersApiWrapper(ApiClient apiClient) {
        charactersApi = new CharactersApi(apiClient);
    }

    public CharacterResponseSchema getCharacterCharactersNameGet(String name) {
        Instant now = Instant.now();
        CharacterCacheEntry entry = characterCache.get(name);

        // Return cached result if still valid (<100ms old)
        if (entry != null && now.minusMillis(CACHE_DURATION_MS)
                                .isBefore(entry.timestamp)) {
            return entry.response;
        }

        // Cache miss or expired - fetch fresh data
        try {
            CharacterResponseSchema response = charactersApi.getCharacterCharactersNameGet(name);
            characterCache.put(name, new CharacterCacheEntry(response, now));
            return response;
        } catch (ApiException e) {
            logger.error("Error getting character characters name", e);
            throw new RuntimeException(e);
        }
    }

    private static class CharacterCacheEntry {
        final CharacterResponseSchema response;
        final Instant timestamp;

        CharacterCacheEntry(CharacterResponseSchema response, Instant timestamp) {
            this.response = response;
            this.timestamp = timestamp;
        }
    }
}
