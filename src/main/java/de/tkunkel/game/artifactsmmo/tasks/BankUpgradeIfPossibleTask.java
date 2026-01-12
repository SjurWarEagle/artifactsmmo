package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.BankResponseSchema;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.DepositWithdrawGoldSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BankUpgradeIfPossibleTask {
    private final Logger logger = LoggerFactory.getLogger(BankUpgradeIfPossibleTask.class.getName());
    private final CharHelper charHelper;
    private final MapHelper mapHelper;
    private final MyAccountApiWrapper myAccountApi;
    private final MyCharactersApiWrapper myCharactersApi;

    public BankUpgradeIfPossibleTask(CharHelper charHelper, MapHelper mapHelper, MyAccountApiWrapper myAccountApi, MyCharactersApiWrapper myCharactersApi) {
        this.charHelper = charHelper;
        this.mapHelper = mapHelper;
        this.myAccountApi = myAccountApi;
        this.myCharactersApi = myCharactersApi;
    }

    public void perform(CharacterSchema character) {
        charHelper.waitUntilCooldownDone(character.getName());

        BankResponseSchema bankDetailsMyBankGet = myAccountApi.getBankDetailsMyBankGet();
        if (bankDetailsMyBankGet.getData()
                                .getNextExpansionCost() > bankDetailsMyBankGet.getData()
                                                                              .getGold()) {
            // too expensive
            return;
        }
        logger.info("Bank upgrade possible for character {}", character.getName()
        );

        Optional<MapSchema> bank = mapHelper.findClosestLocation(character, "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getName());
        }
        charHelper.moveToLocationSync(character, bank.get());


        DepositWithdrawGoldSchema transaction = new DepositWithdrawGoldSchema().quantity(bankDetailsMyBankGet.getData()
                                                                                                             .getNextExpansionCost());
        myCharactersApi.actionWithdrawBankGoldMyNameActionBankWithdrawGoldPost(character.getName(), transaction
        );
        charHelper.waitUntilCooldownDone(character.getName());

        myCharactersApi.actionBuyBankExpansionMyNameActionBankBuyExpansionPost(character.getName()
        );
        charHelper.waitUntilCooldownDone(character.getName());
    }

}
