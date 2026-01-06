package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.ApiHolder;
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
public class AlchemistT1Brain extends CommonBrain {
    private final Logger logger = LoggerFactory.getLogger(AlchemistT1Brain.class.getName());
    private final CraftItemTask craftItemTask;
    private final FarmHighestResourceTask farmHighestResourceTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final TrainingSkillTask trainingSkillTask;

    public AlchemistT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, CraftItemTask craftItemTask, FarmHighestResourceTask farmHighestResourceTask,
                            BankDepositAllTask bankDepositAllTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask, CharHelper charHelper,
                            MapHelper mapHelper, TrainingSkillTask trainingSkillTask) {
        super(caches, wishList, apiHolder, charHelper, bankFetchItemsAndCraftTask, mapHelper);
        this.craftItemTask = craftItemTask;
        this.farmHighestResourceTask = farmHighestResourceTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.trainingSkillTask = trainingSkillTask;
    }

    @Override
    public String decideWhatResourceToFarm(String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        String ressource = caches.findHighestFarmableResourceForSkillLevel(character.getData()
                                                                                    .getAlchemyLevel(), GatheringSkill.ALCHEMY
        );
        return ressource;
    }

    @Override
    public void runBaseLoop(String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        charHelper.waitUntilCooldownDone(character);
        equipOrRequestBestToolForSkill(character, "alchemy");
        equipOrRequestBestArmorForSlot(characterName, "body_armor");
        bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(this, character);

        character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        Optional<String> itemToCraft = findPossibleItemToCraft(character);
        if (itemToCraft.isPresent()) {
            craftItemTask.craftItem(this, characterName, itemToCraft.get());
        } else {
            trainingSkillTask.trainSkills(character.getData(), Skill.ALCHEMY);
            // farmHighestResourceTask.farmResource(this, characterName);
        }
    }
}
