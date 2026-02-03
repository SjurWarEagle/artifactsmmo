package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FarmAndCraftResourceTask {
    private final Logger logger = LoggerFactory.getLogger(FarmAndCraftResourceTask.class.getName());
    private final CharHelper charHelper;
    private final ItemHelper itemHelper;
    private final CharactersApiWrapper charactersApi;
    private final CharHelper characterHelper;
    private final CraftItemTask craftItemTask;
    private final FarmResourceTask farmResourceTask;
    private final BankFetchItemTask bankFetchItemTask;

    public FarmAndCraftResourceTask(CharHelper charHelper, ItemHelper itemHelper, CharactersApiWrapper charactersApi, CharHelper characterHelper, CraftItemTask craftItemTask, FarmResourceTask farmResourceTask, BankFetchItemTask bankFetchItemTask) {
        this.charHelper = charHelper;
        this.itemHelper = itemHelper;
        this.charactersApi = charactersApi;
        this.characterHelper = characterHelper;
        this.craftItemTask = craftItemTask;
        this.farmResourceTask = farmResourceTask;
        this.bankFetchItemTask = bankFetchItemTask;
    }

    public void farmResource(String characterName, String resourceToFarm, int amount) {

        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        MapSchema whereToGather = itemHelper.findLocationWhereToFarm(character.getData(), resourceToFarm)
                                            .get();
        // logger.info("Farming {} at {}", resourceToFarm, whereToGather);
        charHelper.waitUntilCooldownDone(character);
        charHelper.moveToLocationSync(character.getData(), whereToGather);
        charHelper.waitUntilCooldownDone(character);
        farmResourceTask.farmResource(characterName, resourceToFarm, amount);
        charHelper.waitUntilCooldownDone(character);
    }

    public void farmAndCraft(String charName, String itemCode, Integer quantity) {
        boolean hasNeededResources = false;
        ItemSchema itemSchema = itemHelper.findItemDefinition(itemCode)
                                          .get();
        if (itemSchema.getCraft() == null) {
            CharacterSchema character = charactersApi.getCharacterCharactersNameGet(charName)
                                                     .getData();
            if (characterHelper.canCanGatherItem(character, itemCode)) {
                // farm
                farmResource(charName, itemCode, quantity);
            } else if (characterHelper.cntItemsInBank(itemCode) >= quantity) {
                bankFetchItemTask.fetchItemFromBank(character, itemCode, quantity);
            } else {
                System.out.println("Hm?");
            }
        } else {
            // craft
            hasNeededResources = itemSchema.getCraft()
                                           .getItems()
                                           .stream()
                                           .allMatch(item -> characterHelper.cntItemsInInventory(charName, item.getCode()) >= item.getQuantity());
            if (hasNeededResources) {
                craftItemTask.craftItem(charName, itemCode, quantity);
            } else {
                // do all gathering
                for (SimpleItemSchema neededItem : itemSchema.getCraft()
                                                             .getItems()) {
                    if (itemHelper.findItemDefinition(neededItem.getCode())
                                  .get()
                                  .getCraft() != null) {
                        continue;
                    }
                    if (characterHelper.cntItemsInInventory(charName, neededItem.getCode()) < neededItem.getQuantity() * quantity) {
                        int missing = neededItem.getQuantity() * quantity - characterHelper.cntItemsInInventory(charName, neededItem.getCode());
                        farmAndCraft(charName, neededItem.getCode(), missing);
                    }
                }

                // do all crafting
                // TODO process them from bottom down, how? no idea
                for (SimpleItemSchema neededItem : itemSchema.getCraft()
                                                             .getItems()) {
                    if (itemHelper.findItemDefinition(neededItem.getCode())
                                  .get()
                                  .getCraft() == null) {
                        continue;
                    }
                    if (characterHelper.cntItemsInInventory(charName, neededItem.getCode()) < neededItem.getQuantity() * quantity) {
                        int missing = neededItem.getQuantity() * quantity - characterHelper.cntItemsInInventory(charName, neededItem.getCode());
                        farmAndCraft(charName, neededItem.getCode(), missing);
                    }
                }
            }
        }
    }
}
