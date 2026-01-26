package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
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
    private final BankDepositAllTask bankDepositAllTask;
    private final CharHelper characterHelper;
    private final CharactersApiWrapper charactersApi;
    private final HarvestResourceTask harvestResourceTask;
    private final BankFetchItemTask bankFetchItemTask;

    public TrainingSkillTask(Caches caches, ItemHelper itemHelper, CraftItemTask craftItemTask,
                             BankDepositSingleItemTask bankDepositSingleItemTask, BankDepositAllTask bankDepositAllTask, CharHelper characterHelper,
                             CharactersApiWrapper charactersApi, HarvestResourceTask harvestResourceTask, BankFetchItemTask bankFetchItemTask) {
        this.caches = caches;
        this.itemHelper = itemHelper;
        this.craftItemTask = craftItemTask;
        this.bankDepositSingleItemTask = bankDepositSingleItemTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.characterHelper = characterHelper;
        this.charactersApi = charactersApi;
        this.harvestResourceTask = harvestResourceTask;
        this.bankFetchItemTask = bankFetchItemTask;
    }

    public Optional<ItemSchema> findHighestItemCraftableByCharWithBank(CharacterSchema character, Skill... skills) {
        Optional<Skill> skillToTrain = Arrays.stream(skills)
                                             .min((o1, o2) -> {
                                                 int skillLevel1 = CharHelper.getSkillLevelForSkill(character, o1);
                                                 int skillLevel2 = CharHelper.getSkillLevelForSkill(character, o2);
                                                 return skillLevel1 - skillLevel2;
                                             });
        if (skillToTrain.isEmpty()) {
            throw new RuntimeException("No skill to train found");
        }

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
                                     boolean charHasEnoughSkill = characterHelper.charHasEnoughSkill(character, skillToTrain.get(), neededLevel);
                                     return isRelevantSkill && charHasEnoughSkill;

                                 })
                                 .filter(itemSchema -> resouresAreInBank(character, itemSchema))
                                 .sorted((itemSchema1, itemSchema2) -> {
                                     int level1 = itemSchema1.getLevel();
                                     int level2 = itemSchema2.getLevel();
                                     return Integer.compare(level2, level1);
                                 })
                                 .findFirst()
                ;
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
                                     boolean charHasEnoughSkill = characterHelper.charHasEnoughSkill(character, skillToTrain.get(), neededLevel);
                                     return isRelevantSkill && charHasEnoughSkill;

                                 })
                                 .filter(itemSchema -> characterHelper.canCanGatherResources(character, itemSchema))
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

    private boolean resouresAreInBank(CharacterSchema character, ItemSchema itemSchema) {
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
                                       boolean canGatherItem = canCanGatherItem(simpleItemSchema.getCode());

                                       int cntInBank = characterHelper.cntItemsInBank(simpleItemSchema.getCode());
                                       return cntInBank >= simpleItemSchema.getQuantity();
                                   }
                         )
                ;
    }

    private boolean canCanGatherItem(String code) {
        return false;
    }

    public void trainSkillsWithBankItems(CharacterSchema character, Skill... skills) {
        Optional<ItemSchema> itemToTrain = findHighestItemCraftableByCharWithBank(character, skills);
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
                MapSchema whereToGather = itemHelper.findLocationWhereToFarm(character, farmableItemCode.get())
                                                    .get();
                harvestResourceTask.farmResourceWithTool(character.getName(), whereToGather);
                characterHelper.waitUntilCooldownDone(character.getName());
            }
        }
    }

    public void trainSkills(CharacterSchema character, Skill... skills) {
        Optional<ItemSchema> itemToTrain = findHighestItemCraftableByCharWithBank(character, skills);
        // Optional<ItemSchema> itemToTrain = findHighestItemThatThisCharCanCreateAlone(character, skills);
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
        // FIXME neededForTrainingItem contains all items, so also the ore if you need bars, this is not needed, we need it to craft butnot for the final item. maybe filter them ojut.
        // FIXME also I forgot to add logic to fetch the items that the char cannot gather from bank!
        neededForTrainingItem = CharHelper.removeWhatIsAlreadyInInventory(character, neededForTrainingItem);
        var itemsNotInInventory = CharHelper.removeWhatIsAlreadyInInventory(character, neededForTrainingItem);
        //       bankFetchItemsAndCraftTask.fetchItemFromBank(character, "copper_bar", 6);

        character = charactersApi.getCharacterCharactersNameGet(character.getName())
                                 .getData();

        fetchItemsThatAreMissingFromInventory(character, itemsNotInInventory, itemToTrain.get());

        Optional<String> itemCodeCraftableWithInventory;
        if (canCraftItem(character, itemToTrain.get())) {
            itemCodeCraftableWithInventory = Optional.of(itemToTrain.get()
                                                                    .getCode());
        } else {
            itemCodeCraftableWithInventory = findCraftableWithInventory(character, itemsNotInInventory);
        }
        if (itemCodeCraftableWithInventory.isPresent()) {
            craftItemTask.craftItem(character.getName(), itemCodeCraftableWithInventory.get());
            characterHelper.waitUntilCooldownDone(character.getName());
            bankDepositAllTask.depositInventoryInBank(character.getName());
            characterHelper.waitUntilCooldownDone(character.getName());
        } else {
            Optional<String> farmableItemCode = findFarmableItem(itemsNotInInventory);
            if (farmableItemCode.isPresent()) {
                Optional<MapSchema> whereToGather = itemHelper.findLocationWhereToFarm(character, farmableItemCode.get());
                if (whereToGather.isEmpty()) {
                    throw new RuntimeException("Could not find location to farm " + farmableItemCode.get());
                }
                harvestResourceTask.farmResourceWithTool(character.getName(), whereToGather.get());
                characterHelper.waitUntilCooldownDone(character.getName());
            }
        }
    }

    private void fetchItemsThatAreMissingFromInventory(CharacterSchema character, List<SimpleItemSchema> itemsNotInInventory, ItemSchema itemToTrain) {
        for (SimpleItemSchema simpleItemSchema : itemsNotInInventory) {
            if (simpleItemSchema.getCode()
                                .equalsIgnoreCase(itemToTrain.getCode())) {
                continue;
            }
            if (characterHelper.cntItemsInBank(simpleItemSchema.getCode()) >= simpleItemSchema.getQuantity()) {
                bankFetchItemTask.fetchItemFromBank(character, simpleItemSchema.getCode(), simpleItemSchema.getQuantity());
                characterHelper.waitUntilCooldownDone(character.getName());
            }
        }
    }

    private boolean canCraftItem(CharacterSchema character, ItemSchema itemSchema) {
        return itemSchema.getCraft()
                         .getItems()
                         .stream()
                         .filter(simpleItemSchema -> character.getInventory() != null)
                         .allMatch(simpleItemSchema -> character.getInventory()
                                                                .stream()
                                                                .anyMatch(inventorySlot -> inventorySlot.getCode()
                                                                                                        .equalsIgnoreCase(simpleItemSchema.getCode()) && inventorySlot.getQuantity() >= simpleItemSchema.getQuantity()));
    }

    private Optional<String> findFarmableItem(List<SimpleItemSchema> neededForTrainingItem) {
        return neededForTrainingItem.stream()
                                    .map(simpleItemSchema -> itemHelper.findItemDefinition(simpleItemSchema.getCode())
                                                                       .get())
                                    .filter(itemSchema -> itemSchema.getCraft() == null)
                                    .filter(itemSchema -> !itemSchema.getSubtype()
                                                                     .equalsIgnoreCase("mob"))
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
