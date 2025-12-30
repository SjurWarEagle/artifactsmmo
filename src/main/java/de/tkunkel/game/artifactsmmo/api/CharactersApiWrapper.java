package de.tkunkel.game.artifactsmmo.api;

import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.api.CharactersApi;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharactersApiWrapper {
    private final Logger logger = LoggerFactory.getLogger(CharactersApiWrapper.class.getName());
    public final CharactersApi charactersApi;

    public CharactersApiWrapper(ApiClient apiClient) {
        charactersApi = new CharactersApi(apiClient);
    }

    // TODO add short, maybe 1-2sec cache
    public CharacterResponseSchema getCharacterCharactersNameGet(String name) {
        try {
            return charactersApi.getCharacterCharactersNameGet(name);
        } catch (ApiException e) {
            logger.error("getCharacterCharactersNameGet", e);
            throw new RuntimeException(e);
        }
    }
}
