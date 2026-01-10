package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BankDepositGoldIfRichTask {
    private final Logger logger = LoggerFactory.getLogger(BankDepositGoldIfRichTask.class.getName());
    private final CharHelper charHelper;
    private final MapHelper mapHelper;
    private final MyCharactersApiWrapper myCharactersApi;

    public BankDepositGoldIfRichTask(CharHelper charHelper, MapHelper mapHelper, MyCharactersApiWrapper myCharactersApi) {
        this.charHelper = charHelper;
        this.mapHelper = mapHelper;
        this.myCharactersApi = myCharactersApi;
    }

    public void depositInventoryInBankIfInventoryIsFull(CharacterResponseSchema character) {
        if (character.getData()
                     .getGold() < 100) {
            return;
        }

        Optional<MapSchema> bank = mapHelper.findClosestLocation(character.getData(), "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getData()
                                                                                       .getName());
        }
        charHelper.moveToLocationSync(character.getData(), bank.get());
        charHelper.waitUntilCooldownDone(character);

        DepositWithdrawGoldSchema goldDeposit = new DepositWithdrawGoldSchema().quantity(character.getData()
                                                                                                  .getGold());
        myCharactersApi.actionDepositBankGoldMyNameActionBankDepositGoldPost(character.getData()
                                                                                      .getName(), goldDeposit
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

}
