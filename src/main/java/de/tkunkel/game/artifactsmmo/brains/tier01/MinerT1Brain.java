package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.game.artifactsmmo.shopping.Wish;
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
public class MinerT1Brain extends CommonBrain {
    private final Logger logger = LoggerFactory.getLogger(MinerT1Brain.class.getName());
    private final TrainingSkillTask trainingSkillTask;
    private final BankDepositAllTask bankDepositAllTask;


    public MinerT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, FarmHighestResourceTask farmHighestResourceTask,
                        CraftItemTask craftItemTask, BankDepositAllTask bankDepositAllTask,
                        BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask, TrainingSkillTask trainingSkillTask,
                        CharHelper charHelper, MapHelper mapHelper) {
        super(caches, wishList, apiHolder, charHelper, bankFetchItemsAndCraftTask, mapHelper);
        this.bankDepositAllTask = bankDepositAllTask;
        this.trainingSkillTask = trainingSkillTask;
    }

    @Override
    public void runBaseLoop(String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        charHelper.waitUntilCooldownDone(character);
        bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(this, character);
        charHelper.waitUntilCooldownDone(character);
        equipOrRequestBestToolForSkill(character, "mining");

        // TODO farm items for wish
        Optional<Wish> wish = findPossibleItemToCraftFromWishlist(character);

        if (wish.isPresent()) {
            bankFetchItemsAndCraftTask.craftItemWithBankItems(this, character, wish.get().itemCode, wish.get().amount);
            wish.get().reservedBy = null;
            wish.get().fulfilled = true;
        } else {
            trainingSkillTask.trainSkills(character.getData(), Skill.MINING, Skill.GEARCRAFTING, Skill.WEAPONCRAFTING);
        }
    }

    private Optional<Wish> findPossibleItemToCraftFromWishlist(CharacterResponseSchema character) {
        return wishList.reserveWishThatCanBeCraftedByMe(character);
    }

    @Override
    public String decideWhatResourceToFarm(String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);

        return caches.findHighestFarmableResourceForSkillLevel(character.getData()
                                                                        .getMiningLevel(), GatheringSkill.MINING
        );
    }


}
