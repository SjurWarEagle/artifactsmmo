package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.DataPageSimpleItemSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BankFetchItemsAndCraftTask extends CommonTask {
    private final Logger logger = LoggerFactory.getLogger(BankFetchItemsAndCraftTask.class.getName());
    private final CraftItemTask craftItemTask;
    private final BankDepositSingleItemTask bankDepositSingleItemTask;

    public BankFetchItemsAndCraftTask(ApiHolder apiHolder, CraftItemTask craftItemTask, BankDepositSingleItemTask bankDepositSingleItemTask) {
        super(apiHolder);
        this.craftItemTask = craftItemTask;
        this.bankDepositSingleItemTask = bankDepositSingleItemTask;
    }

    public void craftItemWithBankItems(CommonBrain brain, CharacterResponseSchema character, String itemToCraft) {
        // get resources needed for item
        Optional<ItemSchema> optionalItemSchema = brain.caches.cachedItems.stream()
                                                                          .filter(item -> item.getCode()
                                                                                              .equals(itemToCraft))
                                                                          .findFirst()
                ;
        if (optionalItemSchema.isEmpty()) {
            logger.warn("No item found for {}", itemToCraft);
            throw new RuntimeException("No item found for " + itemToCraft);
        }
        try {
            // is item already in the bank?
            // TODO paging
            DataPageSimpleItemSchema bankItemsMyBankItemsGet = brain.apiHolder.myAccountApi.getBankItemsMyBankItemsGet(null, 1, 100);
            Optional<SimpleItemSchema> itemInBank = bankItemsMyBankItemsGet.getData()
                                                                           .stream()
                                                                           .filter(item -> itemToCraft.equals(item.getCode()))
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
                fetchItemFromBank(brain, character, neededItem.getCode(), neededItem.getQuantity());
            }

            // craft item
            craftItemTask.craftItem(brain, character.getData()
                                                    .getName(), itemToCraft
            );

            // deposit crafted item into bank
            bankDepositSingleItemTask.depositInventoryInBank(brain, character.getData()
                                                                             .getName(), itemToCraft
            );
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

}
