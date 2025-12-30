package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BankDepositGoldIfRichTask {
    private final Logger logger = LoggerFactory.getLogger(BankDepositGoldIfRichTask.class.getName());

    public void depositInventoryInBankIfInventoryIsFull(CommonBrain brain, CharacterResponseSchema character) {
        if (character.getData()
                     .getGold() < 100) {
            return;
        }

        Optional<MapSchema> bank = brain.findClosestLocation(character, "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getData()
                                                                                       .getName());
        }
        brain.moveToLocation(character, bank.get());
        brain.waitUntilCooldownDone(character);

        DepositWithdrawGoldSchema goldDeposit = new DepositWithdrawGoldSchema().quantity(character.getData()
                                                                                                  .getGold());
        brain.apiHolder.myCharactersApi.actionDepositBankGoldMyNameActionBankDepositGoldPost(character.getData()
                                                                                                      .getName(), goldDeposit
        );
        brain.waitUntilCooldownDone(character);
    }

    public void depositInventoryInBank(CommonBrain brain, CharacterResponseSchema character) {
        Optional<MapSchema> bank = brain.findClosestLocation(character, "bank");
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
            brain.moveToLocation(character, bank.get());
            brain.waitUntilCooldownDone(character);
            brain.apiHolder.myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getData()
                                                                                                          .getName(), itemsToDeposit
            );
        }
        brain.waitUntilCooldownDone(character);
    }

}
