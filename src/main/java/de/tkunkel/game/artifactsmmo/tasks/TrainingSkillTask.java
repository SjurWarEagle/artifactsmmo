package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.games.artifactsmmo.model.*;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class TrainingSkillTask {
    private final Caches caches;
    private final ItemHelper itemHelper;
    private final CraftItemTask craftItemTask;
    private final BankDepositSingleItemTask bankDepositSingleItemTask;
    private final CharHelper characterHelper;
    private final MyCharactersApiWrapper myCharactersApi;
    private final BankFetchItemTask bankFetchItemsAndCraftTask;
    private final CharactersApiWrapper charactersApi;

    public TrainingSkillTask(Caches caches, ItemHelper itemHelper, CraftItemTask craftItemTask,
                             BankDepositSingleItemTask bankDepositSingleItemTask, CharHelper characterHelper,
                             MyCharactersApiWrapper myCharactersApi, BankFetchItemTask bankFetchItemsAndCraftTask,
                             CharactersApiWrapper charactersApi) {
        this.caches = caches;
        this.itemHelper = itemHelper;
        this.craftItemTask = craftItemTask;
        this.bankDepositSingleItemTask = bankDepositSingleItemTask;
        this.characterHelper = characterHelper;
        this.myCharactersApi = myCharactersApi;
        this.bankFetchItemsAndCraftTask = bankFetchItemsAndCraftTask;
        this.charactersApi = charactersApi;
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
                                 .filter(itemSchema -> itemSchema.getCraft() != null)
                                 .filter(itemSchema -> itemSchema.getCraft()
                                                                 .getSkill()
                                                                 .getValue()
                                                                 .equalsIgnoreCase(skillToTrain.get()
                                                                                               .getValue()))
                                 .filter(itemSchema -> {
                                     Integer neededLevel = itemSchema.getCraft()
                                                                     .getLevel();

                                     boolean isRelevantSkill = itemSchema.getCraft()
                                                                         .getSkill()
                                                                         .name()
                                                                         .equalsIgnoreCase(skillToTrain.get()
                                                                                                       .name())
                                             ;
                                     boolean charHasEnoughSkill = charHasEnoughSkill(character, skillToTrain.get(), neededLevel);
                                     return isRelevantSkill && charHasEnoughSkill;

                                 })
                                 .filter(itemSchema -> canCanGatherResources(character, itemSchema))
                                 .sorted(this::sortByNeededResource)
                                 .findFirst()
                ;
    }

    private int sortByNeededResource(ItemSchema o1, ItemSchema o2) {
        int level1 = o1.getLevel();
        int level2 = o2.getLevel();
        if (level1 != level2) {
            return Integer.compare(level1, level2);
        }
        if (o1.getCraft() == null
                || o1.getCraft()
                     .getItems() == null) {
            return -1;
        }
        if (o2.getCraft() == null
                || o2.getCraft()
                     .getItems() == null) {
            return -1;
        }

        int needed1 = o1.getCraft()
                        .getItems()
                        .stream()
                        .mapToInt(item -> {
                            List<SimpleItemSchema> neededItems = itemHelper.getRecursiveResourcesToCraft(item.getCode(), 1);
                            return neededItems.stream()
                                              .mapToInt(SimpleItemSchema::getQuantity)
                                              .sum() * item.getQuantity();
                        })
                        .sum()
                ;

        int needed2 = o2.getCraft()
                        .getItems()
                        .stream()
                        .mapToInt(item -> {
                            List<SimpleItemSchema> neededItems = itemHelper.getRecursiveResourcesToCraft(item.getCode(), 1);
                            return neededItems.stream()
                                              .mapToInt(SimpleItemSchema::getQuantity)
                                              .sum() * item.getQuantity();
                        })
                        .sum()
                ;

        if (needed1 != needed2) {
            return Integer.compare(needed1, needed2);
        }
        return o1.getName()
                 .compareTo(o2.getName());
    }

    private boolean canCanGatherResources(CharacterSchema character, ItemSchema itemSchema) {
        if (itemSchema.getCraft() == null ||
                itemSchema.getCraft()
                          .getItems() == null) {
            // TODO check if returning false here is correct
            return false;
        }

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
                                           return canCanGatherResources(character, itemHelper.findItemDefinition(simpleItemSchema.getCode())
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

    public void trainSkills(CharacterSchema character, Skill... skills) {
        Optional<ItemSchema> itemToTrain = findHighestItemThatThisCharCanCreateAlone(character, skills);
        if (itemToTrain.isEmpty()) {
            return;
        }
        // This is needed to have a loop, if we do not get rid of the item it is detected as "already crafted" and therefore skipped, no training would be done.
        bankDepositSingleItemTask.depositInventoryInBank(character.getName(), itemToTrain.get()
                                                                                         .getCode()
        );

        List<SimpleItemSchema> neededForTrainingItem = itemHelper.getRecursiveResourcesToCraft(itemToTrain.get()
                                                                                                          .getCode(), 1
        );
        neededForTrainingItem = CharHelper.removeWhatIsAlreadyInInventory(character, neededForTrainingItem);
        //       bankFetchItemsAndCraftTask.fetchItemFromBank(character, "copper_bar", 6);

        character = charactersApi.getCharacterCharactersNameGet(character.getName())
                                 .getData();

        Optional<String> itemCodeCraftableWithInventory;
        if (canCraftItem(character, itemToTrain.get())) {
            itemCodeCraftableWithInventory = Optional.of(itemToTrain.get()
                                                                    .getCode());
        } else {
            itemCodeCraftableWithInventory = findCraftableWithInventory(character, neededForTrainingItem);
        }
        if (itemCodeCraftableWithInventory.isPresent()) {
            craftItemTask.craftItem(character.getName(), itemCodeCraftableWithInventory.get());
        } else {
            Optional<String> farmableItemCode = findFarmableItem(neededForTrainingItem);
            if (farmableItemCode.isPresent()) {
                MapSchema whereToGather = itemHelper.findLocationWhereToFarm(character, farmableItemCode.get());
                characterHelper.moveToLocationSync(character.getName(), whereToGather);
                characterHelper.waitUntilCooldownDone(character.getName());
                myCharactersApi.actionGatheringMyNameActionGatheringPost(character.getName());
                characterHelper.waitUntilCooldownDone(character.getName());
            }
        }
    }

    private boolean canCraftItem(CharacterSchema character, ItemSchema itemSchema) {
        return itemSchema.getCraft()
                         .getItems()
                         .stream()
                         .allMatch(simpleItemSchema -> {
                             return
                                     character.getInventory()
                                              .stream()
                                              .anyMatch(inventorySlot -> inventorySlot.getCode()
                                                                                      .equalsIgnoreCase(simpleItemSchema.getCode()) && inventorySlot.getQuantity() >= simpleItemSchema.getQuantity());
                         });
    }

    private Optional<String> findFarmableItem(List<SimpleItemSchema> neededForTrainingItem) {
        return neededForTrainingItem.stream()
                                    .map(simpleItemSchema -> itemHelper.findItemDefinition(simpleItemSchema.getCode())
                                                                       .get())
                                    .filter(itemSchema -> itemSchema.getCraft() == null)
                                    .sorted(Comparator.comparing(ItemSchema::getName))
                                    .map(ItemSchema::getCode)
                                    .findFirst()
                ;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Optional<String> findCraftableWithInventory(CharacterSchema character, List<SimpleItemSchema> desiredItems) {
        return desiredItems.stream()
                           .map(desiredItem -> itemHelper.findItemDefinition(desiredItem.getCode())
                                                         .get())
                           .filter(desiredItem -> desiredItem.getCraft() != null)
                           .filter(desiredItem -> desiredItem.getCraft()
                                                             .getItems()
                                                             .stream()
                                                             .allMatch(simpleItemSchema -> character.getInventory()
                                                                                                    .stream()
                                                                                                    .anyMatch(inventorySlot -> inventorySlot.getCode()
                                                                                                                                            .equalsIgnoreCase(simpleItemSchema.getCode())
                                                                                                            && inventorySlot.getQuantity() >= simpleItemSchema.getQuantity())))
                           .map(ItemSchema::getCode)
                           .findFirst();
    }
}
