package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.BankResponseSchema;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
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

    public BankUpgradeIfPossibleTask(CharHelper charHelper, MapHelper mapHelper) {
        this.charHelper = charHelper;
        this.mapHelper = mapHelper;
    }

    public void perform(CommonBrain brain, CharacterResponseSchema character) {
        charHelper.waitUntilCooldownDone(character);

        BankResponseSchema bankDetailsMyBankGet = brain.apiHolder.myAccountApi.getBankDetailsMyBankGet();
        if (bankDetailsMyBankGet.getData()
                                .getNextExpansionCost() > bankDetailsMyBankGet.getData()
                                                                              .getGold()) {
            // too expensive
            return;
        }
        logger.info("Bank upgrade possible for character {}", character.getData()
                                                                       .getName()
        );

        Optional<MapSchema> bank = mapHelper.findClosestLocation(character, "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getData()
                                                                                       .getName());
        }
        charHelper.moveToLocationSync(character, bank.get());


        DepositWithdrawGoldSchema transaction = new DepositWithdrawGoldSchema().quantity(bankDetailsMyBankGet.getData()
                                                                                                             .getNextExpansionCost());
        brain.apiHolder.myCharactersApi.actionWithdrawBankGoldMyNameActionBankWithdrawGoldPost(character.getData()
                                                                                                        .getName(), transaction
        );
        charHelper.waitUntilCooldownDone(character);

        brain.apiHolder.myCharactersApi.actionBuyBankExpansionMyNameActionBankBuyExpansionPost(character.getData()
                                                                                                        .getName()
        );
        charHelper.waitUntilCooldownDone(character);
    }

}
