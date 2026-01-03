package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.Skill;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class TrainingSkillTask {
    private final Caches caches;

    public TrainingSkillTask(Caches caches) {
        this.caches = caches;
    }

    public Optional<ItemSchema> findHighestItemThatThisCharCanCreateAlone(CommonBrain brain, CharacterResponseSchema character, Skill... skills) {
        Optional<Skill> skillToTrain = Arrays.stream(skills)
                                             .min((o1, o2) -> {
                                                 int skillLevel1 = CharHelper.getSkillLevelForSkill(character.getData(), o1);
                                                 int skillLevel2 = CharHelper.getSkillLevelForSkill(character.getData(), o2);
                                                 return skillLevel1 - skillLevel2;
                                             });
        if (skillToTrain.isEmpty()) {
            throw new RuntimeException("No skill to train found");
        }
        return caches.cachedItems.stream()
                                 // TODO what to do if it is null?
                                 .filter(itemSchema -> itemSchema.getCraft() != null)
                                 .filter(itemSchema -> itemSchema.getCraft()
                                                                 .getSkill()
                                                                 .getValue()
                                                                 .equalsIgnoreCase(skillToTrain.get()
                                                                                               .getValue()))
                                 .filter(itemSchema -> {
                                     Integer neededLevel = itemSchema.getCraft()
                                                                     .getLevel();

                                     boolean isRelevantSkill = Arrays.stream(skills)
                                                                     .anyMatch(skillToTrain.get()::equals);
                                     boolean charHasEnoughSkill = charHasEnoughSkill(character, skillToTrain.get(), neededLevel);
                                     return isRelevantSkill && charHasEnoughSkill;

                                 })
                                 .filter(itemSchema -> canCanGatherResources(character, itemSchema))
                                 .sorted((o1, o2) -> o2.getLevel() - o1.getLevel())
                                 .findFirst()
                ;
    }

    private boolean canCanGatherResources(CharacterResponseSchema character, ItemSchema itemSchema) {
        return itemSchema.getCraft()
                         .getItems()
                         .stream()
                         .allMatch(simpleItemSchema -> {
                                       ItemSchema resource = caches.cachedItems.stream()
                                                                               .filter(itemDef -> itemDef.getCode()
                                                                                                         .equalsIgnoreCase(simpleItemSchema.getCode())
                                                                               )
                                                                               .findFirst()
                                                                               .get()
                                               ;
                                       var resourceSource = caches.cachedResources.stream()
                                                                                  .filter(resourceSchema -> {
                                                                                      return resourceSchema.getDrops()
                                                                                                           .stream()
                                                                                                           .anyMatch(dropRateSchema -> dropRateSchema.getCode()
                                                                                                                                                     .equalsIgnoreCase(resource.getCode()));
                                                                                  })
                                                                                  .findFirst()
                                               ;
                                       if (resourceSource.isEmpty()) {
                                           // this is no resource to gather but to craft
                                           return canCanGatherResources(character, caches.findItemDefinition(simpleItemSchema.getCode())
                                                                                         .get()
                                           );
                                       } else {
                                           // this is something to harvest
                                       }
                                       var harvestSkill = Skill.fromValue(resourceSource.get()
                                                                                        .getSkill()
                                                                                        .getValue());
                                       return charHasEnoughSkill(character, harvestSkill, resourceSource.get()
                                                                                                        .getLevel()
                                       );
                                   }
                         )
                ;
    }

    private boolean charHasEnoughSkill(CharacterResponseSchema character, Skill skill, Integer skillLevel) {
        int skillOfChar = getSkillOfCharToCreate(character, skill);
        return skillOfChar >= skillLevel;
    }

    private int getSkillOfCharToCreate(CharacterResponseSchema character, Skill skill) {
        return switch (skill) {
            case WEAPONCRAFTING -> character.getData()
                                            .getWeaponcraftingLevel();
            case GEARCRAFTING -> character.getData()
                                          .getGearcraftingLevel();
            case JEWELRYCRAFTING -> character.getData()
                                             .getJewelrycraftingLevel();
            case COOKING -> character.getData()
                                     .getCookingLevel();
            case WOODCUTTING -> character.getData()
                                         .getWoodcuttingLevel();
            case MINING -> character.getData()
                                    .getMiningLevel();
            case ALCHEMY -> character.getData()
                                     .getAlchemyLevel();
            case FISHING -> character.getData()
                                     .getFishingLevel();
            default -> throw new RuntimeException("unknown skill " + skill.name());
        };
    }
}
