package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import de.tkunkel.game.artifactsmmo.tasks.TaskAcceptNewItemTask;
import de.tkunkel.game.artifactsmmo.tasks.TrainingSkillTask;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.GatheringSkill;
import de.tkunkel.games.artifactsmmo.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FisherT1Brain {
    private final Logger logger = LoggerFactory.getLogger(FisherT1Brain.class.getName());
    private final BankDepositAllTask bankDepositAllTask;
    private final TrainingSkillTask trainingSkillTask;
    private final CharHelper charHelper;
    private final CharactersApiWrapper charactersApi;
    private final Caches caches;
    private final TaskAcceptNewItemTask taskAcceptNewItemTask;

    public FisherT1Brain(BankDepositAllTask bankDepositAllTask,
                         TrainingSkillTask trainingSkillTask, CharHelper charHelper,
                         CharactersApiWrapper charactersApi, Caches caches,
                         TaskAcceptNewItemTask taskAcceptNewItemTask) {
        this.bankDepositAllTask = bankDepositAllTask;
        this.trainingSkillTask = trainingSkillTask;
        this.charHelper = charHelper;
        this.charactersApi = charactersApi;
        this.caches = caches;
        this.taskAcceptNewItemTask = taskAcceptNewItemTask;
    }

    public String decideWhatResourceToFarm(String characterName) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);

        String resource = caches.findHighestFarmableResourceForSkillLevel(character.getData()
                                                                                   .getFishingLevel(), GatheringSkill.FISHING
        );
        return resource;
    }

    public void runBaseLoop(String characterName) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        charHelper.waitUntilCooldownDone(character.getName());
//        getBestItemForSlot.equipOrRequestBestToolForSkill(character, "fishing");
        bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(character);
        taskAcceptNewItemTask.getNewTaskIfCurrentTaskIsDone(character);

        trainingSkillTask.trainSkills(character, Skill.FISHING, Skill.JEWELRYCRAFTING);
    }
}
