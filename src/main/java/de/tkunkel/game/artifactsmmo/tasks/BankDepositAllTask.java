package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
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
    private final Caches caches;

    public BankDepositAllTask(CharactersApiWrapper charactersApi, MyCharactersApiWrapper myCharactersApi, CharHelper charHelper, MapHelper mapHelper, Caches caches) {
        this.charactersApi = charactersApi;
        this.myCharactersApi = myCharactersApi;
        this.charHelper = charHelper;
        this.mapHelper = mapHelper;
        this.caches = caches;
    }

    public void depositInventoryInBankIfInventoryIsFull(CharacterSchema character) {
        character = charactersApi.getCharacterCharactersNameGet(character.getName())
                                 .getData();
        int inventoryUsed = charHelper.cntAllItemsInInventory(character.getName());
        // store if more than 75% are used
        if (inventoryUsed < character.getInventoryMaxItems() * 0.75) {
            return;
        }

        Optional<MapSchema> bank = mapHelper.findClosestLocation(character, "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getName());
        }
        charHelper.moveToLocationSync(character, bank.get());
        charHelper.waitUntilCooldownDone(character.getName());
        List<SimpleItemSchema> itemsToDeposit = character.getInventory()
                                                         .stream()
                                                         .filter(inventorySlot -> {
                                                             List<ItemSchema> item = caches.cachedItems.stream()
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
        myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getName(), itemsToDeposit
        );
        charHelper.waitUntilCooldownDone(character.getName());
    }

    public void depositInventoryInBank(String characterName) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        Optional<MapSchema> bank = mapHelper.findClosestLocation(character, "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getName());
        }
        List<SimpleItemSchema> itemsToDeposit = character.getInventory()
                                                         .stream()
                                                         .filter(inventorySlot -> inventorySlot.getQuantity() > 0)
                                                         .map(inventorySlot -> new SimpleItemSchema().code(inventorySlot.getCode())
                                                                                                     .quantity(inventorySlot.getQuantity()))
                                                         .toList()
                ;
        if (!itemsToDeposit.isEmpty()) {
            charHelper.moveToLocationSync(character, bank.get());
            charHelper.waitUntilCooldownDone(character.getName());
            myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getName(), itemsToDeposit);
            charHelper.waitUntilCooldownDone(character.getName());
        }
        charHelper.waitUntilCooldownDone(character.getName());
    }

    public void depositItemInBank(CharacterSchema character, String itemCode, int amount) {
        Optional<MapSchema> bank = mapHelper.findClosestLocation(character, "bank");
        charHelper.moveToLocationSync(character, bank.get());
        List<SimpleItemSchema> itemsToDeposit = new ArrayList<>();
        itemsToDeposit.add(new SimpleItemSchema().code(itemCode)
                                                 .quantity(amount));
        charHelper.waitUntilCooldownDone(character.getName());
        myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getName(), itemsToDeposit
        );
        charHelper.waitUntilCooldownDone(character.getName());
    }
}
