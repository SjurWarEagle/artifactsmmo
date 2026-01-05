package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import de.tkunkel.games.artifactsmmo.model.Skill;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class TrainingSkillTask {
    private final Caches caches;
    private final ItemHelper itemHelper;

    public TrainingSkillTask(Caches caches, ItemHelper itemHelper) {
        this.caches = caches;
        this.itemHelper = itemHelper;
    }

    public Optional<ItemSchema> findHighestItemThatThisCharCanCreateAlone(CharacterSchema character, Skill... skills) {
        Optional<Skill> skillToTrain = Arrays.stream(skills)
                                             .min((o1, o2) -> {
                                                 int skillLevel1 = CharHelper.getSkillLevelForSkill(character, o1);
                                                 int skillLevel2 = CharHelper.getSkillLevelForSkill(character, o2);
                                                 return skillLevel1 - skillLevel2;
                                             });
        if (skillToTrain.isEmpty()) {
            throw new RuntimeException("No skill to train found");
        }
        // TODO what to do if it is null?
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

                                     boolean isRelevantSkill = Arrays.asList(skills)
                                                                     .contains(skillToTrain.get());
                                     boolean charHasEnoughSkill = charHasEnoughSkill(character, skillToTrain.get(), neededLevel);
                                     return isRelevantSkill && charHasEnoughSkill;

                                 })
                                 .filter(itemSchema -> canCanGatherResources(character, itemSchema))
                                 .min((o1, o2) -> o2.getLevel() - o1.getLevel())
                ;
    }

    private boolean canCanGatherResources(CharacterSchema character, ItemSchema itemSchema) {
        assert itemSchema.getCraft() != null;
        assert itemSchema.getCraft()
                         .getItems() != null;

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
                                                                                  .filter(resourceSchema -> resourceSchema.getDrops()
                                                                                                                          .stream()
                                                                                                                          .anyMatch(dropRateSchema -> dropRateSchema.getCode()
                                                                                                                                                                    .equalsIgnoreCase(resource.getCode())))
                                                                                  .findFirst()
                                               ;
                                       if (resourceSource.isEmpty()) {
                                           // this is no resource to gather but to craft
                                           return canCanGatherResources(character, caches.findItemDefinition(simpleItemSchema.getCode())
                                                                                         .get()
                                           );
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

    private boolean charHasEnoughSkill(CharacterSchema character, Skill skill, Integer skillLevel) {
        int skillOfChar = getSkillOfCharToCreate(character, skill);
        return skillOfChar >= skillLevel;
    }

    private int getSkillOfCharToCreate(CharacterSchema character, Skill skill) {
        return switch (skill) {
            case WEAPONCRAFTING -> character.getWeaponcraftingLevel();
            case GEARCRAFTING -> character.getGearcraftingLevel();
            case JEWELRYCRAFTING -> character.getJewelrycraftingLevel();
            case COOKING -> character.getCookingLevel();
            case WOODCUTTING -> character.getWoodcuttingLevel();
            case MINING -> character.getMiningLevel();
            case ALCHEMY -> character.getAlchemyLevel();
            case FISHING -> character.getFishingLevel();
            // noinspection UnnecessaryDefault
            default -> throw new RuntimeException("unknown skill " + skill.name());
        };
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void trainSkills(CharacterSchema character, Skill... skills) {
        Optional<ItemSchema> itemToTrain = findHighestItemThatThisCharCanCreateAlone(character, skills);
        List<SimpleItemSchema> neededForTrainingItem = itemHelper.getRecursiveResourcesToCraft(itemToTrain.get()
                                                                                                          .getCode(), 1
        );
        neededForTrainingItem = CharHelper.removeWhatIsAlreadyInInventory(character, neededForTrainingItem);
        Optional<String> itemCodeToCraft = findCraftableWithInventory(character, neededForTrainingItem);

    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Optional<String> findCraftableWithInventory(CharacterSchema character, List<SimpleItemSchema> desiredItems) {
        for (SimpleItemSchema desiredItem : desiredItems) {
            ItemSchema itemDefinition = caches.findItemDefinition(desiredItem.getCode())
                                              .get();
            if (itemDefinition.getCraft() == null) {
                // just crafts
                continue;
            }
            boolean hasAllCraftingItems = itemDefinition.getCraft()
                                                        .getItems()
                                                        .stream()
                                                        .allMatch(simpleItemSchema -> character.getInventory()
                                                                                               .stream()
                                                                                               .anyMatch(inventorySlot -> inventorySlot.getCode()
                                                                                                                                       .equalsIgnoreCase(simpleItemSchema.getCode())
                                                                                                       && inventorySlot.getQuantity() >= simpleItemSchema.getQuantity()))
                    ;
            if (hasAllCraftingItems) {
                return Optional.of(desiredItem.getCode());
            }
        }
        return Optional.empty();
    }
}
