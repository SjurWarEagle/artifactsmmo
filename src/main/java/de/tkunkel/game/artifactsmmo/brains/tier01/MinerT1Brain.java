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

    public MinerT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, FarmHighestResourceTask farmHighestResourceTask, CraftItemTask craftItemTask, BankDepositAllTask bankDepositAllTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask) {
        super(caches, wishList, apiHolder, bankFetchItemsAndCraftTask);
        this.farmHighestResourceTask = farmHighestResourceTask;
        this.craftItemTask = craftItemTask;
        this.bankDepositAllTask = bankDepositAllTask;
    }

    public boolean mineIfNotEnoughInInventory(String characterName, String oreName, int amount) {
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
        logger.info("Starting mineUntilReached");
        return true;
    }

    private void gather(CharacterResponseSchema character) {
        waitUntilCooldownDone(character);
        apiHolder.myCharactersApi.actionGatheringMyNameActionGatheringPost(character.getData()
                                                                                    .getName());

    }

    public void smelt(String characterName, String toCraft) {
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
        } catch (InterruptedException e) {
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
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);

        String resource = caches.findHighestFarmableResourceForSkillLevel(character.getData()
                                                                                   .getMiningLevel(), GatheringSkill.MINING
        );
        return resource;
    }


}
