package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.tasks.*;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AlchemistT1Brain {
    private final Logger logger = LoggerFactory.getLogger(AlchemistT1Brain.class.getName());
    private final CraftItemTask craftItemTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final TrainingSkillTask trainingSkillTask;
    private final ApiHolder apiHolder;
    private final Caches caches;
    private final CharHelper charHelper;
    private final GetBestItemForSlotTask getBestItemForSlotTask;
    private final TaskAcceptNewItemTask taskAcceptNewItemTask;

    public AlchemistT1Brain(ApiHolder apiHolder, CraftItemTask craftItemTask,
                            BankDepositAllTask bankDepositAllTask,
                            TrainingSkillTask trainingSkillTask, Caches caches1,
                            CharHelper charHelper1, GetBestItemForSlotTask getBestItemForSlotTask,
                            TaskAcceptNewItemTask taskAcceptNewItemTask) {
        this.craftItemTask = craftItemTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.trainingSkillTask = trainingSkillTask;
        this.apiHolder = apiHolder;
        this.caches = caches1;
        this.charHelper = charHelper1;
        this.getBestItemForSlotTask = getBestItemForSlotTask;
        this.taskAcceptNewItemTask = taskAcceptNewItemTask;
    }

    public String decideWhatResourceToFarm(String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        String resource = caches.findHighestFarmableResourceForSkillLevel(character.getData()
                                                                                   .getAlchemyLevel(), GatheringSkill.ALCHEMY
        );
        return resource;
    }

    public void runBaseLoop(String characterName) {
        CharacterSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName)
                                                           .getData();
        charHelper.waitUntilCooldownDone(character.getName());
        // getBestItemForSlotTask.equipOrRequestBestToolForSkill(character, "alchemy");
        getBestItemForSlotTask.equipOrRequestItemArmorForSlot(characterName, ItemSlot.BODY_ARMOR);
        bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(character);
        taskAcceptNewItemTask.getNewTaskIfCurrentTaskIsDone(character);

        character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName)
                                           .getData();
        Optional<String> itemToCraft = charHelper.findPossibleItemToCraft(character);
        if (itemToCraft.isPresent()) {
            craftItemTask.craftItem(characterName, itemToCraft.get());
        } else {
            trainingSkillTask.trainSkillsWithBankItems(character, Skill.ALCHEMY);
            // farmHighestResourceTask.farmResource(this, characterName);
        }
    }
}
