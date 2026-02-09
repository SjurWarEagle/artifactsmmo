package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class FillStorageTask {
    private final Logger logger = LoggerFactory.getLogger(FillStorageTask.class.getName());

    private final CharHelper charHelper;
    private final FarmResourceTask farmResourceTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final HuntForItemTask huntForItemTask;
    private final ItemHelper itemHelper;
    private final Caches caches;
    private final CharactersApiWrapper charactersApi;

    public FillStorageTask(CharHelper charHelper, FarmResourceTask farmResourceTask, BankDepositAllTask bankDepositAllTask, HuntForItemTask huntForItemTask, ItemHelper itemHelper, Caches caches, CharactersApiWrapper charactersApi) {
        this.charHelper = charHelper;
        this.farmResourceTask = farmResourceTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.huntForItemTask = huntForItemTask;
        this.itemHelper = itemHelper;
        this.caches = caches;
        this.charactersApi = charactersApi;
    }

    public void fillStorage(String characterName) {
        ArrayList<String> wantedItems = new ArrayList();
        wantedItems.add("green_slimeball");
        wantedItems.add("feather");
        wantedItems.add("gudgeon");
        wantedItems.add("copper_ore");
        wantedItems.add("sunflower");
        wantedItems.add("red_slimeball");
        wantedItems.add("yellow_slimeball");
        wantedItems.add("cowhide");
        wantedItems.add("iron_ore");
        wantedItems.add("algae");
        wantedItems.add("nettle_leaf");
        wantedItems.add("trout");
        wantedItems.add("coal");
        wantedItems.add("birch_wood");
        wantedItems.add("spruce_wood");
        wantedItems.add("ash_wood");
        for (String itemCode : wantedItems) {
            int inBank = charHelper.cntItemsInBank(itemCode);
            int inInventory = charHelper.cntItemsInInventory(characterName, itemCode);
            int desiredAmount = 1000 - inInventory - inBank;
            if (desiredAmount <= 0) {
                continue;
            }

            if (canObtainResource(characterName, itemCode)) {
                while (desiredAmount > 0) {
                    inBank = charHelper.cntItemsInBank(itemCode);
                    inInventory = charHelper.cntItemsInInventory(characterName, itemCode);
                    desiredAmount = 1000 - inInventory - inBank;
                    obtainResource(characterName, itemCode, desiredAmount);
                    bankDepositAllTask.depositInventoryInBank(characterName);
                }

            } else {
                logger.warn("Not possible to get " + itemCode + " unknown how it can be obtained.");
            }
        }
    }

    private void obtainResource(String characterName, String itemCode, int desiredAmount) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        boolean isFarmable = charHelper.canFarmItem(character, itemCode);
        boolean isHuntable = charHelper.canHuntItem(character, itemCode);
        boolean isCraftable = charHelper.canCraftItemAndFarmParts(character, itemCode);
        if (isFarmable) {
            farmResourceTask.farmResource(characterName, itemCode, 50);
        } else if (isHuntable) {
            huntForItemTask.huntForItem(characterName, itemCode);
        } else if (isCraftable) {
            logger.info("Crafting " + itemCode);
        } else {
            logger.warn("Not possible to get " + itemCode + " unknown how it can be obtained.");
        }
    }

    private boolean canObtainResource(String characterName, String itemCode) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        boolean isFarmable = charHelper.canFarmItem(character, itemCode);
        boolean isHuntable = charHelper.canHuntItem(character, itemCode);
        boolean isCraftable = charHelper.canCraftItemAndFarmParts(character, itemCode);

        return isCraftable || isFarmable || isHuntable;
    }

    private boolean storageLow(String charName, String itemCode) {
        boolean storageLow = charHelper.cntItemsInBankAndInventory(charName, itemCode) < 100;
        return storageLow;

    }
}
