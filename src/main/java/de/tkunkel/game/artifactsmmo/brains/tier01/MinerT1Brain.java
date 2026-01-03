package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.BrainCompletedException;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.shopping.Wish;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.tasks.*;
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
    private final FarmHighestResourceTask farmHighestResourceTask;
    private final CraftItemTask craftItemTask;
    private final TrainingSkillTask trainingSkillTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask;

    public MinerT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, FarmHighestResourceTask farmHighestResourceTask, CraftItemTask craftItemTask, BankDepositAllTask bankDepositAllTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask, TrainingSkillTask trainingSkillTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask1) {
        super(caches, wishList, apiHolder, bankFetchItemsAndCraftTask);
        this.farmHighestResourceTask = farmHighestResourceTask;
        this.craftItemTask = craftItemTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.trainingSkillTask = trainingSkillTask;
        this.bankFetchItemsAndCraftTask = bankFetchItemsAndCraftTask1;
    }

    public boolean mineIfNotEnoughInInventory(String characterName, String oreName, int amount) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        waitUntilCooldownDone(character);

        Optional<ItemSchema> itemToTrain = trainingSkillTask.findBestItemToHarvestAndCraft(this, character, Skill.MINING, Skill.GEARCRAFTING);

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


            Optional<Wish> wish = findPossibleItemToCraftFromWishlist(character);

            if (wish.isPresent()) {
                bankFetchItemsAndCraftTask.craftItemWithBankItems(this, character, wish.get().itemCode);
                wish.get().reservedBy = null;
                wish.get().fulfilled = true;
            } else {
                var itemToCraft = findPossibleItemToCraft(character);
                if (itemToCraft.isPresent()) {
                    craftItemTask.craftItem(this, characterName, itemToCraft.get());
                } else {
                    farmHighestResourceTask.farmResource(this, characterName);
                }
            }

            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException e) {
            logger.error("Error while mining", e);
            throw new RuntimeException(e);
        }
    }

    private Optional<Wish> findPossibleItemToCraftFromWishlist(CharacterResponseSchema character) {
        Optional<Wish> wish = wishList.reserveWishThatCanBeCraftedByMe(character);
        return wish;
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
