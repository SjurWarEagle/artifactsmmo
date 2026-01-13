package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FarmHighestResourceTask {
    private final Logger logger = LoggerFactory.getLogger(FarmHighestResourceTask.class.getName());
    private final CharHelper charHelper;
    private final ItemHelper itemHelper;

    public FarmHighestResourceTask(CharHelper charHelper, ItemHelper itemHelper) {
        this.charHelper = charHelper;
        this.itemHelper = itemHelper;
    }

    public void farmResource(CommonBrain brain, String characterName) {
        String resourceToFarm = brain.decideWhatResourceToFarm(characterName);

        CharacterResponseSchema character = brain.apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        MapSchema whereToGather = itemHelper.findLocationWhereToFarm(character.getData(), resourceToFarm)
                                            .get();
        // logger.info("Farming {} at {}", resourceToFarm, whereToGather);
        charHelper.waitUntilCooldownDone(character);
        charHelper.moveToLocationSync(character.getData(), whereToGather);
        charHelper.waitUntilCooldownDone(character);
        brain.apiHolder.myCharactersApi.actionGatheringMyNameActionGatheringPost(character.getData()
                                                                                          .getName());
        charHelper.waitUntilCooldownDone(character);
    }
}
