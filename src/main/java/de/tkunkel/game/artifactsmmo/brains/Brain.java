package de.tkunkel.game.artifactsmmo.brains;

import de.tkunkel.game.artifactsmmo.BrainCompletedException;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;

public interface Brain {
    void waitUntilCooldownDone(CharacterResponseSchema character);

    void runBaseLoop(String characterName) throws BrainCompletedException;

    boolean shouldBeUsed(String characterName);

    String decideWhatResourceToFarm(String characterName);
}
