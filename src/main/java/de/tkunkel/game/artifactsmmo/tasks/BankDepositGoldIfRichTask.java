package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.DepositWithdrawGoldSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    public void depositGoldInBank(CharacterSchema character) {
        if (character.getGold() < 100) {
            return;
        }

        Optional<MapSchema> bank = mapHelper.findClosestLocation(character, "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getName());
        }
        charHelper.moveToLocationSync(character, bank.get());
        charHelper.waitUntilCooldownDone(character.getName());

        DepositWithdrawGoldSchema goldDeposit = new DepositWithdrawGoldSchema().quantity(character.getGold());
        myCharactersApi.actionDepositBankGoldMyNameActionBankDepositGoldPost(character.getName(), goldDeposit
        );
        charHelper.waitUntilCooldownDone(character.getName());
    }

}
