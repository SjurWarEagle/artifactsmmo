package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.BrainCompletedException;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.tasks.*;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.GatheringSkill;
import de.tkunkel.games.artifactsmmo.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FisherT1Brain extends CommonBrain {
    private final Logger logger = LoggerFactory.getLogger(FisherT1Brain.class.getName());
    private FarmHighestResourceTask farmHighestResourceTask;
    private CraftItemTask craftItemTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final TrainingSkillTask trainingSkillTask;

    public FisherT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, FarmHighestResourceTask farmHighestResourceTask,
                         CraftItemTask craftItemTask, BankDepositAllTask bankDepositAllTask,
                         BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask,
                         CharHelper charHelper, MapHelper mapHelper, TrainingSkillTask trainingSkillTask) {
        super(caches, wishList, apiHolder, charHelper, bankFetchItemsAndCraftTask, mapHelper);
        this.farmHighestResourceTask = farmHighestResourceTask;
        this.craftItemTask = craftItemTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.trainingSkillTask = trainingSkillTask;
    }

    @Override
    public String decideWhatResourceToFarm(String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);

        String resource = caches.findHighestFarmableResourceForSkillLevel(character.getData()
                                                                                   .getFishingLevel(), GatheringSkill.FISHING
        );
        return resource;
    }

    @Override
    public void runBaseLoop(String characterName) throws BrainCompletedException {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        charHelper.waitUntilCooldownDone(character);
        equipOrRequestBestToolForSkill(character, "fishing");
        bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(this, character);

        character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        Optional<String> itemToCraft = findPossibleItemToCraft(character);
        if (itemToCraft.isPresent()) {
            craftItemTask.craftItem(characterName, itemToCraft.get());
        } else {
            trainingSkillTask.trainSkills(character.getData(), Skill.FISHING, Skill.JEWELRYCRAFTING);
            // farmHighestResourceTask.farmResource(this, characterName);
        }
    }
}
