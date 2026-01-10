package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HarvestResourceTask {
    private final Logger logger = LoggerFactory.getLogger(HarvestResourceTask.class.getName());
    private final CharHelper charHelper;
    private final ItemHelper itemHelper;
    private final Caches caches;
    private final CharactersApiWrapper charactersApi;
    private final MyCharactersApiWrapper myCharactersApi;

    public HarvestResourceTask(CharHelper charHelper, ItemHelper itemHelper, Caches caches, CharactersApiWrapper charactersApi, MyCharactersApiWrapper myCharactersApi) {
        this.charHelper = charHelper;
        this.itemHelper = itemHelper;
        this.caches = caches;
        this.charactersApi = charactersApi;
        this.myCharactersApi = myCharactersApi;
    }

    public void farmResourceWithTool(String characterName, MapSchema whereToGather) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        Optional<GatheringSkill> neededSkill = findSkillNeededToFarm(whereToGather);
        if (neededSkill.isPresent()) {
            int charSkillLevel = charHelper.getSkillLevelForSkill(character.getData(), neededSkill.get()
                                                                                                  .getValue()
            );
            Optional<ItemSchema> bestTool = charHelper.findBestToolForSkillThatCanBeCraftedByAccount(neededSkill.get()
                                                                                                                .getValue(), charSkillLevel
            );
        }

        charHelper.waitUntilCooldownDone(character);
        charHelper.moveToLocationSync(character.getData(), whereToGather);
        charHelper.waitUntilCooldownDone(character);
        myCharactersApi.actionGatheringMyNameActionGatheringPost(character.getData()
                                                                          .getName());
        charHelper.waitUntilCooldownDone(character);
    }

    private Optional<GatheringSkill> findSkillNeededToFarm(MapSchema whereToGather) {
        if (whereToGather.getInteractions()
                         .getContent() == null) {
            return Optional.empty();
        }
        String resourceCode = whereToGather.getInteractions()
                                           .getContent()
                                           .getCode()
                ;
        return caches.cachedResources.stream()
                                     .filter(resourceSchema -> resourceSchema.getCode()
                                                                             .equals(resourceCode))
                                     .map(ResourceSchema::getSkill)
                                     .findFirst()
                ;

    }
}
