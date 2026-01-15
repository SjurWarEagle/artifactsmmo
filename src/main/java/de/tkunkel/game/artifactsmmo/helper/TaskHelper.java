package de.tkunkel.game.artifactsmmo.helper;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.shopping.Wish;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import de.tkunkel.game.artifactsmmo.tasks.BankFetchItemTask;
import de.tkunkel.game.artifactsmmo.tasks.HuntMonsterTask;
import de.tkunkel.game.artifactsmmo.tasks.TaskAcceptNewItemTask;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.InventorySlot;
import org.springframework.stereotype.Service;

@Service
public class TaskHelper {
    private final HuntMonsterTask huntMonsterTask;
    private final CharHelper charHelper;
    private final WishList wishList;
    private final BankFetchItemTask bankFetchItemTask;
    private final TaskAcceptNewItemTask taskAcceptNewItemTask;
    private final MyCharactersApiWrapper myCharactersApi;
    private final CharactersApiWrapper charactersApi;
    private final BankDepositAllTask bankDepositAllTask;

    public TaskHelper(HuntMonsterTask huntMonsterTask, CharHelper charHelper, WishList wishList,
                      BankFetchItemTask bankFetchItemTask, TaskAcceptNewItemTask taskAcceptNewItemTask,
                      MyCharactersApiWrapper myCharactersApi, CharactersApiWrapper charactersApi, BankDepositAllTask bankDepositAllTask) {
        this.huntMonsterTask = huntMonsterTask;
        this.charHelper = charHelper;
        this.wishList = wishList;
        this.bankFetchItemTask = bankFetchItemTask;
        this.taskAcceptNewItemTask = taskAcceptNewItemTask;
        this.myCharactersApi = myCharactersApi;
        this.charactersApi = charactersApi;
        this.bankDepositAllTask = bankDepositAllTask;
    }

    /**
     * has task and makes progress in task
     *
     * @param character
     * @return
     */
    public boolean isHandlingTask(CharacterSchema character) {
        character = charactersApi.getCharacterCharactersNameGet(character.getName())
                                 .getData();
        if ("".equalsIgnoreCase(character.getTask())) {
            return false;
        }
        boolean isMonsterTask = "monsters".equalsIgnoreCase(character.getTaskType());
        boolean isItemTask = "items".equalsIgnoreCase(character.getTaskType());
        if (isMonsterTask) {
            huntMonsterTask.hunt(character.getName(), character.getTask());
        } else if (isItemTask) {
            character = charactersApi.getCharacterCharactersNameGet(character.getName())
                                     .getData();
            // TODO check if it can be fulfilled
            int cntItemsInBank = charHelper.cntItemsInBank(character.getTask());
            int needed = character.getTaskTotal() - character.getTaskProgress();
            int missing = needed - cntItemsInBank;
            // cannot e fulfilled right now, so request what is missing
            if (missing > 0) {
                Wish wish = new Wish(character.getName(), character.getTask(), missing);
                wishList.addRequest(wish, true);
            } else {
                int freeInventory = character.getInventoryMaxItems() - character.getInventory()
                                                                                .stream()
                                                                                .mapToInt(InventorySlot::getQuantity)
                                                                                .sum();
                bankFetchItemTask.fetchItemFromBank(character, character.getTask(), Math.min(needed, freeInventory));
                int inInventory = charHelper.cntItemsInInventory(character.getName(), character.getTask());
                if (inInventory > 0) {
                    // give them to task giver
                    taskAcceptNewItemTask.giveItemsToTaskMaster(character);
                    charHelper.waitUntilCooldownDone(character.getName());
                    character = charactersApi.getCharacterCharactersNameGet(character.getName())
                                             .getData();
                    if (character.getTaskProgress() >= character.getTaskTotal()) {
                        myCharactersApi.actionCompleteTaskMyNameActionTaskCompletePost(character.getName());
                        charHelper.waitUntilCooldownDone(character.getName());
                        bankDepositAllTask.depositInventoryInBank(character.getName());
                        charHelper.waitUntilCooldownDone(character.getName());
                    }
                }
            }
        } else {
            throw new RuntimeException("unknown task type: " + character.getTaskType());
        }
        return false;
    }

}
