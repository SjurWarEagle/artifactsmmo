package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class BankFetchItemTask {
    private final Logger logger = LoggerFactory.getLogger(BankFetchItemTask.class.getName());
    private final MapHelper mapHelper;
    private final CharHelper charHelper;
    private final MyCharactersApiWrapper myCharactersApi;

    public BankFetchItemTask(MapHelper mapHelper, CharHelper charHelper, MyCharactersApiWrapper myCharactersApi) {
        this.mapHelper = mapHelper;
        this.charHelper = charHelper;
        this.myCharactersApi = myCharactersApi;
    }


    public void fetchItemFromBank(CharacterSchema character, String neededItemCode, int quantity) {
        charHelper.waitUntilCooldownDone(character.getName());
        charHelper.moveToLocationSync(character, mapHelper.findClosestLocation(character, "bank")
                                                          .get()
        );
        charHelper.waitUntilCooldownDone(character.getName());

        SimpleItemSchema simpleItemSchema = new SimpleItemSchema().code(neededItemCode)
                                                                  .quantity(quantity);
        myCharactersApi.actionWithdrawBankItemMyNameActionBankWithdrawItemPost(character.getName(), Collections.singletonList(simpleItemSchema)
        );
        charHelper.waitUntilCooldownDone(character.getName());
    }


}
