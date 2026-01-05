package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FarmHighestResourceTask {
    private final Logger logger = LoggerFactory.getLogger(FarmHighestResourceTask.class.getName());
    private final CharHelper charHelper;

    public FarmHighestResourceTask(CharHelper charHelper) {
        this.charHelper = charHelper;
    }

    public void farmResource(CommonBrain brain, String characterName) {
        String resourceToFarm = brain.decideWhatResourceToFarm(characterName);

        MapSchema whereToGather = brain.findLocationWhereToFarm(resourceToFarm);
        // logger.info("Farming {} at {}", resourceToFarm, whereToGather);
        CharacterResponseSchema character = null;
        character = brain.apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        charHelper.waitUntilCooldownDone(character);
        charHelper.moveToLocation(character, whereToGather);
        charHelper.waitUntilCooldownDone(character);
        brain.apiHolder.myCharactersApi.actionGatheringMyNameActionGatheringPost(character.getData()
                                                                                          .getName());
        charHelper.waitUntilCooldownDone(character);
    }
}
