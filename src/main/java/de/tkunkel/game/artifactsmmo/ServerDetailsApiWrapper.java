package de.tkunkel.game.artifactsmmo;

import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.api.ServerDetailsApi;
import de.tkunkel.games.artifactsmmo.model.StatusResponseSchema;

public class ServerDetailsApiWrapper {
    public final ServerDetailsApi serverDetailsApi;

    public ServerDetailsApiWrapper(ApiClient apiClient) {

        serverDetailsApi = new ServerDetailsApi(apiClient);
    }

    public StatusResponseSchema getServerDetailsGet() {
        try {
            return serverDetailsApi.getServerDetailsGet();
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
