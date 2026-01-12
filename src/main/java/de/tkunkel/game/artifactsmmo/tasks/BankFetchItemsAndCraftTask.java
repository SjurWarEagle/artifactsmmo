package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
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

    public BankFetchItemsAndCraftTask(CraftItemTask craftItemTask, BankDepositSingleItemTask bankDepositSingleItemTask,
                                      BankFetchItemTask bankFetchItemTask, Caches caches, MyAccountApiWrapper myAccountApi) {
        this.craftItemTask = craftItemTask;
        this.bankDepositSingleItemTask = bankDepositSingleItemTask;
        this.bankFetchItemTask = bankFetchItemTask;
        this.caches = caches;
        this.myAccountApi = myAccountApi;
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
        // is item already in the bank?
        DataPageSimpleItemSchema bankItemsMyBankItemsGet = myAccountApi.getBankItemsMyBankItemsGet(null, 1, 100);
        Optional<SimpleItemSchema> itemInBank = bankItemsMyBankItemsGet.getData()
                                                                       .stream()
                                                                       .filter(item -> itemToCraft.equals(item.getCode()))
                                                                       .filter(item -> item.getQuantity() >= amount)
                                                                       .filter(item -> item.getQuantity() >= 1)
                                                                       .findFirst()
                ;
        if (itemInBank.isPresent()) {
            // item already exists, no need to build it again
            return;
        }
        List<SimpleItemSchema> neededItems = optionalItemSchema.get()
                                                               .getCraft()
                                                               .getItems()
                ;
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
            bankFetchItemTask.fetchItemFromBank(character, neededItem.getCode(), neededItem.getQuantity() * amount);
        }

        // todo after fetching check again if item can be crafted

        // craft item
        craftItemTask.craftItem(character.getName(), itemToCraft, amount
        );

        // deposit crafted item into bank
        bankDepositSingleItemTask.depositInventoryInBank(character.getName(), itemToCraft
        );
    }

}
