package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FarmHighestResourceTask {
    private final Logger logger = LoggerFactory.getLogger(FarmHighestResourceTask.class.getName());

    public void farmResource(CommonBrain brain, String characterName) throws ApiException {
        String resourceToFarm = brain.decideWhatResourceToFarm(characterName);

        MapSchema whereToGather = brain.findLocationWhereToFarm(resourceToFarm);
        // logger.info("Farming {} at {}", resourceToFarm, whereToGather);
        CharacterResponseSchema character = null;
        character = brain.apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        brain.waitUntilCooldownDone(character);
        brain.moveToLocation(character, whereToGather);
        brain.waitUntilCooldownDone(character);
        brain.apiHolder.myCharactersApi.actionGatheringMyNameActionGatheringPost(character.getData()
                                                                                          .getName());
        brain.waitUntilCooldownDone(character);
    }
}
