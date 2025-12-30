package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.ApiException;
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

    public void perform(CommonBrain brain, CharacterResponseSchema character) {
        brain.waitUntilCooldownDone(character);

        try {
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

            Optional<MapSchema> bank = brain.findClosestLocation(character, "bank");
            if (bank.isEmpty()) {
                throw new RuntimeException("Could not find bank for character " + character.getData()
                                                                                           .getName());
            }
            brain.moveToLocation(character, bank.get());


            DepositWithdrawGoldSchema transaction = new DepositWithdrawGoldSchema().quantity(bankDetailsMyBankGet.getData()
                                                                                                                 .getNextExpansionCost());
            brain.apiHolder.myCharactersApi.actionWithdrawBankGoldMyNameActionBankWithdrawGoldPost(character.getData()
                                                                                                            .getName(), transaction
            );
            brain.waitUntilCooldownDone(character);

            brain.apiHolder.myCharactersApi.actionBuyBankExpansionMyNameActionBankBuyExpansionPost(character.getData()
                                                                                                            .getName()
            );
            brain.waitUntilCooldownDone(character);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

}
