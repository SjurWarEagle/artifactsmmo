package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.ApiHolder;
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
public class AlchemistT1Brain extends CommonBrain {
    private final Logger logger = LoggerFactory.getLogger(AlchemistT1Brain.class.getName());
    private final CraftItemTask craftItemTask;
    private final FarmHighestResourceTask farmHighestResourceTask;

    public AlchemistT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, CraftItemTask craftItemTask, FarmHighestResourceTask farmHighestResourceTask) {
        super(caches, wishList, apiHolder);
        this.craftItemTask = craftItemTask;
        this.farmHighestResourceTask = farmHighestResourceTask;
    }

    @Override
    public boolean shouldBeUsed(String characterName) {
        return false;
    }

    public String decideWhatResourceToFarm(String characterName) {
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
            String ressource = caches.findHighestFarmableRessourceForSkillLevel(character.getData()
                                                                                         .getAlchemyLevel(), GatheringSkill.ALCHEMY
            );
            return ressource;
        } catch (ApiException e) {
            logger.error("Error while deciding what resource to farm", e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public void runBaseLoop(String characterName) {
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
            logger.error("Error while running base loop", e);
            throw new RuntimeException(e);
        }
    }
}
