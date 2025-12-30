package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.BrainCompletedException;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import de.tkunkel.game.artifactsmmo.tasks.BankFetchItemsAndCraftTask;
import de.tkunkel.game.artifactsmmo.tasks.CraftItemTask;
import de.tkunkel.game.artifactsmmo.tasks.FarmHighestResourceTask;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.GatheringSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WoodworkerT1Brain extends CommonBrain {
    private final Logger logger = LoggerFactory.getLogger(WoodworkerT1Brain.class.getName());
    private FarmHighestResourceTask farmHighestResourceTask;
    private CraftItemTask craftItemTask;
    private BankDepositAllTask bankDepositAllTask;

    public WoodworkerT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, FarmHighestResourceTask farmHighestResourceTask, CraftItemTask craftItemTask, BankDepositAllTask bankDepositAllTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask) {
        super(caches, wishList, apiHolder, bankFetchItemsAndCraftTask);
        this.farmHighestResourceTask = farmHighestResourceTask;
        this.craftItemTask = craftItemTask;
        this.bankDepositAllTask = bankDepositAllTask;
    }

    @Override
    public String decideWhatResourceToFarm(String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);

        String resource = caches.findHighestFarmableResourceForSkillLevel(character.getData()
                                                                                   .getWoodcuttingLevel(), GatheringSkill.WOODCUTTING
        );
        return resource;
    }

    @Override
    public void runBaseLoop(String characterName) throws BrainCompletedException {
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
            waitUntilCooldownDone(character);
            equipOrRequestBestToolForSkill(character, "woodcutting");
            bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(this, character);

            character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
            Optional<String> itemToCraft = findPossibleItemToCraft(character);
            if (itemToCraft.isPresent()) {
                craftItemTask.craftItem(this, characterName, itemToCraft.get());
            } else {
                farmHighestResourceTask.farmResource(this, characterName);
            }
        } catch (ApiException e) {
            logger.error("Error while farming", e);
            throw new RuntimeException(e);
        }
    }
}
