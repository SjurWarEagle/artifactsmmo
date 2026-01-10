package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
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
public class WoodworkerT1Brain {
    private final Logger logger = LoggerFactory.getLogger(WoodworkerT1Brain.class.getName());
    private final FarmHighestResourceTask farmHighestResourceTask;
    private final CraftItemTask craftItemTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final TrainingSkillTask trainingSkillTask;
    private final CharHelper charHelper;
    private final CharactersApiWrapper charactersApi;
    private final Caches caches;
    private final GetBestItemForSlotTask getBestItemForSlot;

    public WoodworkerT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, FarmHighestResourceTask farmHighestResourceTask,
                             CraftItemTask craftItemTask, BankDepositAllTask bankDepositAllTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask,
                             CharHelper charHelper, MapHelper mapHelper, TrainingSkillTask trainingSkillTask, CharHelper charHelper1, CharactersApiWrapper charactersApi, Caches caches1, GetBestItemForSlotTask getBestItemForSlot) {
        this.farmHighestResourceTask = farmHighestResourceTask;
        this.craftItemTask = craftItemTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.trainingSkillTask = trainingSkillTask;
        this.charHelper = charHelper1;
        this.charactersApi = charactersApi;
        this.caches = caches;
        this.getBestItemForSlot = getBestItemForSlot;
    }

    public String decideWhatResourceToFarm(String characterName) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);

        String resource = caches.findHighestFarmableResourceForSkillLevel(character.getData()
                                                                                   .getWoodcuttingLevel(), GatheringSkill.WOODCUTTING
        );
        return resource;
    }

    public void runBaseLoop(String characterName) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        charHelper.waitUntilCooldownDone(character);
        // getBestItemForSlot.equipOrRequestBestToolForSkill(character, "woodcutting");
        bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(character);

        character = charactersApi.getCharacterCharactersNameGet(characterName);
        Optional<String> itemToCraft = charHelper.findPossibleItemToCraft(character);
        if (itemToCraft.isPresent()) {
            craftItemTask.craftItem(characterName, itemToCraft.get());
        } else {
            trainingSkillTask.trainSkills(character.getData(), Skill.WOODCUTTING, Skill.WEAPONCRAFTING);
            // farmHighestResourceTask.farmResource(this, characterName);
        }
    }
}
