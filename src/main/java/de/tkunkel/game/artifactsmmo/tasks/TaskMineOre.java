package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import org.springframework.stereotype.Service;

@Service
public class TaskMineOre extends CommonTask {

    public TaskMineOre(ApiHolder apiHolder, CharHelper charHelper) {
        super(apiHolder, charHelper);
    }

    public void perform(String characterName, String itemToGather) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        waitUntilCooldownDone(characterName);
    }

}
