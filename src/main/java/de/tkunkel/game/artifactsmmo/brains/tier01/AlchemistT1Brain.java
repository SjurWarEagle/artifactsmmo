package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.tasks.*;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.GatheringSkill;
import de.tkunkel.games.artifactsmmo.model.ItemSlot;
import de.tkunkel.games.artifactsmmo.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AlchemistT1Brain {
    private final Logger logger = LoggerFactory.getLogger(AlchemistT1Brain.class.getName());
    private final CraftItemTask craftItemTask;
    private final FarmHighestResourceTask farmHighestResourceTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final TrainingSkillTask trainingSkillTask;
    private final ApiHolder apiHolder;
    private final Caches caches;
    private final CharHelper charHelper;
    private final GetBestItemForSlotTask getBestItemForSlotTask;

    public AlchemistT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, CraftItemTask craftItemTask, FarmHighestResourceTask farmHighestResourceTask,
                            BankDepositAllTask bankDepositAllTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask, CharHelper charHelper,
                            MapHelper mapHelper, TrainingSkillTask trainingSkillTask, ApiHolder apiHolder1, Caches caches1, CharHelper charHelper1, GetBestItemForSlotTask getBestItemForSlotTask) {
        this.craftItemTask = craftItemTask;
        this.farmHighestResourceTask = farmHighestResourceTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.trainingSkillTask = trainingSkillTask;
        this.apiHolder = apiHolder1;
        this.caches = caches1;
        this.charHelper = charHelper1;
        this.getBestItemForSlotTask = getBestItemForSlotTask;
    }

    public String decideWhatResourceToFarm(String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        String ressource = caches.findHighestFarmableResourceForSkillLevel(character.getData()
                                                                                    .getAlchemyLevel(), GatheringSkill.ALCHEMY
        );
        return ressource;
    }

    public void runBaseLoop(String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        charHelper.waitUntilCooldownDone(character);
        getBestItemForSlotTask.equipOrRequestBestToolForSkill(character, "alchemy");
        getBestItemForSlotTask.equipOrRequestItemArmorForSlot(characterName, ItemSlot.BODY_ARMOR);
        bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(character);

        character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        Optional<String> itemToCraft = charHelper.findPossibleItemToCraft(character);
        if (itemToCraft.isPresent()) {
            craftItemTask.craftItem(characterName, itemToCraft.get());
        } else {
            trainingSkillTask.trainSkills(character.getData(), Skill.ALCHEMY);
            // farmHighestResourceTask.farmResource(this, characterName);
        }
    }
}
