package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class CommonTask {
    private final Logger logger = LoggerFactory.getLogger(CommonTask.class.getName());

    protected final ApiHolder apiHolder;

    public CommonTask(ApiHolder apiHolder) {
        this.apiHolder = apiHolder;
    }

    public void waitUntilCooldownDone(String characterName) {
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
            waitUntilCooldownDone(character);
        } catch (ApiException e) {
            logger.error("Error waiting for cooldown", e);
            throw new RuntimeException(e);
        }
    }

    public void fetchItemFromBank(CommonBrain brain, CharacterResponseSchema character, String neededItemCode, int quantity) {
        try {
            waitUntilCooldownDone(character.getData()
                                           .getName());
            SimpleItemSchema simpleItemSchema = new SimpleItemSchema().code(neededItemCode)
                                                                      .quantity(quantity);
            brain.moveToLocation(character, brain.findClosestLocation(character, "bank")
                                                 .get()
            );
            waitUntilCooldownDone(character.getData()
                                           .getName());
            brain.apiHolder.myCharactersApi.actionWithdrawBankItemMyNameActionBankWithdrawItemPost(character.getData()
                                                                                                            .getName(), Collections.singletonList(simpleItemSchema)
            );
            waitUntilCooldownDone(character.getData()
                                           .getName());
        } catch (ApiException e) {
            logger.error("Error while withdrawing item from bank", e);
            throw new RuntimeException(e);
        }
    }

    public void waitUntilCooldownDone(CharacterResponseSchema character) {
        OffsetDateTime serverTime;
        try {
            serverTime = apiHolder.serverDetailsApi.getServerDetailsGet()
                                                   .getData()
                                                   .getServerTime();
            character = apiHolder.charactersApi.getCharacterCharactersNameGet(character.getData()
                                                                                       .getName());
            long timeToWait = character.getData()
                                       .getCooldownExpiration()
                                       .toEpochSecond() - serverTime.toEpochSecond();
            if (timeToWait > 0) {
                logger.debug("Server time: {}", serverTime);
                logger.debug("Character cooldown expiration: {}", character.getData()
                                                                           .getCooldownExpiration()
                );
                logger.info("Waiting for cooldown: {} seconds", timeToWait);
                Thread.sleep(timeToWait + 1);
            }
        } catch (ApiException e) {
            logger.error("Error waiting for cooldown", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            logger.error("Error waiting for cooldown", e);
            throw new RuntimeException(e);
        }
        long secondsToWait = (character.getData()
                                       .getCooldownExpiration()
                                       .toEpochSecond()) - serverTime.toEpochSecond();
        if (secondsToWait > 0) {
            // has active cooldown
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(secondsToWait + 1));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
