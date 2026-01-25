package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.game.artifactsmmo.helper.NpcHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.NPCItem;
import de.tkunkel.games.artifactsmmo.model.NPCSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BuyItemForTokenTask {
    private final Logger logger = LoggerFactory.getLogger(BuyItemForTokenTask.class.getName());
    private final MapHelper mapHelper;
    private final ItemHelper itemHelper;
    private final CharHelper charHelper;
    private final NpcHelper npcHelper;
    private final MyCharactersApiWrapper myCharactersApi;
    private final BankFetchItemTask bankFetchItemTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final CharactersApiWrapper charactersApi;

    public BuyItemForTokenTask(MapHelper mapHelper, ItemHelper itemHelper, CharHelper charHelper, NpcHelper npcHelper, MyCharactersApiWrapper myCharactersApi, BankFetchItemTask bankFetchItemTask, BankDepositAllTask bankDepositAllTask, CharactersApiWrapper charactersApi) {
        this.mapHelper = mapHelper;
        this.itemHelper = itemHelper;
        this.charHelper = charHelper;
        this.npcHelper = npcHelper;
        this.myCharactersApi = myCharactersApi;
        this.bankFetchItemTask = bankFetchItemTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.charactersApi = charactersApi;
    }


    public void buyItem(String characterName, String neededItemCode, int quantity) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        Optional<NPCSchema> npc = npcHelper.findNpcThatSellsExcludeGold(neededItemCode);
        if (npc.isEmpty()) {
            throw new RuntimeException("No npc found that sells " + neededItemCode);
        }
        Optional<NPCItem> npcItem = npcHelper.findNpcItemFromSeller(neededItemCode, npc.get());
        if (npcItem.isEmpty()) {
            throw new RuntimeException("No npc item found that sells " + neededItemCode);
        }
        int currencyInBank = charHelper.cntItemsInBank(npcItem.get()
                                                              .getCurrency());

        if (currencyInBank < npcItem.get()
                                    .getBuyPrice() * quantity) {
            // not enough currency
            return;
        }
        charHelper.waitUntilCooldownDone(character.getName());
        bankDepositAllTask.depositInventoryInBank(character.getName());
        charHelper.waitUntilCooldownDone(character.getName());
        bankFetchItemTask.fetchItemFromBank(character, npcItem.get()
                                                              .getCurrency(), npcItem.get()
                                                                                     .getBuyPrice() * quantity
        );

        charHelper.waitUntilCooldownDone(character.getName());
        charHelper.moveToLocationSync(character, mapHelper.findClosestLocation(character, npc.get()
                                                                                             .getCode()
                                                          )
                                                          .get()
        );
        charHelper.waitUntilCooldownDone(character.getName());

        myCharactersApi.actionNpcBuyItemMyNameActionNpcBuyPost(character.getName(), neededItemCode, quantity);
        charHelper.waitUntilCooldownDone(character.getName());
        bankDepositAllTask.depositInventoryInBank(character.getName());
        charHelper.waitUntilCooldownDone(character.getName());
    }

    public boolean canBuyItem(String characterName, String neededItemCode, int quantity) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        Optional<NPCSchema> npc = npcHelper.findNpcThatSellsExcludeGold(neededItemCode);
        if (npc.isEmpty()) {
            return false;
        }
        Optional<NPCItem> npcItem = npcHelper.findNpcItemFromSeller(neededItemCode, npc.get());
        if (npcItem.isEmpty()) {
            return false;
        }
        int currencyInBank = charHelper.cntItemsInBank(npcItem.get()
                                                              .getCurrency());

        if (currencyInBank < npcItem.get()
                                    .getBuyPrice() * quantity) {
            // not enough currency
            return false;
        }
        return true;
    }


}
