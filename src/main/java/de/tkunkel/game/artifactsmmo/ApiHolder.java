package de.tkunkel.game.artifactsmmo;

import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.games.artifactsmmo.ApiClient;
import org.springframework.stereotype.Service;

@Service
public class ApiHolder {
    public final MyCharactersApiWrapper myCharactersApi;
    public final CharactersApiWrapper charactersApi;
    public final ServerDetailsApiWrapper serverDetailsApi;
    public final MyAccountApiWrapper myAccountApi;

    private ApiClient createApiClient() {
        ApiClient rc = new ApiClient();
        rc.setBearerToken(Config.API_TOKEN);
        rc.setBasePath("https://api.artifactsmmo.com");
        return rc;
    }

    public ApiHolder() {
        myCharactersApi = new MyCharactersApiWrapper(createApiClient());
        charactersApi = new CharactersApiWrapper(createApiClient());
        serverDetailsApi = new ServerDetailsApiWrapper(createApiClient());
        myAccountApi = new MyAccountApiWrapper(createApiClient());
    }
}
