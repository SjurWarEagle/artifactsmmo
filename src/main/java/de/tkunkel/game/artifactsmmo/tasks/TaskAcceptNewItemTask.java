package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.AccountHelper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskAcceptNewItemTask {

    private final CharHelper charHelper;
    private final MapHelper mapHelper;
    private final MyCharactersApiWrapper myCharactersApi;
    private final AccountHelper accountHelper;

    public TaskAcceptNewItemTask(CharHelper charHelper, MapHelper mapHelper, MyCharactersApiWrapper myCharactersApi, AccountHelper accountHelper) {
        this.charHelper = charHelper;
        this.mapHelper = mapHelper;
        this.myCharactersApi = myCharactersApi;
        this.accountHelper = accountHelper;
    }

    public void giveItemsToTaskMaster(CharacterSchema character) {
        if (moveToTaskMaster(character)) {
            return;
        }
        if ("".equalsIgnoreCase(character.getTask())
                || character.getTaskTotal() <= character.getTaskProgress()) {
            return;
        }
        int amount = character.getTaskTotal() - character.getTaskProgress();
        int inInventory = charHelper.cntItemsInInventory(character.getName(), character.getTask());
        amount = Math.min(amount, inInventory);
        SimpleItemSchema tradeRequest = new SimpleItemSchema().code(character.getTask())
                                                              .quantity(amount);
        myCharactersApi.actionTaskTradeMyNameActionTaskTradePost(character.getName(), tradeRequest);

    }

    public void getNewTaskIfCurrentTaskIsDone(CharacterSchema character) {
        if (!"".equalsIgnoreCase(character.getTask())) {
            // still has task
            return;
        }
        if (charHelper.cntItemsInBank("tasks_coin") >= 50
                && accountHelper.hasAchievement("tasks_farmer")) {
            // enough token in bank
            return;
        }

        if (moveToTaskMaster(character)) {
            return;
        }

        myCharactersApi.actionAcceptNewTaskMyNameActionTaskNewPost(character.getName());
        charHelper.waitUntilCooldownDone(character.getName());
    }

    private boolean moveToTaskMaster(CharacterSchema character) {
        Optional<MapSchema> closestLocation = mapHelper.findClosestLocation(character, "items");
        if (closestLocation.isEmpty()) {
            return true;
        }
        charHelper.moveToLocationSync(character, closestLocation.get());
        charHelper.waitUntilCooldownDone(character.getName());
        return false;
    }


}
