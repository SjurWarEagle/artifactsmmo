package de.tkunkel.game.artifactsmmo;

import de.tkunkel.game.artifactsmmo.api.AccountsApiWrapper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.combat.CombatSimulator;
import de.tkunkel.game.artifactsmmo.combat.CombatStats;
import de.tkunkel.game.artifactsmmo.combat.CombatStatsEditor;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.game.artifactsmmo.helper.MonsterHelper;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CharHelper {
    private final Logger logger = LoggerFactory.getLogger(CharHelper.class.getName());

    private final ServerDetailsApiWrapper serverDetailsApi;
    private final CharactersApiWrapper charactersApi;
    private final MyCharactersApiWrapper myCharactersApi;
    private final MyAccountApiWrapper myAccountApi;
    private final ItemHelper itemHelper;
    private final AccountsApiWrapper accountsApi;
    private final CombatSimulator combatSimulator;
    private final MonsterHelper monsterHelper;
    private final Caches caches;
    private final CombatStatsEditor combatStatsEditor;

    public CharHelper(ServerDetailsApiWrapper serverDetailsApi, CharactersApiWrapper charactersApi, MyCharactersApiWrapper myCharactersApi,
                      MyAccountApiWrapper myAccountApi, Caches caches, ItemHelper itemHelper,
                      AccountsApiWrapper accountsApi,
                      CombatSimulator combatSimulator, MonsterHelper monsterHelper, CombatStatsEditor combatStatsEditor) {
        this.serverDetailsApi = serverDetailsApi;
        this.charactersApi = charactersApi;
        this.myCharactersApi = myCharactersApi;
        this.myAccountApi = myAccountApi;
        this.itemHelper = itemHelper;
        this.accountsApi = accountsApi;
        this.combatSimulator = combatSimulator;
        this.monsterHelper = monsterHelper;
        this.caches = caches;
        this.combatStatsEditor = combatStatsEditor;
    }

    public static int getSkillLevelForSkill(CharacterSchema character, Skill requiredSkill) {
        return getSkillLevelForSkill(character, requiredSkill.name());
    }

    public Optional<ItemSchema> findBestItemForSlotThatCanBeCraftedByAccount(ItemSlot slot, CharacterSchema character) {
        final CombatStats characterCombatStats = CombatStats.fromCharacter(character);

        final List<CombatStats> monsterCombatStats = caches.cachedMonsters.stream()
                                                                          .map(monsterSchema -> CombatStats.fromMonster(monsterSchema))
                                                                          .toList()
                ;
        return caches.cachedItems.stream()
                                 .filter(itemSchema -> isCorrectSlot(itemSchema, slot))
                                 .filter(itemSchema -> itemSchema.getCraft() != null)
                                 .filter(itemSchema -> itemSchema.getCraft()
                                                                 .getSkill() != null)
                                 .filter(itemSchema -> aCharCanCraftThis(itemSchema.getCraft()
                                                                                   .getSkill()
                                                                                   .name(), itemSchema.getCraft()
                                                                                                      .getLevel()
                                 ))
                                 .filter(itemSchema -> canCharEquipItem(itemSchema, character.getLevel()
                                 ))
                                 .filter(itemSchema -> canAnyCharFarmResourcesForItem(itemSchema.getCode()))
                                 .sorted((item1, item2) -> compareByKillableMonsters(item1, item2, character, characterCombatStats, monsterCombatStats))
                                 // use last of streams
                                 .reduce((o1, o2) -> o2)
                ;
    }

    private boolean aCharCanCraftThis(String requiredSkill, Integer requiredSkillLevel) {
        CharactersListSchema characters = accountsApi.getAccountCharactersAccountsAccountCharactersGet();
        for (CharacterSchema characterDatum : characters.getData()) {
            if (charHasRequiredSkillLevel(characterDatum, requiredSkill, requiredSkillLevel)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCorrectSlot(ItemSchema itemSchema, ItemSlot slot) {
        String slotName = slot.name();
        // this is because of items like rings
        slotName = slotName.replace("1", "");
        slotName = slotName.replace("2", "");
        return itemSchema.getType()
                         .equalsIgnoreCase(slotName);
    }

    private int compareByKillableMonsters(ItemSchema item1, ItemSchema item2, CharacterSchema character, CombatStats characterCombatStats,
                                          List<CombatStats> defenders) {
        ItemSlot itemSlot = ItemSlot.fromValue(item1.getType());
        Optional<ItemSchema> oldItem = getEquippedItemOfSlot(character, itemSlot);
        if (oldItem.isEmpty()) {
            return -1;
        }
        CombatStats manipulatedStats = combatStatsEditor.createManipulatedStats(characterCombatStats, oldItem.get(), item1);
        int cntMonsters1 = combatSimulator.simulateHowManyMonstersCanBeBeaten(manipulatedStats, defenders);

        itemSlot = ItemSlot.fromValue(item2.getType());
        oldItem = getEquippedItemOfSlot(character, itemSlot);
        if (oldItem.isEmpty()) {
            return -1;
        }
        manipulatedStats = combatStatsEditor.createManipulatedStats(characterCombatStats, oldItem.get(), item2);
        int cntMonsters2 = combatSimulator.simulateHowManyMonstersCanBeBeaten(manipulatedStats, defenders);
        return cntMonsters1 - cntMonsters2;

    }

    public Optional<ItemSchema> findBestToolForSkillThatCanBeCraftedByAccount(String skill, Integer level) {

        return caches.cachedItems.stream()
                                 .filter(itemSchema -> itemSchema.getEffects() != null)
                                 .filter(itemSchema -> itemSchema.getEffects()
                                                                 .stream()
                                                                 .anyMatch(effectSchema -> effectSchema.getCode()
                                                                                                       .equalsIgnoreCase(skill)))
                                 .filter(itemSchema -> {
                                     if (itemSchema.getCraft() == null) {
                                         return true;
                                     } else {
                                         if (itemSchema.getCraft()
                                                       .getSkill() == null) {
                                             // can be crated without any skill, so everyone can do it
                                             return true;
                                         }
                                         String requiredSkill = itemSchema.getCraft()
                                                                          .getSkill()
                                                                          .getValue()
                                                 ;
                                         Integer requiredSkillLevel = itemSchema.getCraft()
                                                                                .getLevel();
                                         return aCharCanCraftThis(requiredSkill, requiredSkillLevel);
                                     }
                                 })
                                 .filter(itemSchema -> itemSchema.getLevel() <= level)
                                 .max((o1, o2) -> o1.getLevel() - o2.getLevel())
                ;
    }


    private boolean canCharEquipItem(ItemSchema itemSchema, Integer charLevel) {
        if (itemSchema.getConditions() == null) {
            return true;
        }
        return itemSchema.getConditions()
                         .stream()
                         .allMatch(conditionSchema -> (conditionSchema.getOperator()
                                                                      .equals(ConditionOperator.GT)
                                 && conditionSchema.getCode()
                                                   .equalsIgnoreCase("level")
                                 && charLevel >= conditionSchema.getValue()
                         ));
    }


    public static int getSkillLevelForSkill(CharacterSchema character, String requiredSkill) {
        return switch (requiredSkill.toLowerCase()) {
            case "alchemy" -> character.getAlchemyLevel();
            case "fishing" -> character.getFishingLevel();
            case "jewelrycrafting" -> character.getJewelrycraftingLevel();
            case "woodcutting" -> character.getWoodcuttingLevel();
            case "mining" -> character.getMiningLevel();
            case "weaponcrafting" -> character.getWeaponcraftingLevel();
            case "gearcrafting" -> character.getGearcraftingLevel();
            case "cooking" -> character.getCookingLevel();
            default -> throw new RuntimeException("unknown skill: " + requiredSkill);
        };
    }

    public boolean charHasRequiredSkillLevel(CharacterSchema character, String requiredSkill, int requiredSkillLevel) {
        int charSkillLevel = getSkillLevelForSkill(character, requiredSkill);
        return charSkillLevel >= requiredSkillLevel;
    }

    public static List<SimpleItemSchema> removeWhatIsAlreadyInInventory(CharacterSchema characterData, List<SimpleItemSchema> toCraft) {
        List<SimpleItemSchema> rc = new ArrayList<>();
        for (SimpleItemSchema simpleItemSchema : toCraft) {
            Optional<InventorySlot> slotInInventory = characterData.getInventory()
                                                                   .stream()
                                                                   .filter(inventorySlot -> inventorySlot.getCode()
                                                                                                         .equalsIgnoreCase(simpleItemSchema.getCode()))
                                                                   .findFirst()
                    ;
            if (slotInInventory.isEmpty()) {
                rc.add(simpleItemSchema);
            } else {
                int alreadyThere = slotInInventory.get()
                                                  .getQuantity();
                int missing = simpleItemSchema.getQuantity() - alreadyThere;
                if (missing > 0) {
                    rc.add(new SimpleItemSchema().code(simpleItemSchema.getCode())
                                                 .quantity(missing));
                }
            }
        }
        return rc;
    }

    public boolean anyCharHasEnoughSkill(CraftSkill skill, Integer level) {
        return accountsApi.getAccountCharactersAccountsAccountCharactersGet()
                          .getData()
                          .stream()
                          .anyMatch(characterSchema -> charHasRequiredSkillLevel(characterSchema, skill.name(), level));
    }

    public boolean canACharHuntMonsterThatDropsThis(ItemSchema itemSchema) {
        List<MonsterSchema> monstersThatDropThis = monsterHelper.findMonstersThatDropThis(itemSchema.getCode());
        final List<CombatStats> characters = new ArrayList<>(accountsApi.getAccountCharactersAccountsAccountCharactersGet()
                                                                        .getData()
                                                                        .stream()
                                                                        .map(CombatStats::fromCharacter)
                                                                        .toList());

        return monstersThatDropThis.stream()
                                   .anyMatch(monsterSchema -> {
                                       CombatStats combatStatsMonster = CombatStats.fromMonster(monsterSchema);
                                       for (CombatStats character : characters) {
                                           if (combatSimulator.winMoreThanXPercentAgainst(character, combatStatsMonster, 95)) {
                                               return true;
                                           }
                                       }
                                       return false;
                                   });
    }


    public Optional<ItemSchema> getEquippedItemOfSlot(CharacterSchema character, ItemSlot itemSlot) {
        String itemCodeInSlot = switch (itemSlot) {
            case WEAPON -> character.getWeaponSlot();
            case SHIELD -> character.getShieldSlot();
            case HELMET -> character.getHelmetSlot();
            case BODY_ARMOR -> character.getBodyArmorSlot();
            case LEG_ARMOR -> character.getLegArmorSlot();
            case BOOTS -> character.getBootsSlot();
            case RING1 -> character.getRing1Slot();
            case RING2 -> character.getRing2Slot();
            case AMULET -> character.getAmuletSlot();
            case ARTIFACT1 -> character.getArtifact1Slot();
            case ARTIFACT2 -> character.getArtifact2Slot();
            case ARTIFACT3 -> character.getArtifact3Slot();
            case UTILITY1 -> character.getUtility1Slot();
            case UTILITY2 -> character.getUtility2Slot();
            case BAG -> character.getBagSlot();
            case RUNE -> character.getRuneSlot();
        };

        return caches.cachedItems.stream()
                                 .filter(itemSchema -> itemSchema.getCode()
                                                                 .equalsIgnoreCase(itemCodeInSlot))
                                 .findFirst();
    }

    public boolean moveToLocationSync(String characterName, MapSchema destination) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        return moveToLocationSync(character.getData(), destination);
    }

    public boolean moveToLocationSync(CharacterSchema character, MapSchema destination) {
        character = charactersApi.getCharacterCharactersNameGet(character.getName())
                                 .getData();
        boolean alreadyReached = destination.getX()
                                            .equals(character.getX())
                && destination.getY()
                              .equals(character.getY());
        waitUntilCooldownDone(character.getName());
        if (alreadyReached) {
            return false;
        }
        DestinationSchema destinationSchema = new DestinationSchema().x(destination.getX())
                                                                     .y(destination.getY());
        CharacterMovementResponseSchema characterMovementResponseSchema = myCharactersApi.actionMoveMyNameActionMovePost(character.getName(), destinationSchema
        );
        waitUntilCooldownDone(characterMovementResponseSchema.getData()
                                                             .getCooldown());
        return true;
    }

    public void waitUntilCooldownDone(CooldownSchema cooldown) {
        try {
            Thread.sleep(cooldown.getTotalSeconds());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitUntilCooldownDone(String characterName) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        waitUntilCooldownDone(character);
    }

    public void waitUntilCooldownDone(CharacterResponseSchema character) {
        OffsetDateTime serverTime;
        try {
            serverTime = serverDetailsApi.getServerDetailsGet()
                                         .getData()
                                         .getServerTime();
            character = charactersApi.getCharacterCharactersNameGet(character.getData()
                                                                             .getName());
            long timeToWait = character.getData()
                                       .getCooldownExpiration()
                                       .toEpochSecond() - serverTime.toEpochSecond();
            if (timeToWait > 0) {
                // logger.info("Server time: {}", serverTime);
                // logger.info("Character cooldown expiration: {}", character.getData()
                //                                                          .getCooldownExpiration()
                //);
                logger.info("Waiting for cooldown: {} seconds", timeToWait);
                Thread.sleep(timeToWait + 1);
            }
        } catch (InterruptedException e) {
            logger.error("Error waiting for cooldown", e);
            throw new RuntimeException(e);
        }
        long secondsToWait = (character.getData()
                                       .getCooldownExpiration()
                                       .toEpochSecond()) - serverTime.toEpochSecond();
        if (secondsToWait > 0) {
            // has active cooldown
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(secondsToWait + 1));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void healIfNeededSync(String characterName) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        waitUntilCooldownDone(character);
        int missingHp = character.getData()
                                 .getMaxHp() - character.getData()
                                                        .getHp();
        boolean needsHealing = missingHp > 0;
        Optional<ItemSchema> healingItem = findLowestHealingOutOfCombatItemInInventory(character.getData(), missingHp);
        while (needsHealing && healingItem.isPresent()) {
            character = charactersApi.getCharacterCharactersNameGet(characterName);
            missingHp = character.getData()
                                 .getMaxHp() - character.getData()
                                                        .getHp();
            needsHealing = missingHp > 0;
            healingItem = findLowestHealingOutOfCombatItemInInventory(character.getData(), missingHp);
            if (healingItem.isEmpty()) {
                break;
            }
            SimpleItemSchema useItem = new SimpleItemSchema().code(healingItem.get()
                                                                              .getCode())
                                                             .quantity(1);
            myCharactersApi.actionUseItemMyNameActionUsePost(characterName, useItem);
            waitUntilCooldownDone(characterName);
        }

        if (character.getData()
                     .getHp() < character.getData()
                                         .getMaxHp()) {
            // TODO to change this to also use healing items
            CharacterRestResponseSchema characterRestResponseSchema = myCharactersApi.actionRestMyNameActionRestPost(character.getData()
                                                                                                                              .getName());
            waitUntilCooldownDone(characterRestResponseSchema.getData()
                                                             .getCooldown());
        }
    }

    private Optional<ItemSchema> findLowestHealingOutOfCombatItemInInventory(CharacterSchema character, int maxHealing) {
        return character.getInventory()
                        .stream()
                        .filter(inventorySlot -> inventorySlot.getQuantity() > 0)
                        .map(inventorySlot -> itemHelper.findItemDefinition(inventorySlot.getCode())
                                                        .get())
                        .filter(item -> itemHelper.isHealingOutOfCombatItem(item))
                        .filter(item -> {
                                    int heal = itemHelper.getHealAmount(item);
                                    return heal <= maxHealing;
                                }
                        )
                        .sorted((o1, o2) -> {
                            int heal1 = itemHelper.getHealAmount(o1);
                            int heal2 = itemHelper.getHealAmount(o2);
                            return Integer.compare(heal2, heal1);
                        })
                        .findFirst();
    }

    public int cntItemsInBank(String itemCode) {
        DataPageSimpleItemSchema bankItemsGet = myAccountApi.getBankItemsMyBankItemsGet(itemCode, 1, 100);
        return bankItemsGet.getData()
                           .stream()
                           .mapToInt(SimpleItemSchema::getQuantity)
                           .sum();
    }

    public int cntItemsInInventory(String characterName, String itemCode) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        return character.getData()
                        .getInventory()
                        .stream()
                        .filter(itemSchema -> itemSchema.getCode()
                                                        .equals(itemCode))
                        .mapToInt(inventorySlot -> inventorySlot.getQuantity())
                        .sum();
    }

    public boolean checkIfEquipped(String gear, ItemSlot itemSlot, CharacterSchema character) {
        return switch (itemSlot) {
            case BOOTS -> character.getBootsSlot()
                                   .equalsIgnoreCase(gear)
            ;
            case SHIELD -> character.getShieldSlot()
                                    .equalsIgnoreCase(gear)
            ;
            case HELMET -> character.getHelmetSlot()
                                    .equalsIgnoreCase(gear)
            ;
            case WEAPON -> character.getWeaponSlot()
                                    .equalsIgnoreCase(gear)
            ;
            case BODY_ARMOR -> character.getBodyArmorSlot()
                                        .equalsIgnoreCase(gear)
            ;
            case LEG_ARMOR -> character.getLegArmorSlot()
                                       .equalsIgnoreCase(gear)
            ;
            case RING1 -> character.getRing1Slot()
                                   .equalsIgnoreCase(gear)
            ;
            case RING2 -> character.getRing2Slot()
                                   .equalsIgnoreCase(gear)
            ;
            case AMULET -> character.getAmuletSlot()
                                    .equalsIgnoreCase(gear)
            ;
            case ARTIFACT1 -> character.getArtifact1Slot()
                                       .equalsIgnoreCase(gear)
            ;
            case ARTIFACT2 -> character.getArtifact2Slot()
                                       .equalsIgnoreCase(gear)
            ;
            case ARTIFACT3 -> character.getArtifact3Slot()
                                       .equalsIgnoreCase(gear)
            ;
            case UTILITY1 -> character.getUtility1Slot()
                                      .equalsIgnoreCase(gear)
            ;
            case UTILITY2 -> character.getUtility2Slot()
                                      .equalsIgnoreCase(gear)
            ;
            case BAG -> character.getBagSlot()
                                 .equalsIgnoreCase(gear)
            ;
            case RUNE -> character.getRuneSlot()
                                  .equalsIgnoreCase(gear)
            ;
            default -> throw new RuntimeException("unknown slot " + itemSlot);
        };
    }

    private boolean canAnyCharFarmResourcesForItem(String itemCode) {
        ItemSchema itemSchema = itemHelper.findItemDefinition(itemCode)
                                          .get();
        boolean isCraftable = itemSchema.getCraft() != null;
        boolean hasResources = isCraftable && itemSchema.getCraft()
                                                        .getItems() != null;
        return isCraftable
                && hasResources
                && itemSchema.getCraft()
                             .getItems()
                             .stream()
                             .allMatch(simpleItemSchema -> {
                                 ItemSchema resourceItem = itemHelper.findItemDefinition(simpleItemSchema.getCode())
                                                                     .get();
                                 if (resourceItem.getType()
                                                 .equalsIgnoreCase("resource")
                                         && resourceItem.getSubtype()
                                                        .equalsIgnoreCase("mob")) {
                                     // it needs a monster drop check if we have a hunter
                                     return canACharHuntMonsterThatDropsThis(resourceItem);
                                 } else {
                                     // it needs a farmable resource, do we have a gather for it?
                                     if (resourceItem.getCraft() == null) {
                                         return true;
                                     }
                                     return anyCharHasEnoughSkill(resourceItem.getCraft()
                                                                              .getSkill(), resourceItem.getCraft()
                                                                                                       .getLevel()
                                     );
                                 }
                             })
                ;
    }


    /**
     * find an item that can be crafted with the items in inventory and skill of the char.
     * Use highest level
     */
    public Optional<String> findPossibleItemToCraft(CharacterSchema character) {
        return caches.cachedItems.stream()
                                 .filter(
                                         item -> item.getCraft() != null)
                                 .filter(
                                         item -> item.getCraft()
                                                     .getSkill() != null)
                                 .filter(item -> {
                                     String requiredSkill = item.getCraft()
                                                                .getSkill()
                                                                .getValue()
                                             ;
                                     int requiredSkillLevel = item.getCraft()
                                                                  .getLevel();
                                     return charHasRequiredSkillLevel(character, requiredSkill, requiredSkillLevel);
                                 })
                                 .sorted(Comparator.comparingInt(o -> o.getCraft()
                                                                       .getLevel()))
                                 .filter(item -> hasAllItemsInInventory(character, item.getCraft()
                                                                                       .getItems()
                                 ))
                                 .map(ItemSchema::getCode)
                                 .findFirst()
                ;
    }

    public boolean hasAllItemsInInventory(CharacterSchema character, List<SimpleItemSchema> items) {
        for (SimpleItemSchema requiredItem : items) {
            if (cntSpecificItemsInInventory(character, requiredItem.getCode()) < requiredItem.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    public int cntAllItemsInInventory(String characterName) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        return character.getInventory()
                        .stream()
                        .mapToInt(InventorySlot::getQuantity)
                        .sum();
    }

    public int cntSpecificItemsInInventory(CharacterSchema character, String itemCode) {
        return character.getInventory()
                        .stream()
                        .filter(inventorySlot -> inventorySlot.getCode()
                                                              .equals(itemCode))
                        .mapToInt(InventorySlot::getQuantity)
                        .sum();
    }


    public void equipGearIfNotEquipped(String characterName, String gear, ItemSlot itemSlot) {
        EquipSchema equipSchema = new EquipSchema().slot(itemSlot)
                                                   .code(gear);
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        waitUntilCooldownDone(characterName);
        boolean alreadyEquipped = checkIfEquipped(gear, itemSlot, character);
        if (alreadyEquipped) {
            return;
        }
        boolean hasInInventory = cntSpecificItemsInInventory(character, gear) > 0;
        if (!hasInInventory) {
            return;
        }
        myCharactersApi.actionEquipItemMyNameActionEquipPost(character.getName(), equipSchema
        );
    }

    public boolean checkIfEquipped(String characterName, String gear, ItemSlot itemSlot) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        return checkIfEquipped(gear, itemSlot, character);
    }

}
