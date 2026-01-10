package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.DataPageSimpleItemSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskCancelTask {
    private final Logger logger = LoggerFactory.getLogger(TaskCancelTask.class.getName());

    private final MyAccountApiWrapper myAccountApiWrapper;
    private final MyCharactersApiWrapper myCharactersApiWrapper;
    private final CharactersApiWrapper charactersApi;
    private final CharHelper charHelper;
    private final BankFetchItemTask bankFetchItemTask;
    private final MapHelper mapHelper;

    public TaskCancelTask(ApiHolder apiHolder, MyAccountApiWrapper myAccountApiWrapper, MyCharactersApiWrapper myCharactersApiWrapper,
                          CharHelper charHelper, MapHelper mapHelper, CharactersApiWrapper charactersApi, CharHelper charHelper1, BankFetchItemTask bankFetchItemTask, MapHelper mapHelper1) {
        this.myAccountApiWrapper = myAccountApiWrapper;
        this.myCharactersApiWrapper = myCharactersApiWrapper;
        this.charactersApi = charactersApi;
        this.charHelper = charHelper1;
        this.bankFetchItemTask = bankFetchItemTask;
        this.mapHelper = mapHelper1;
    }

    public void perform(String characterName) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);

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
        charHelper.waitUntilCooldownDone(characterName);
        Optional<MapSchema> taskMaster = mapHelper.findClosesTaskMaster(character, "monsters");
        if (taskMaster.isEmpty()) {
            logger.error("Cannot cancel task, because no task master found");
            return;

        }
        bankFetchItemTask.fetchItemFromBank(character.getData(), "tasks_coin", 1);
        charHelper.waitUntilCooldownDone(characterName);
        character = charactersApi.getCharacterCharactersNameGet(characterName);

        charHelper.moveToLocationSync(character.getData(), taskMaster.get());
        charHelper.waitUntilCooldownDone(characterName);
        myCharactersApiWrapper.actionTaskCancelMyNameActionTaskCancelPost(characterName);
        charHelper.waitUntilCooldownDone(characterName);

    }

}
