package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
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
    private final CharHelper charHelper;
    private final CharactersApiWrapper charactersApi;
    private final MapHelper mapHelper;
    private final MyCharactersApiWrapper myCharactersApi;

    public BankDepositSingleItemTask(CharHelper charHelper, CharactersApiWrapper charactersApi, MapHelper mapHelper, MyCharactersApiWrapper myCharactersApi) {
        this.charHelper = charHelper;
        this.charactersApi = charactersApi;
        this.mapHelper = mapHelper;
        this.myCharactersApi = myCharactersApi;
    }

    public void depositInventoryInBank(String characterName, String itemToDeposit) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        Optional<MapSchema> bank = mapHelper.findClosestLocation(character.getData(), "bank");
        if (bank.isEmpty()) {
            logger.error("Could not find bank for character %s".formatted(character.getData()
                                                                                   .getName()));
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
        if (!itemsToDeposit.isEmpty()) {
            charHelper.moveToLocationSync(character.getData(), bank.get());
            charHelper.waitUntilCooldownDone(character);
            myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getData()
                                                                                          .getName(), itemsToDeposit
            );
        }
        charHelper.waitUntilCooldownDone(character);
    }

}
