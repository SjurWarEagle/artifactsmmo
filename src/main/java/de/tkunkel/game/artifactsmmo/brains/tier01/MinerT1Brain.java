package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
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
public class MinerT1Brain {
    private final Logger logger = LoggerFactory.getLogger(MinerT1Brain.class.getName());
    private final TrainingSkillTask trainingSkillTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final CharactersApiWrapper charactersApi;
    private final CharHelper charHelper;
    private final GetBestItemForSlotTask getBestItemForSlotTask;
    private final BankFetchItemTask bankFetchItemTask;
    private final BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask;
    private final WishList wishList;
    private final Caches caches;

    public MinerT1Brain(Caches caches, WishList wishList,
                        BankDepositAllTask bankDepositAllTask,
                        TrainingSkillTask trainingSkillTask,
                        CharHelper charHelper, CharactersApiWrapper charactersApi,
                        GetBestItemForSlotTask getBestItemForSlotTask, BankFetchItemTask bankFetchItemTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask) {
        this.bankDepositAllTask = bankDepositAllTask;
        this.trainingSkillTask = trainingSkillTask;
        this.charactersApi = charactersApi;
        this.charHelper = charHelper;
        this.getBestItemForSlotTask = getBestItemForSlotTask;
        this.bankFetchItemTask = bankFetchItemTask;
        this.wishList = wishList;
        this.caches = caches;
        this.bankFetchItemsAndCraftTask = bankFetchItemsAndCraftTask;
    }

    public void runBaseLoop(String characterName) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        charHelper.waitUntilCooldownDone(character);
        bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(character);
        charHelper.waitUntilCooldownDone(character);
        // getBestItemForSlotTask.equipOrRequestBestToolForSkill(character, "mining");

        // TODO farm items for wish
        Optional<Wish> wish = findPossibleItemToCraftFromWishlist(character);

        if (wish.isPresent()) {
            bankFetchItemsAndCraftTask.craftItemWithBankItems(character.getData(), wish.get().itemCode, wish.get().amount);
            wish.get().reservedBy = null;
            wish.get().fulfilled = true;
        } else {
            trainingSkillTask.trainSkills(character.getData(), Skill.MINING, Skill.GEARCRAFTING);
        }
    }

    private Optional<Wish> findPossibleItemToCraftFromWishlist(CharacterResponseSchema character) {
        return wishList.reserveWishThatCanBeCraftedByMe(character);
    }

    public String decideWhatResourceToFarm(String characterName) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);

        return caches.findHighestFarmableResourceForSkillLevel(character.getData()
                                                                        .getMiningLevel(), GatheringSkill.MINING
        );
    }


}
