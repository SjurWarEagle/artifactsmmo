package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.shopping.Wish;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.InventorySlot;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetBestItemForSlotTask {
    private final Logger logger = LoggerFactory.getLogger(GetBestItemForSlotTask.class.getName());


    private final CharHelper charHelper;
    private final MyAccountApiWrapper myAccountApi;
    private final WishList wishList;
    private final BankFetchItemTask bankFetchItemTask;
    private final CharactersApiWrapper charactersApi;

    public GetBestItemForSlotTask(CharHelper charHelper, MyAccountApiWrapper myAccountApi, WishList wishList,
                                  BankFetchItemTask bankFetchItemTask, CharactersApiWrapper charactersApi) {
        this.charHelper = charHelper;
        this.myAccountApi = myAccountApi;
        this.wishList = wishList;
        this.bankFetchItemTask = bankFetchItemTask;
        this.charactersApi = charactersApi;
    }

    public void equipOrRequestBestWeapon(String characterName) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        Optional<ItemSchema> bestForSlot = charHelper.findBestItemForSlotThatCanBeCraftedByAccount(ItemSlot.WEAPON, character);
        if (bestForSlot.isEmpty()) {
            return;
        }
        ItemSlot itemSlot = ItemSlot.fromValue(bestForSlot.get()
                                                          .getType());
        if (charHelper.checkIfEquipped(character.getName(), bestForSlot.get()
                                                                       .getCode(), itemSlot
        )) {
            return;
        }
        Optional<InventorySlot> inventorySlot = character.getInventory()
                                                         .stream()
                                                         .filter(innerInventorySlot -> innerInventorySlot.getCode()
                                                                                                         .equals(bestForSlot.get()
                                                                                                                            .getCode()))
                                                         .findFirst()
                ;
        boolean itemExistsInBank;
        itemExistsInBank = !myAccountApi.getBankItemsMyBankItemsGet(bestForSlot.get()
                                                                               .getCode(), 1, 100
                                        )
                                        .getData()
                                        .isEmpty();
        boolean itemExistsInInventory = inventorySlot.isPresent();

        boolean alreadyEquipped = charHelper.checkIfEquipped(bestForSlot.get()
                                                                        .getCode(), itemSlot, character
        );
        if (!itemExistsInInventory && !itemExistsInBank && !alreadyEquipped) {
            logger.info("Best item (%s) for %s not in inventory nor bank nor equipped, requesting"
                                .formatted(bestForSlot.get()
                                                      .getCode(), itemSlot.getValue()
                                ));
            wishList.addRequest(new Wish(character.getName(), bestForSlot.get()
                                                                         .getCode()
                                        , 1
                                ), false
            );
            return;
        }
        if (!alreadyEquipped && itemExistsInBank) {
            bankFetchItemTask.fetchItemFromBank(character, bestForSlot.get()
                                                                      .getCode()
                    , 1
            );
        }
        if (!alreadyEquipped) {
            charHelper.equipGearIfNotEquipped(character.getName(), bestForSlot.get()
                                                                              .getCode(), itemSlot
            );
        }
    }

    public void equipOrRequestItemArmorForSlot(String characterName, ItemSlot slot) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();

        Optional<ItemSchema> bestInSlot = charHelper.findBestItemForSlotThatCanBeCraftedByAccount(slot, character);
        if (bestInSlot.isEmpty()) {
            return;
        }
        if (charHelper.checkIfEquipped(character.getName(), bestInSlot.get()
                                                                      .getCode(), slot
        )) {
            return;
        }
        Optional<InventorySlot> inventorySlot = character.getInventory()
                                                         .stream()
                                                         .filter(innerInventorySlot -> innerInventorySlot.getCode()
                                                                                                         .equals(bestInSlot.get()
                                                                                                                           .getCode()))
                                                         .findFirst()
                ;
        boolean itemExistsInBank;
        itemExistsInBank = myAccountApi.getBankItemsMyBankItemsGet(bestInSlot.get()
                                                                             .getCode(), 1, 100
                                       )
                                       .getData()
                                       .size() > 0;
        boolean itemExistsInInventory = inventorySlot.isPresent();

        boolean alreadyEquipped = charHelper.checkIfEquipped(bestInSlot.get()
                                                                       .getCode(), slot, character
        );
        if (!itemExistsInInventory && !itemExistsInBank && !alreadyEquipped) {
            logger.info("Best tool (" + bestInSlot.get()
                                                  .getCode() + ") not in inventory nor bank nor equipped, requesting");
            wishList.addRequest(new Wish(character.getName(), bestInSlot.get()
                                                                        .getCode()
                                        , 1
                                ), false
            );
            return;
        }
        if (!alreadyEquipped && itemExistsInBank) {
            bankFetchItemTask.fetchItemFromBank(character, bestInSlot.get()
                                                                     .getCode()
                    , 1
            );
        }
        if (!alreadyEquipped) {
            charHelper.equipGearIfNotEquipped(character.getName(), bestInSlot.get()
                                                                             .getCode(), slot
            );
        }
    }

    public void equipOrRequestBestToolForSkill(CharacterSchema character, String skillName) {
        Optional<ItemSchema> bestToolForSkill = charHelper.findBestToolForSkillThatCanBeCraftedByAccount(skillName, character.getMiningLevel()
        );
        if (bestToolForSkill.isEmpty()) {
            return;
        }
        ItemSlot itemSlot = ItemSlot.fromValue(bestToolForSkill.get()
                                                               .getType());
        if (charHelper.checkIfEquipped(character.getName(), bestToolForSkill.get()
                                                                            .getCode(), itemSlot
        )) {
            return;
        }

        Optional<InventorySlot> inventorySlot = character.getInventory()
                                                         .stream()
                                                         .filter(innerInventorySlot -> innerInventorySlot.getCode()
                                                                                                         .equals(bestToolForSkill.get()
                                                                                                                                 .getCode()))
                                                         .findFirst()
                ;
        boolean itemExistsInBank = !myAccountApi.getBankItemsMyBankItemsGet(bestToolForSkill.get()
                                                                                            .getCode(), 1, 100
                                                )
                                                .getData()
                                                .isEmpty();
        boolean itemExistsInInventory = inventorySlot.isPresent();

        boolean alreadyEquipped = charHelper.checkIfEquipped(bestToolForSkill.get()
                                                                             .getCode(), itemSlot, character
        );
        if (!itemExistsInInventory && !itemExistsInBank && !alreadyEquipped) {
            logger.info("Best tool (" + bestToolForSkill.get()
                                                        .getCode() + ") not in inventory nor bank nor equipped, requesting");
            wishList.addRequest(new Wish(character.getName(), bestToolForSkill.get()
                                                                              .getCode()
                                        , 1
                                ), false
            );
            return;
        }
        if (!alreadyEquipped && itemExistsInBank) {
            bankFetchItemTask.fetchItemFromBank(character, bestToolForSkill.get()
                                                                           .getCode()
                    , 1
            );
        }
        if (!alreadyEquipped) {
            charHelper.equipGearIfNotEquipped(character.getName(), bestToolForSkill.get()
                                                                                   .getCode(), itemSlot
            );
        }
    }

}
