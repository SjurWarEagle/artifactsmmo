package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.brains.tier01.FisherT1Brain;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FarmHighestResourceTask {
    private final Logger logger = LoggerFactory.getLogger(FisherT1Brain.class.getName());
    private final CommonBrain brain;

    public FarmHighestResourceTask(CommonBrain brain) {
        this.brain = brain;
    }

    public void farmResource(String characterName) throws ApiException {
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
