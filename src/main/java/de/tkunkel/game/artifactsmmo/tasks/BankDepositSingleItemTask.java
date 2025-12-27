package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BankDepositSingleItemTask {
    private final Logger logger = LoggerFactory.getLogger(BankDepositSingleItemTask.class.getName());

    public void depositInventoryInBank(CommonBrain brain, CharacterResponseSchema character, String itemToDeposit) {
        Optional<MapSchema> bank = brain.findClosestLocation(character, "bank");
        if (bank.isEmpty()) {
            logger.error("Could not find bank for character " + character.getData()
                                                                         .getName());
            throw new RuntimeException("Could not find bank for character " + character.getData()
                                                                                       .getName());
        }
        List<SimpleItemSchema> itemsToDeposit = character.getData()
                                                         .getInventory()
                                                         .stream()
                                                         .filter(inventorySlot -> inventorySlot.getCode()
                                                                                               .equals(itemToDeposit))
                                                         .map(inventorySlot -> new SimpleItemSchema().code(inventorySlot.getCode())
                                                                                                     .quantity(inventorySlot.getQuantity()))
                                                         .toList()
                ;
        try {
            if (!itemsToDeposit.isEmpty()) {
                brain.moveToLocation(character, bank.get());
                brain.waitUntilCooldownDone(character);
                brain.apiHolder.myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getData()
                                                                                                              .getName(), itemsToDeposit
                );
            }
            brain.waitUntilCooldownDone(character);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

}
