package de.tkunkel.game.artifactsmmo;

import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.brains.tier01.*;
import de.tkunkel.game.artifactsmmo.shopping.Wish;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSlot;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

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
        brain = decideBrain();
    }

    public void startLoop() {
        bankDepositAllTask.depositInventoryInBank(brain, apiHolder.charactersApi.getCharacterCharactersNameGet(characterName));
        while (true) {
            logger.info("Adventurer {} of class {} is running", characterName, adventurerClass.name());
            // no weapon because a non-fighter switched between tool and weapon in each loop
            brain.equipOrRequestItemArmorForSlot(characterName, ItemSlot.BODY_ARMOR);
            brain.equipOrRequestItemArmorForSlot(characterName, ItemSlot.HELMET);
            brain.equipOrRequestItemArmorForSlot(characterName, ItemSlot.SHIELD);
            brain.equipOrRequestItemArmorForSlot(characterName, ItemSlot.BOOTS);
            brain.equipOrRequestItemArmorForSlot(characterName, ItemSlot.RING1);
            brain.equipOrRequestItemArmorForSlot(characterName, ItemSlot.RING2);
            brain.equipOrRequestItemArmorForSlot(characterName, ItemSlot.AMULET);
            brain.equipOrRequestItemArmorForSlot(characterName, ItemSlot.LEG_ARMOR);

            try {
                CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
                Optional<Wish> wishThatCanBeCraftedByMe = wishList.reserveWishThatCanBeCraftedByMe(character);
                boolean allResourcesAvailable = checkIfAllResourcesAreAvailable(character, wishThatCanBeCraftedByMe);
                if (allResourcesAvailable && wishThatCanBeCraftedByMe.isPresent()) {
                    Wish wish = wishThatCanBeCraftedByMe.get();
                    brain.bankFetchItemsAndCraftTask.craftItemWithBankItems(brain, character, wish.itemCode, wish.amount);
                    wish.fulfilled = true;
                    wish.reservedBy = null;
                } else {
                    // nothing to craft, so use default
                    brain.runBaseLoop(characterName);
                }
            } catch (BrainCompletedException _) {
                logger.info("Adventurer {} of class {} needs new brain", characterName, adventurerClass.name());
                brain = decideBrain();
            }
        }
    }

    private boolean checkIfAllResourcesAreAvailable(CharacterResponseSchema character, @UnknownNullability Optional<Wish> optionalWish) {
        if (optionalWish.isEmpty()) {
            return false;
        }
        Wish wish = optionalWish.get();
        Optional<ItemSchema> itemDefinition = brain.caches.findItemDefinition(wish.itemCode);
        if (itemDefinition.isEmpty()
                || itemDefinition.get().getCraft()==null
                || itemDefinition.get().getCraft().getItems()==null) {
            return false;
        }
        return itemDefinition.get()
                             .getCraft()
                             .getItems()
                             .stream()
                             .allMatch(resourceItem -> {
                                 if (character.getData()
                                         .getInventory()==null){
                                     return false;
                                 }
                                 boolean inInventory = character.getData()
                                                                .getInventory()
                                                                .stream()
                                                                .anyMatch(inventorySlot -> inventorySlot.getCode()
                                                                                                      .equals(resourceItem.getCode()))
                                         ;
                                 if (inInventory) {
                                     return true;
                                 }
                                 return !apiHolder.myAccountApi.getBankItemsMyBankItemsGet(resourceItem.getCode(), 1, 100
                                                           )
                                                                        .getData().isEmpty();
                             })
                ;


    }

    private CommonBrain decideBrain() {
        Optional<CommonBrain> optionalBrain = switch (adventurerClass) {
            case MINER -> brains.stream()
                                .filter(MinerT1Brain.class::isInstance)
                                .findFirst()
            ;
            case FIGHTER -> brains.stream()
                                  .filter(FighterT1Brain.class::isInstance)
                                  .findFirst()
            ;
            case WOODWORKER -> brains.stream()
                                     .filter(WoodworkerT1Brain.class::isInstance)
                                     .findFirst()
            ;
            case ALCHEMIST -> brains.stream()
                                    .filter(AlchemistT1Brain.class::isInstance)
                                    .findFirst()
            ;
            case FISHER -> brains.stream()
                                 .filter(FisherT1Brain.class::isInstance)
                                 .findFirst()
            ;
        };
        return optionalBrain.get();
    }
}
