package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.GatheringSkill;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import de.tkunkel.games.artifactsmmo.model.ResourceSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HarvestResourceTask {
    private final Logger logger = LoggerFactory.getLogger(HarvestResourceTask.class.getName());
    private final CharHelper charHelper;
    private final Caches caches;
    private final CharactersApiWrapper charactersApi;
    private final MyCharactersApiWrapper myCharactersApi;
    private final GetBestItemForSlotTask getBestItemForSlotTask;

    public HarvestResourceTask(CharHelper charHelper, Caches caches,
                               CharactersApiWrapper charactersApi, MyCharactersApiWrapper myCharactersApi,
                               GetBestItemForSlotTask getBestItemForSlotTask) {
        this.charHelper = charHelper;
        this.caches = caches;
        this.charactersApi = charactersApi;
        this.myCharactersApi = myCharactersApi;
        this.getBestItemForSlotTask = getBestItemForSlotTask;
    }

    public void farmResourceWithTool(String characterName, MapSchema whereToGather) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        Optional<GatheringSkill> neededSkill = findSkillNeededToFarm(whereToGather);
        if (neededSkill.isPresent()) {
            getBestItemForSlotTask.equipOrRequestBestToolForSkill(character, neededSkill.get()
                                                                                        .name()
            );
        }
        charHelper.moveToLocationSync(characterName, whereToGather);
        charHelper.waitUntilCooldownDone(characterName);


        charHelper.waitUntilCooldownDone(character.getName());
        charHelper.moveToLocationSync(character, whereToGather);
        charHelper.waitUntilCooldownDone(character.getName());
        myCharactersApi.actionGatheringMyNameActionGatheringPost(character.getName());
        charHelper.waitUntilCooldownDone(character.getName());
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
