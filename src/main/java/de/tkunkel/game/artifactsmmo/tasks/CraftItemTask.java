package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.model.CraftingSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CraftItemTask {
    private final Logger logger = LoggerFactory.getLogger(CraftItemTask.class.getName());

    public void craftItem(CommonBrain brain, String characterName, String itemToCraft) {
        MapSchema map = brain.findLocationToCraftItem(characterName, itemToCraft);
        brain.moveToLocation(characterName, map);
        brain.waitUntilCooldownDone(characterName);
        brain.apiHolder.myCharactersApi.actionCraftingMyNameActionCraftingPost(characterName, new CraftingSchema().code(itemToCraft)
                                                                                                                  .quantity(1)
        );
        brain.waitUntilCooldownDone(characterName);
    }

}
