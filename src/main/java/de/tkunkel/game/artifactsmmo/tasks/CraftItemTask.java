package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.game.artifactsmmo.shopping.Wish;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CraftItemTask {
    private final Logger logger = LoggerFactory.getLogger(CraftItemTask.class.getName());
    private final CharactersApiWrapper charactersApiWrapper;
    private final MyAccountApiWrapper myAccountApiWrapper;
    private final Caches caches;
    private final ItemHelper itemHelper;
    private final CharHelper charHelper;
    private final MyCharactersApiWrapper myCharactersApi;

    public CraftItemTask(CharactersApiWrapper charactersApiWrapper, MyAccountApiWrapper myAccountApiWrapper, Caches caches, ItemHelper itemHelper, CharHelper charHelper, MyCharactersApiWrapper myCharactersApi) {
        this.charactersApiWrapper = charactersApiWrapper;
        this.myAccountApiWrapper = myAccountApiWrapper;
        this.caches = caches;
        this.itemHelper = itemHelper;
        this.charHelper = charHelper;
        this.myCharactersApi = myCharactersApi;
    }

    public void craftItem(CommonBrain brain, String characterName, String itemToCraft) {
        if (!hasResourcesInInventory(characterName, itemToCraft)) {
            logger.warn("Tried to craft {} but did not have all resources.", itemToCraft);
            return;
        }
        MapSchema map = itemHelper.findLocationToCraftItem(itemToCraft);
        charHelper.moveToLocationSync(characterName, map);
        charHelper.waitUntilCooldownDone(characterName);
        myCharactersApi.actionCraftingMyNameActionCraftingPost(characterName, new CraftingSchema().code(itemToCraft)
                                                                                                  .quantity(1)
        );
        charHelper.waitUntilCooldownDone(characterName);
    }

    public void craftItemForWish(String characterName, Wish wish) {
        boolean hasResourcesInInventory = hasResourcesInInventory(characterName, wish.itemCode);
        boolean hasResourcesInBank = hasResourcesInBank(wish.itemCode);
        if (!hasResourcesInInventory
                && !hasResourcesInBank
        ) {
            logger.error("Tried to craft {} but did not have all resources, this wish should not have been tried.", wish);
            wish.reservedBy = null;
            return;
        }
        MapSchema map = itemHelper.findLocationToCraftItem(wish.itemCode);
        charHelper.moveToLocationSync(characterName, map);
        charHelper.waitUntilCooldownDone(characterName);
        myCharactersApi.actionCraftingMyNameActionCraftingPost(characterName, new CraftingSchema().code(wish.itemCode)
                                                                                                  .quantity(1)
        );
        charHelper.waitUntilCooldownDone(characterName);
    }

    public boolean hasResourcesInBank(String itemToCraft) {
        Optional<ItemSchema> itemDefinition = caches.findItemDefinition(itemToCraft);
        if (itemDefinition.isEmpty()
                || itemDefinition.get()
                                 .getCraft() == null
                || itemDefinition.get()
                                 .getCraft()
                                 .getItems() == null
        ) {
            return true;
        }
        DataPageSimpleItemSchema bankItemsMyBankItemsGet = myAccountApiWrapper.getBankItemsMyBankItemsGet(null, 1, 100);
        return itemDefinition.get()
                             .getCraft()
                             .getItems()
                             .stream()
                             .allMatch(simpleItemSchema -> bankItemsMyBankItemsGet.getData()
                                                                                  .stream()
                                                                                  .anyMatch(inventorySlot -> inventorySlot.getCode()
                                                                                                                          .equalsIgnoreCase(simpleItemSchema.getCode())));
    }

    public boolean hasResourcesInInventory(String characterName, String itemToCraft) {
        CharacterResponseSchema character = charactersApiWrapper.getCharacterCharactersNameGet(characterName);
        Optional<ItemSchema> itemDefinition = caches.findItemDefinition(itemToCraft);
        if (itemDefinition.isEmpty()
                || itemDefinition.get()
                                 .getCraft() == null
                || itemDefinition.get()
                                 .getCraft()
                                 .getItems() == null
        ) {
            return true;
        }
        // noinspection DataFlowIssue
        return itemDefinition.get()
                             .getCraft()
                             .getItems()
                             .stream()
                             .allMatch(simpleItemSchema -> character.getData()
                                                                    .getInventory()
                                                                    .stream()
                                                                    .anyMatch(inventorySlot -> inventorySlot.getCode()
                                                                                                            .equalsIgnoreCase(simpleItemSchema.getCode())));
    }

}
