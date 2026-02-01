package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.DataPageSimpleItemSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BankFetchItemsAndCraftTask {
    private final Logger logger = LoggerFactory.getLogger(BankFetchItemsAndCraftTask.class.getName());
    private final CraftItemTask craftItemTask;
    private final BankDepositSingleItemTask bankDepositSingleItemTask;
    private final BankFetchItemTask bankFetchItemTask;
    private final Caches caches;
    private final MyAccountApiWrapper myAccountApi;
    private final CharHelper charHelper;
    private final BankDepositAllTask bankDepositAllTask;
    private final ItemHelper itemHelper;
    private final CharactersApiWrapper charactersApi;

    public BankFetchItemsAndCraftTask(CraftItemTask craftItemTask, BankDepositSingleItemTask bankDepositSingleItemTask,
                                      BankFetchItemTask bankFetchItemTask, Caches caches, MyAccountApiWrapper myAccountApi, CharHelper charHelper, BankDepositAllTask bankDepositAllTask, ItemHelper itemHelper, CharactersApiWrapper charactersApi) {
        this.craftItemTask = craftItemTask;
        this.bankDepositSingleItemTask = bankDepositSingleItemTask;
        this.bankFetchItemTask = bankFetchItemTask;
        this.caches = caches;
        this.myAccountApi = myAccountApi;
        this.charHelper = charHelper;
        this.bankDepositAllTask = bankDepositAllTask;
        this.itemHelper = itemHelper;
        this.charactersApi = charactersApi;
    }

    public void craftItemWithBankItems(CharacterSchema character, String itemToCraft, int amount) {
        // get resources needed for item
        Optional<ItemSchema> optionalItemSchema = caches.cachedItems.stream()
                                                                    .filter(item -> item.getCode()
                                                                                        .equals(itemToCraft))
                                                                    .findFirst()
                ;
        if (optionalItemSchema.isEmpty()) {
            logger.warn("No item found for {}", itemToCraft);
            throw new RuntimeException("No item found for " + itemToCraft);
        }

        DataPageSimpleItemSchema bankItemsMyBankItemsGet = myAccountApi.getBankItemsMyBankItemsGet(null, 1, 100);

        List<SimpleItemSchema> neededItems = optionalItemSchema.get()
                                                               .getCraft()
                                                               .getItems()
                ;
        depositObsoleteItemsToBank(character.getName(), neededItems);
        // fetch resources from bank that are missing from inventory
        // TODO only fetch what is missing in inventory
        for (SimpleItemSchema neededItem : neededItems) {
            if (bankItemsMyBankItemsGet.getData()
                                       .stream()
                                       .noneMatch(simpleItemSchema ->
                                                          simpleItemSchema.getCode()
                                                                          .equalsIgnoreCase(neededItem.getCode())
                                                                  && simpleItemSchema.getQuantity() >= neededItem.getQuantity()
                                       )
            ) {
                return;
            }
            int cntInInventory;

            cntInInventory = charHelper.cntAllItemsInInventory(character.getName());
            if (character.getInventoryMaxItems() - cntInInventory >= neededItem.getQuantity() * amount) {
                bankFetchItemTask.fetchItemFromBank(character, neededItem.getCode(), neededItem.getQuantity() * amount);
                charHelper.waitUntilCooldownDone(character.getName());
                craftItemTask.craftItem(character.getName(), itemToCraft, amount);
                // deposit crafted item into bank
                bankDepositSingleItemTask.depositInventoryInBank(character.getName(), itemToCraft);
            } else {
                logger.error("Shall fetch more items than space in inventory!");
            }
        }
    }

    // make space in inventory
    private void depositObsoleteItemsToBank(String characterName,
                                            List<SimpleItemSchema> neededItems) {
        final CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                       .getData();
        charHelper.waitUntilCooldownDone(characterName);

        // remove items that are not needed in this amount
        character.getInventory()
                 .stream()
                 .filter(inventorySlot -> inventorySlot.getQuantity() > 0)
                 .filter(inventoryItem -> neededItems.stream()
                                                     .anyMatch(simpleItemSchema -> simpleItemSchema.getCode()
                                                                                                   .equalsIgnoreCase(inventoryItem.getCode())))
                 .forEach(inventoryItem -> {
                     Optional<SimpleItemSchema> needed = neededItems.stream()
                                                                    .filter(simpleItemSchema -> simpleItemSchema.getCode()
                                                                                                                .equalsIgnoreCase(inventoryItem.getCode()))
                                                                    .findFirst()
                             ;
                     int surplus = inventoryItem.getQuantity() - needed.get()
                                                                       .getQuantity();
                     if (surplus > 0) {
                         bankDepositSingleItemTask.depositInventoryInBank(characterName, inventoryItem.getCode(), surplus);
                     }
                 })
        ;

        // remove items that are not needed at all
        character.getInventory()
                 .stream()
                 .filter(inventorySlot -> inventorySlot.getQuantity() > 0)
                 .filter(itemSchema -> neededItems.stream()
                                                  .noneMatch(simpleItemSchema -> simpleItemSchema.getCode()
                                                                                                 .equalsIgnoreCase(itemSchema.getCode())))
                 .forEach(itemSchema -> {
                     bankDepositSingleItemTask.depositInventoryInBank(characterName, itemSchema.getCode());
                 })
        ;
    }

}
