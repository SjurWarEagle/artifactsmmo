package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.BrainCompletedException;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
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
public class FisherT1Brain extends CommonBrain {
    private final Logger logger = LoggerFactory.getLogger(FisherT1Brain.class.getName());
    private FarmHighestResourceTask farmHighestResourceTask;
    private CraftItemTask craftItemTask;

    public FisherT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, FarmHighestResourceTask farmHighestResourceTask, CraftItemTask craftItemTask) {
        super(caches, wishList, apiHolder);
        this.farmHighestResourceTask = farmHighestResourceTask;
        this.craftItemTask = craftItemTask;
    }

    @Override
    public boolean shouldBeUsed(String characterName) {
        return false;
    }

    public String decideWhatResourceToFarm(String characterName) {
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);

            String resource = caches.findHighestFarmableRessourceForSkillLevel(character.getData()
                                                                                        .getFishingLevel(), GatheringSkill.FISHING
            );
            return resource;
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void runBaseLoop(String characterName) throws BrainCompletedException {
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
            depositInBankIfInventoryIsFull(character);
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
