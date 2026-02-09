package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.GatheringSkill;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import de.tkunkel.games.artifactsmmo.model.Skill;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FarmResourceTask {
    private final Logger logger = LoggerFactory.getLogger(FarmResourceTask.class.getName());
    private final Caches caches;
    private final CharHelper charHelper;
    private final ItemHelper itemHelper;
    private final GetBestItemForSlotTask getBestItemForSlotTask;
    private final CharactersApiWrapper charactersApi;
    private final MyCharactersApiWrapper myCharactersApi;

    public FarmResourceTask(Caches caches, CharHelper charHelper, ItemHelper itemHelper, GetBestItemForSlotTask getBestItemForSlotTask, CharactersApiWrapper charactersApi, MyCharactersApiWrapper myCharactersApi) {
        this.caches = caches;
        this.charHelper = charHelper;
        this.itemHelper = itemHelper;
        this.getBestItemForSlotTask = getBestItemForSlotTask;
        this.charactersApi = charactersApi;
        this.myCharactersApi = myCharactersApi;
    }

    public boolean canFarmResource(String characterName, String resourceToFarm) {

        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        if (itemHelper.findLocationWhereToFarm(character.getData(), resourceToFarm)
                      .isEmpty()) {
            return false;
        }
        MapSchema whereToGather = itemHelper.findLocationWhereToFarm(character.getData(), resourceToFarm)
                                            .get();
        Optional<Pair<GatheringSkill, Integer>> neededSkill = findSkillNeededToFarm(whereToGather);
        if (neededSkill.isPresent()) {
            var skill = Skill.fromValue(neededSkill.get()
                                                   .getLeft()
                                                   .getValue());
            return charHelper.charHasEnoughSkill(character.getData(), skill, neededSkill.get()
                                                                                        .getRight()
            );
        }
        return false;
    }

    public void farmResource(String characterName, String resourceToFarm, int amount) {

        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        if (itemHelper.findLocationWhereToFarm(character.getData(), resourceToFarm)
                      .isEmpty()) {
            System.err.println("MEEEP");
        }
        MapSchema whereToGather = itemHelper.findLocationWhereToFarm(character.getData(), resourceToFarm)
                                            .get();
        Optional<Pair<GatheringSkill, Integer>> neededSkill = findSkillNeededToFarm(whereToGather);
        if (neededSkill.isPresent()) {
            getBestItemForSlotTask.equipOrRequestBestToolForSkill(character.getData(), neededSkill.get()
                                                                                                  .getLeft()
                                                                                                  .name()
            );
        }
        charHelper.waitUntilCooldownDone(character);
        charHelper.moveToLocationSync(character.getData(), whereToGather);
        charHelper.waitUntilCooldownDone(character);
        for (int i = 0; i < amount; i++) {
            myCharactersApi.actionGatheringMyNameActionGatheringPost(character.getData()
                                                                              .getName());
            charHelper.waitUntilCooldownDone(character);
        }
    }

    private Optional<Pair<GatheringSkill, Integer>> findSkillNeededToFarm(MapSchema whereToGather) {
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
                                     .map(x -> Pair.of(x.getSkill(), x.getLevel()))
                                     .findFirst()
                ;

    }
}
