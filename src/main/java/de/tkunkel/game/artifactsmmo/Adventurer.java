package de.tkunkel.game.artifactsmmo;

import de.tkunkel.game.artifactsmmo.brains.Brain;
import de.tkunkel.game.artifactsmmo.brains.tier01.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Adventurer {
    private final AdventurerClass adventurerClass;
    Logger logger = LoggerFactory.getLogger(Adventurer.class.getName());

    private final String characterName;
    private final Set<Brain> brains;
    private Brain brain;

    public Adventurer(String characterName, AdventurerClass adventurerClass, Set<Brain> brains) {
        this.characterName = characterName;
        this.adventurerClass = adventurerClass;
        this.brains = brains;
        brain = decideNewBrain();
    }

    public void startLoop() {
        while (true) {
            logger.info("Adventurer {} of class {} is running", characterName, adventurerClass.name());
            try {
                brain.runBaseLoop(characterName);
            } catch (BrainCompletedException e) {
                logger.info("Adventurer {} of class {} needs new brain", characterName, adventurerClass.name());
                brain = decideNewBrain();
            }
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Brain decideNewBrain() {
        Optional<Brain> optionalBrain = switch (adventurerClass) {
            case MINER -> brains.stream()
                                .filter(brain -> brain instanceof MinerT1Brain)
                                .findFirst()
            ;
            case FIGHTER -> brains.stream()
                                  .filter(brain -> brain instanceof FighterT1Brain)
                                  .findFirst()
            ;
            case WOODWORKER -> brains.stream()
                                     .filter(brain -> brain instanceof WoodworkerT1Brain)
                                     .findFirst()
            ;
            case ALCHEMIST -> brains.stream()
                                    .filter(brain -> brain instanceof AlchemistT1Brain)
                                    .findFirst()
            ;
            case FISHER -> brains.stream()
                                 .filter(brain -> brain instanceof FisherT1Brain)
                                 .findFirst()
            ;
        };
        return optionalBrain.get();
    }
}
