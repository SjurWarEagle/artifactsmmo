package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FarmResourceTask {
    private final Logger logger = LoggerFactory.getLogger(FarmResourceTask.class.getName());
    private final CharHelper charHelper;
    private final ItemHelper itemHelper;
    private final CharactersApiWrapper charactersApi;
    private final MyCharactersApiWrapper myCharactersApi;

    public FarmResourceTask(CharHelper charHelper, ItemHelper itemHelper, CharactersApiWrapper charactersApi, MyCharactersApiWrapper myCharactersApi) {
        this.charHelper = charHelper;
        this.itemHelper = itemHelper;
        this.charactersApi = charactersApi;
        this.myCharactersApi = myCharactersApi;
    }

    public void farmResource(String characterName, String resourceToFarm) {

        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        MapSchema whereToGather = itemHelper.findLocationWhereToFarm(character.getData(), resourceToFarm);
        // logger.info("Farming {} at {}", resourceToFarm, whereToGather);
        charHelper.waitUntilCooldownDone(character);
        charHelper.moveToLocationSync(character.getData(), whereToGather);
        charHelper.waitUntilCooldownDone(character);
        myCharactersApi.actionGatheringMyNameActionGatheringPost(character.getData()
                                                                          .getName());
        charHelper.waitUntilCooldownDone(character);
    }
}
