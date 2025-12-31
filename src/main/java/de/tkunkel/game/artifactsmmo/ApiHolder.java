package de.tkunkel.game.artifactsmmo;

import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import org.springframework.stereotype.Service;

@Service
public class ApiHolder {
    public final MyCharactersApiWrapper myCharactersApi;
    public final CharactersApiWrapper charactersApi;
    public final ServerDetailsApiWrapper serverDetailsApi;
    public final MyAccountApiWrapper myAccountApi;

    public ApiHolder(MyCharactersApiWrapper myCharactersApi, CharactersApiWrapper charactersApi, ServerDetailsApiWrapper serverDetailsApi, MyAccountApiWrapper myAccountApi) {
        this.myCharactersApi = myCharactersApi;
        this.charactersApi = charactersApi;
        this.serverDetailsApi = serverDetailsApi;
        this.myAccountApi = myAccountApi;
    }
}
