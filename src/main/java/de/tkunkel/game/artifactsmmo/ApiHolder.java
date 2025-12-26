package de.tkunkel.game.artifactsmmo;

import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.api.CharactersApi;
import de.tkunkel.games.artifactsmmo.api.MyAccountApi;
import de.tkunkel.games.artifactsmmo.api.MyCharactersApi;
import de.tkunkel.games.artifactsmmo.api.ServerDetailsApi;
import org.springframework.stereotype.Service;

@Service
public class ApiHolder {
    public final MyCharactersApi myCharactersApi;
    public final CharactersApi charactersApi;
    public final ServerDetailsApi serverDetailsApi;
    public final MyAccountApi myAccountApi;

    private ApiClient createApiClient() {
        ApiClient rc = new ApiClient();
        rc.setBearerToken(Config.API_TOKEN);
        rc.setBasePath("https://api.artifactsmmo.com");
        return rc;
    }

    public ApiHolder() {
        myCharactersApi = new MyCharactersApi(createApiClient());
        charactersApi = new CharactersApi(createApiClient());
        serverDetailsApi = new ServerDetailsApi(createApiClient());
        myAccountApi = new MyAccountApi(createApiClient());
    }
}
