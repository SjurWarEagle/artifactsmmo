package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommonTask {
    private final Logger logger = LoggerFactory.getLogger(CommonTask.class.getName());

    protected final ApiHolder apiHolder;
    protected final CharHelper charHelper;
    protected final MapHelper mapHelper;
    private final MyCharactersApiWrapper myCharactersApi;

    public CommonTask(ApiHolder apiHolder, CharHelper charHelper, MapHelper mapHelper, MyCharactersApiWrapper myCharactersApi) {
        this.apiHolder = apiHolder;
        this.charHelper = charHelper;
        this.mapHelper = mapHelper;
        this.myCharactersApi = myCharactersApi;
    }

    public void waitUntilCooldownDone(String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        waitUntilCooldownDone(character);
    }

    public void fetchItemsFromBank(CharacterSchema character, List<SimpleItemSchema> items) {
        waitUntilCooldownDone(character.getName());
        charHelper.moveToLocationSync(character, mapHelper.findClosestLocation(character, "bank")
                                                          .get()
        );
        waitUntilCooldownDone(character.getName());

        myCharactersApi.actionWithdrawBankItemMyNameActionBankWithdrawItemPost(character.getName(), items
        );
        waitUntilCooldownDone(character.getName());
    }

    public void fetchItemFromBank(CharacterSchema character, String neededItemCode, int quantity) {
        waitUntilCooldownDone(character.getName());
        charHelper.moveToLocationSync(character, mapHelper.findClosestLocation(character, "bank")
                                                          .get()
        );
        waitUntilCooldownDone(character.getName());

        SimpleItemSchema simpleItemSchema = new SimpleItemSchema().code(neededItemCode)
                                                                  .quantity(quantity);
        myCharactersApi.actionWithdrawBankItemMyNameActionBankWithdrawItemPost(character.getName(), Collections.singletonList(simpleItemSchema)
        );
        waitUntilCooldownDone(character.getName());
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
