package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.BrainCompletedException;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.shopping.Wish;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import de.tkunkel.game.artifactsmmo.tasks.BankFetchItemsAndCraftTask;
import de.tkunkel.game.artifactsmmo.tasks.CraftItemTask;
import de.tkunkel.game.artifactsmmo.tasks.FarmHighestResourceTask;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class MinerT1Brain extends CommonBrain {
    private final Logger logger = LoggerFactory.getLogger(MinerT1Brain.class.getName());
    private FarmHighestResourceTask farmHighestResourceTask;
    private CraftItemTask craftItemTask;
    private BankDepositAllTask bankDepositAllTask;
    private BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask;

    public MinerT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, FarmHighestResourceTask farmHighestResourceTask, CraftItemTask craftItemTask, BankDepositAllTask bankDepositAllTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask) {
        super(caches, wishList, apiHolder, bankFetchItemsAndCraftTask);
        this.farmHighestResourceTask = farmHighestResourceTask;
        this.craftItemTask = craftItemTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.bankFetchItemsAndCraftTask = bankFetchItemsAndCraftTask;
    }

    public boolean hasMaxCraftableGearEquipped(String characterName) {
        return checkIfEquipped(characterName, "copper_dagger", ItemSlot.WEAPON)
                && checkIfEquipped(characterName, "copper_helmet", ItemSlot.HELMET)
                && checkIfEquipped(characterName, "copper_boots", ItemSlot.BOOTS)
                && checkIfEquipped(characterName, "copper_ring", ItemSlot.RING1);
    }

    @Override
    public boolean shouldBeUsed(String characterName) {
        boolean isMissingGear = !hasMaxCraftableGearEquipped(characterName);
        return isMissingGear;
    }

    public boolean mineIfNotEnoughInInventory(String characterName, String oreName, int amount) {
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
            waitUntilCooldownDone(character);
            List<InventorySlot> inventory = character.getData()
                                                     .getInventory();

            int amountInInventory = inventory.stream()
                                             .filter(inventorySlot -> inventorySlot.getCode()
                                                                                   .equals(oreName))
                                             .mapToInt(InventorySlot::getQuantity)
                                             .sum()
                    ;
            if (amountInInventory >= amount) {
                return true;
            }
            Optional<MapSchema> mine = findClosestLocation(character, "copper_rocks");
            if (mine.isEmpty()) {
                logger.error("No mine found");
                return false;
            }
            moveToLocation(character, mine.get());
            gather(character);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        logger.info("Starting mineUntilReached");
        return true;
    }

    private void gather(CharacterResponseSchema character) {
        waitUntilCooldownDone(character);
        try {
            apiHolder.myCharactersApi.actionGatheringMyNameActionGatheringPost(character.getData()
                                                                                        .getName());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

    }

    public void smelt(String characterName, String toCraft) {
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
            Optional<MapSchema> smelter = findClosestLocation(character, "mining");
            if (smelter.isEmpty()) {
                logger.error("No smelter found.");
                throw new RuntimeException("no smelter found");
            }
            waitUntilCooldownDone(character);
            moveToLocation(character, smelter.get());
            waitUntilCooldownDone(character);
            CraftingSchema craftingSchema = new CraftingSchema().code(toCraft)
                                                                .quantity(1);
            apiHolder.myCharactersApi.actionCraftingMyNameActionCraftingPost(character.getData()
                                                                                      .getName(), craftingSchema
            );
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void runBaseLoop(String characterName) throws BrainCompletedException {
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
            bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(this, character);
            waitUntilCooldownDone(character);
            equipOrRequestBestToolForSkill(character, "mining");
            // TODO bankFetchItemsAndCraftTask.craftItemWithBankItems(this, character, "copper_dagger");
            Optional<String> itemToCraft = findPossibleItemToCraftFromWishlist(character);
            if (itemToCraft.isEmpty()) {
                itemToCraft = findPossibleItemToCraft(character);
            }
            if (itemToCraft.isPresent()) {
                craftItemTask.craftItem(this, characterName, itemToCraft.get());
            } else {
                farmHighestResourceTask.farmResource(this, characterName);
            }

            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException | ApiException e) {
            logger.error("Error while mining", e);
            throw new RuntimeException(e);
        }
    }

    private Optional<String> findPossibleItemToCraftFromWishlist(CharacterResponseSchema character) {
        Optional<Wish> wish = wishList.reserveWishThatCanBeCraftedByMe(character);
        return wish.map(value -> value.itemCode);
    }

    @Override
    public String decideWhatResourceToFarm(String characterName) {
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);

            String resource = caches.findHighestFarmableResourceForSkillLevel(character.getData()
                                                                                       .getMiningLevel(), GatheringSkill.MINING
            );
            return resource;
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }


}
