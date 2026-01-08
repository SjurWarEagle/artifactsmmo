package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BankDepositAllTask {
    private final Logger logger = LoggerFactory.getLogger(BankDepositAllTask.class.getName());
    private final CharactersApiWrapper charactersApi;
    private final MyCharactersApiWrapper myCharactersApi;
    private final CharHelper charHelper;
    private final MapHelper mapHelper;

    public BankDepositAllTask(CharactersApiWrapper charactersApi, MyCharactersApiWrapper myCharactersApi, CharHelper charHelper, MapHelper mapHelper) {
        this.charactersApi = charactersApi;
        this.myCharactersApi = myCharactersApi;
        this.charHelper = charHelper;
        this.mapHelper = mapHelper;
    }

    public void depositInventoryInBankIfInventoryIsFull(CommonBrain brain, CharacterResponseSchema character) {
        character = charactersApi.getCharacterCharactersNameGet(character.getData()
                                                                         .getName());
        int inventoryUsed = brain.cntAllItemsInInventory(character);
        // store if more than 75% are used
        if (inventoryUsed < character.getData()
                                     .getInventoryMaxItems() * 0.75) {
            return;
        }

        Optional<MapSchema> bank = mapHelper.findClosestLocation(character.getData(), "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getData()
                                                                                       .getName());
        }
        charHelper.moveToLocationSync(character.getData(), bank.get());
        charHelper.waitUntilCooldownDone(character);
        List<SimpleItemSchema> itemsToDeposit = character.getData()
                                                         .getInventory()
                                                         .stream()
                                                         .filter(inventorySlot -> {
                                                             List<ItemSchema> item = brain.caches.cachedItems.stream()
                                                                                                             .filter(itemSchema -> itemSchema.getCode()
                                                                                                                                             .equals(inventorySlot.getCode()))
//                                                                                                          .filter(itemSchema -> !itemSchema.getSubtype()
//                                                                                                                                           .equals("bar"))
                                                                                                             .toList()
                                                                     ;
                                                             return !item.isEmpty();
                                                         })
                                                         .map(inventorySlot -> new SimpleItemSchema().code(inventorySlot.getCode())
                                                                                                     .quantity(inventorySlot.getQuantity()))
                                                         .toList()
                ;
        brain.apiHolder.myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getData()
                                                                                                      .getName(), itemsToDeposit
        );
        charHelper.waitUntilCooldownDone(character);
    }

    public void depositInventoryInBank(CommonBrain brain, CharacterResponseSchema character) {
        Optional<MapSchema> bank = mapHelper.findClosestLocation(character.getData(), "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getData()
                                                                                       .getName());
        }
        List<SimpleItemSchema> itemsToDeposit = character.getData()
                                                         .getInventory()
                                                         .stream()
                                                         .filter(inventorySlot -> {
                                                             List<ItemSchema> item = brain.caches.cachedItems.stream()
                                                                                                             .filter(itemSchema -> itemSchema.getCode()
                                                                                                                                             .equals(inventorySlot.getCode()))
//                                                                                                          .filter(itemSchema -> !itemSchema.getSubtype()
//                                                                                                                                           .equals("bar"))
                                                                                                             .toList()
                                                                     ;
                                                             return !item.isEmpty();
                                                         })
                                                         .map(inventorySlot -> new SimpleItemSchema().code(inventorySlot.getCode())
                                                                                                     .quantity(inventorySlot.getQuantity()))
                                                         .toList()
                ;
        if (!itemsToDeposit.isEmpty()) {
            charHelper.moveToLocationSync(character.getData(), bank.get());
            charHelper.waitUntilCooldownDone(character);
            brain.apiHolder.myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getData()
                                                                                                          .getName(), itemsToDeposit
            );
        }
        charHelper.waitUntilCooldownDone(character);
    }

    public void depositItemInBank(CharacterResponseSchema character, String itemCode, int amount) {
        Optional<MapSchema> bank = mapHelper.findClosestLocation(character.getData(), "bank");
        charHelper.moveToLocationSync(character.getData(), bank.get());
        List<SimpleItemSchema> itemsToDeposit = new ArrayList<>();
        itemsToDeposit.add(new SimpleItemSchema().code(itemCode)
                                                 .quantity(amount));
        charHelper.waitUntilCooldownDone(character);
        myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getData()
                                                                                      .getName(), itemsToDeposit
        );
        charHelper.waitUntilCooldownDone(character);
    }
}
