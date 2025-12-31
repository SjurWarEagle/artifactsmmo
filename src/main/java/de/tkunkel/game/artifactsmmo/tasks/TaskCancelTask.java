package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.DataPageSimpleItemSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskCancelTask extends CommonTask {
    private final Logger logger = LoggerFactory.getLogger(TaskCancelTask.class.getName());

    private final MyAccountApiWrapper myAccountApiWrapper;
    private final MyCharactersApiWrapper myCharactersApiWrapper;

    public TaskCancelTask(ApiHolder apiHolder, MyAccountApiWrapper myAccountApiWrapper, MyCharactersApiWrapper myCharactersApiWrapper) {
        super(apiHolder);
        this.myAccountApiWrapper = myAccountApiWrapper;
        this.myCharactersApiWrapper = myCharactersApiWrapper;
    }

    public void perform(CommonBrain brain, String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);

        DataPageSimpleItemSchema bankItemsMyBankItemsGet = myAccountApiWrapper.getBankItemsMyBankItemsGet(null, 1, 100);
        Optional<SimpleItemSchema> tasksCoin = bankItemsMyBankItemsGet.getData()
                                                                      .stream()
                                                                      .filter(itemSchema -> itemSchema.getCode()
                                                                                                      .equals("tasks_coin"))
                                                                      .findFirst()
                ;
        if (tasksCoin.isEmpty()
                || tasksCoin.get()
                            .getQuantity() < 1
        ) {
            logger.warn("Cannot cancel task, because not enough tasks_coin in bank");
            return;
        }
        waitUntilCooldownDone(characterName);
        Optional<MapSchema> taskMaster = brain.findClosesTaskMaster(character, "monsters");
        if (taskMaster.isEmpty()) {
            logger.error("Cannot cancel task, because no task master found");
            return;

        }
        fetchItemFromBank(brain, character, "tasks_coin", 1);
        waitUntilCooldownDone(characterName);
        character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);

        brain.moveToLocation(character, taskMaster.get());
        waitUntilCooldownDone(characterName);
        myCharactersApiWrapper.actionTaskCancelMyNameActionTaskCancelPost(characterName);
        waitUntilCooldownDone(characterName);

    }

}
