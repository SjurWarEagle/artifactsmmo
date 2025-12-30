package de.tkunkel.game.artifactsmmo;

import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.brains.tier01.*;
import de.tkunkel.game.artifactsmmo.shopping.Wish;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Adventurer {
    private final AdventurerClass adventurerClass;
    private final ApiHolder apiHolder;
    private final BankDepositAllTask bankDepositAllTask;
    Logger logger = LoggerFactory.getLogger(Adventurer.class.getName());

    private final String characterName;
    private final Set<CommonBrain> brains;
    private CommonBrain brain;
    private final WishList wishList;

    public Adventurer(String characterName, AdventurerClass adventurerClass, ApiHolder apiHolder, BankDepositAllTask bankDepositAllTask, Set<CommonBrain> brains, WishList wishList) {
        this.characterName = characterName;
        this.adventurerClass = adventurerClass;
        this.apiHolder = apiHolder;
        this.bankDepositAllTask = bankDepositAllTask;
        this.brains = brains;
        this.wishList = wishList;
        brain = decideNewBrain();
    }

    public void startLoop() {
        try {
            bankDepositAllTask.depositInventoryInBank(brain, apiHolder.charactersApi.getCharacterCharactersNameGet(characterName));
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        while (true) {
            logger.info("Adventurer {} of class {} is running", characterName, adventurerClass.name());
            brain.equipOrRequestBestWeapon(characterName);
            brain.equipOrRequestBestArmorForSlot(characterName, "body_armor");
            brain.equipOrRequestBestArmorForSlot(characterName, "helmet");
            brain.equipOrRequestBestArmorForSlot(characterName, "shield");
            brain.equipOrRequestBestArmorForSlot(characterName, "boots");
            brain.equipOrRequestBestArmorForSlot(characterName, "leg_armor");

            try {
                CharacterResponseSchema character = null;
                try {
                    character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
                } catch (ApiException e) {
                    throw new RuntimeException(e);
                }
                Optional<Wish> wishThatCanBeCraftedByMe = wishList.reserveWishThatCanBeCraftedByMe(character);
                boolean allResourcesAvailable = checkIfAllResourcesAreAvailable(character, wishThatCanBeCraftedByMe);
                if (allResourcesAvailable && wishThatCanBeCraftedByMe.isPresent()) {
                    Wish wish = wishThatCanBeCraftedByMe.get();
                    brain.bankFetchItemsAndCraftTask.craftItemWithBankItems(brain, character, wish.itemCode);
                    wish.fulfilled = true;
                } else {
                    // nothing to craft, so use default
                    brain.runBaseLoop(characterName);
                }
            } catch (BrainCompletedException e) {
                logger.info("Adventurer {} of class {} needs new brain", characterName, adventurerClass.name());
                brain = decideNewBrain();
            }
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean checkIfAllResourcesAreAvailable(CharacterResponseSchema character, @UnknownNullability Optional<Wish> optionalWish) {
        if (optionalWish.isEmpty()) {
            return false;
        }
        Wish wish = optionalWish.get();
        boolean inInventory = character.getData()
                                       .getInventory()
                                       .stream()
                                       .filter(inventorySlot -> inventorySlot.getCode()
                                                                             .equals(wish.itemCode))
                                       .findAny()
                                       .isPresent()
                ;
        // TODO paging!
        try {
            boolean inBank = apiHolder.myAccountApi.getBankItemsMyBankItemsGet(wish.itemCode, 1, 100)
                                                   .getData()
                                                   .size() > 0;
            return inInventory || inBank;
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private CommonBrain decideNewBrain() {
        Optional<CommonBrain> optionalBrain = switch (adventurerClass) {
            case MINER -> brains.stream()
                                .filter(brain -> brain instanceof MinerT1Brain)
                                .findFirst()
            ;
            case FIGHTER -> brains.stream()
                                  .filter(brain -> brain instanceof FighterT1Brain)
                                  .findFirst()
            ;
            case WOODWORKER -> brains.stream()
                                     .filter(brain -> brain instanceof WoodworkerT1Brain)
                                     .findFirst()
            ;
            case ALCHEMIST -> brains.stream()
                                    .filter(brain -> brain instanceof AlchemistT1Brain)
                                    .findFirst()
            ;
            case FISHER -> brains.stream()
                                 .filter(brain -> brain instanceof FisherT1Brain)
                                 .findFirst()
            ;
        };
        return optionalBrain.get();
    }
}
