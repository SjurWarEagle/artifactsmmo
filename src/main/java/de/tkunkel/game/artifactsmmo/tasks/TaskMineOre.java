package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import org.springframework.stereotype.Service;

@Service
public class TaskMineOre extends CommonTask {

    public TaskMineOre(ApiHolder apiHolder) {
        super(apiHolder);
    }

    public void perform(String characterName, String itemToGather) {
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
            waitUntilCooldownIsOver(characterName);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

    }

}
