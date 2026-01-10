package de.tkunkel.game.artifactsmmo.brains;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.BrainCompletedException;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.tasks.BankFetchItemTask;
import de.tkunkel.game.artifactsmmo.tasks.BankFetchItemsAndCraftTask;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public abstract class CommonBrain implements Brain {
    public final Caches caches;
    protected final WishList wishList;
    public final ApiHolder apiHolder;
    public final CharHelper charHelper;
    public final BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask;

    private final Logger logger = LoggerFactory.getLogger(CommonBrain.class.getName());
    protected final MapHelper mapHelper;
    private final BankFetchItemTask bankFetchItemTask;

    protected CommonBrain(Caches caches, WishList wishList, ApiHolder apiHolder, CharHelper charHelper, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask, MapHelper mapHelper, BankFetchItemTask bankFetchItemTask) {
        this.caches = caches;
        this.wishList = wishList;
        this.apiHolder = apiHolder;
        this.charHelper = charHelper;
        this.bankFetchItemsAndCraftTask = bankFetchItemsAndCraftTask;
        this.mapHelper = mapHelper;
        this.bankFetchItemTask = bankFetchItemTask;
    }

    @Override
    public void runBaseLoop(String characterName) throws BrainCompletedException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void eatFoodOrRestIfNeeded(CharacterResponseSchema character) {
        logger.info("Checking if character {} needs to rest", character.getData()
                                                                       .getName()
        );
        // heal if 75% left
        // TODO change to calculate the max damage of my enemy
        if (character.getData()
                     .getHp() > character.getData()
                                         .getMaxHp() * 0.75) {
            return;
        }
        if (eatFoodIfHasFood(character.getData())) {
            return;
        }
        charHelper.waitUntilCooldownDone(character);
        apiHolder.myCharactersApi.actionRestMyNameActionRestPost(character.getData()
                                                                          .getName());
        charHelper.waitUntilCooldownDone(character);
    }

    private boolean eatFoodIfHasFood(CharacterSchema character) {
        if (character.getInventory() == null) {
            return false;
        }
        for (InventorySlot inventorySlot : character.getInventory()) {
            if (inventorySlot.getQuantity() >= 1
                    && (inventorySlot.getCode()
                                     .equalsIgnoreCase("apple")
                    || inventorySlot.getCode()
                                    .equalsIgnoreCase("cooked_chicken"))
            ) {
                SimpleItemSchema simpleItemSchema = new SimpleItemSchema().quantity(1)
                                                                          .code(inventorySlot.getCode());
                charHelper.waitUntilCooldownDone(character.getName());
                apiHolder.myCharactersApi.actionUseItemMyNameActionUsePost(character.getName(), simpleItemSchema
                );
                return true;
            }

        }
        return false;
    }

    public void craftGearIfNotAtCharacter(String characterName, String gear, String craftingStation, ItemSlot slot) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        boolean enoughResourcesToCraft = character.getData()
                                                  .getInventory()
                                                  .stream()
                                                  .filter(inventorySlot -> inventorySlot.getCode()
                                                                                        .equals("copper_bar"))
                                                  .mapToInt(InventorySlot::getQuantity)
                                                  .sum() >= 10;
        if (!enoughResourcesToCraft) {
            return;
        }
        charHelper.waitUntilCooldownDone(character);
        boolean equipped = charHelper.checkIfEquipped(gear, slot, character);
        if (equipped) {
            return;
        }

        Optional<MapSchema> closestLocation = mapHelper.findClosestLocation(character.getData(), craftingStation);
        if (closestLocation.isEmpty()) {
            logger.error("No location found for {}", craftingStation);
            return;
        }
        charHelper.moveToLocationSync(character.getData(), closestLocation.get());
        charHelper.waitUntilCooldownDone(character);
        CraftingSchema craftingSchema = new CraftingSchema().code(gear)
                                                            .quantity(1);
        apiHolder.myCharactersApi.actionCraftingMyNameActionCraftingPost(character.getData()
                                                                                  .getName(), craftingSchema
        );

    }

    public String decideWhatResourceToFarm(String characterName) {
        throw new UnsupportedOperationException("Not implemented");
    }


}
