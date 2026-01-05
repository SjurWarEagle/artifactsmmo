package de.tkunkel.game.artifactsmmo.brains;

import de.tkunkel.game.artifactsmmo.BrainCompletedException;

public interface Brain {
    void runBaseLoop(String characterName) throws BrainCompletedException;

    String decideWhatResourceToFarm(String characterName);
}
