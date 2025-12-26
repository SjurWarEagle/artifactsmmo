package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;

public class CommonTask {
    protected final ApiHolder apiHolder;

    public CommonTask(ApiHolder apiHolder) {
        this.apiHolder = apiHolder;
    }

    public void waitUntilCooldownIsOver(String characterName) {
        //sleep until cooldown is over
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
