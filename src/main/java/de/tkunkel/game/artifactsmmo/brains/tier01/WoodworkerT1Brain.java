package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import de.tkunkel.game.artifactsmmo.tasks.CraftItemTask;
import de.tkunkel.game.artifactsmmo.tasks.TaskAcceptNewItemTask;
import de.tkunkel.game.artifactsmmo.tasks.TrainingSkillTask;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WoodworkerT1Brain {
    private final Logger logger = LoggerFactory.getLogger(WoodworkerT1Brain.class.getName());
    private final CraftItemTask craftItemTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final TrainingSkillTask trainingSkillTask;
    private final CharHelper charHelper;
    private final CharactersApiWrapper charactersApi;
    private final TaskAcceptNewItemTask taskAcceptNewItemTask;

    public WoodworkerT1Brain(CraftItemTask craftItemTask, BankDepositAllTask bankDepositAllTask,
                             CharHelper charHelper, TrainingSkillTask trainingSkillTask, CharactersApiWrapper charactersApi,
                             TaskAcceptNewItemTask taskAcceptNewItemTask) {
        this.craftItemTask = craftItemTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.trainingSkillTask = trainingSkillTask;
        this.charHelper = charHelper;
        this.charactersApi = charactersApi;
        this.taskAcceptNewItemTask = taskAcceptNewItemTask;
    }

    public void runBaseLoop(String characterName) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        charHelper.waitUntilCooldownDone(character.getName());
        bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(character);

        taskAcceptNewItemTask.getNewTaskIfCurrentTaskIsDone(character);

        character = charactersApi.getCharacterCharactersNameGet(characterName)
                                 .getData();
        Optional<String> itemToCraft = charHelper.findPossibleItemToCraft(character);
        if (itemToCraft.isPresent()) {
            craftItemTask.craftItem(characterName, itemToCraft.get());
        } else {
            trainingSkillTask.trainSkills(character, Skill.WOODCUTTING, Skill.WEAPONCRAFTING);
            // farmHighestResourceTask.farmResource(this, characterName);
        }
    }
}
