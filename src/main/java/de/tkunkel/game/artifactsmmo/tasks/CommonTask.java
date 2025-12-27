package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class CommonTask {
    private final Logger logger = LoggerFactory.getLogger(CommonTask.class.getName());

    protected final ApiHolder apiHolder;

    public CommonTask(ApiHolder apiHolder) {
        this.apiHolder = apiHolder;
    }

    public void fetchItemFromBank(CommonBrain brain, CharacterResponseSchema character, String neededItemCode, int quantity) {
        try {
            waitUntilCooldownIsOver(character.getData()
                                             .getName());
            brain.moveToLocation(character, brain.findClosestLocation(character, "bank")
                                                 .get()
            );
            waitUntilCooldownIsOver(character.getData()
                                             .getName());
            SimpleItemSchema simpleItemSchema = new SimpleItemSchema().code(neededItemCode)
                                                                      .quantity(quantity);
            brain.apiHolder.myCharactersApi.actionWithdrawBankItemMyNameActionBankWithdrawItemPost(neededItemCode, Collections.singletonList(simpleItemSchema)
            );
            waitUntilCooldownIsOver(character.getData()
                                             .getName());
        } catch (ApiException e) {
            logger.error("Error while withdrawing item from bank", e);
            throw new RuntimeException(e);
        }
    }


    public void waitUntilCooldownIsOver(String characterName) {
        // sleep until cooldown is over
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
            Thread.sleep(character.getData()
                                  .getCooldownExpiration()
                                  .toInstant()
                                  .toEpochMilli() - System.currentTimeMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

    }
}
