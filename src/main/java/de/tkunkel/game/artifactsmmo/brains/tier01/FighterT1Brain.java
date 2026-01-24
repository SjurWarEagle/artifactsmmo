package de.tkunkel.game.artifactsmmo.brains.tier01;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.combat.CombatSimulator;
import de.tkunkel.game.artifactsmmo.combat.CombatStats;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.game.artifactsmmo.tasks.*;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class FighterT1Brain {
    private final Logger logger = LoggerFactory.getLogger(FighterT1Brain.class.getName());
    private final BankUpgradeIfPossibleTask bankUpgradeIfPossibleTask;
    private final BankDepositGoldIfRichTask bankDepositGoldIfRichTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final CombatSimulator combatSimulator;
    private final TaskCancelTask taskCancelTask;
    private final CookingTask cookingTask;
    private final CharactersApiWrapper charactersApi;
    private final CharHelper charHelper;
    private final GetBestItemForSlotTask getBestItemForSlot;
    private final MyCharactersApiWrapper myCharactersApi;
    private final Caches caches;
    private final MapHelper mapHelper;

    public FighterT1Brain(BankUpgradeIfPossibleTask bankUpgradeIfPossibleTask, BankDepositGoldIfRichTask bankDepositGoldIfRichTask,
                          BankDepositAllTask bankDepositAllTask,
                          CombatSimulator combatSimulator, TaskCancelTask taskCancelTask, CookingTask cookingTask,
                          CharactersApiWrapper charactersApi, CharHelper charHelper, GetBestItemForSlotTask getBestItemForSlot, MyCharactersApiWrapper myCharactersApi,
                          Caches caches, MapHelper mapHelper) {
        this.bankUpgradeIfPossibleTask = bankUpgradeIfPossibleTask;
        this.bankDepositGoldIfRichTask = bankDepositGoldIfRichTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.combatSimulator = combatSimulator;
        this.taskCancelTask = taskCancelTask;
        this.cookingTask = cookingTask;
        this.charactersApi = charactersApi;
        this.charHelper = charHelper;
        this.getBestItemForSlot = getBestItemForSlot;
        this.myCharactersApi = myCharactersApi;
        this.caches = caches;
        this.mapHelper = mapHelper;
    }

    public void runBaseLoop(String characterName) {
        CharacterSchema character = charactersApi.getCharacterCharactersNameGet(characterName)
                                                 .getData();
        charHelper.waitUntilCooldownDone(character.getName());
        bankDepositGoldIfRichTask.depositGoldInBank(character);
        bankUpgradeIfPossibleTask.perform(character);
        depositNonFoodAtBankIfInventoryIsFull(character);
        cookingTask.cookFoodIfHaveSome(character);
        charHelper.healIfNeededSync(character.getName());
        getBestItemForSlot.equipOrRequestBestWeapon(characterName);

        completeCurrentTaskIfDone(character);
        cancelCurrentTaskIfTooHard(character);
        bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(character);

        charHelper.waitUntilCooldownDone(character.getName());

        character = charactersApi.getCharacterCharactersNameGet(characterName)
                                 .getData();
        String enemyToHunt = decideWhatEnemyToHunt(character);
        Optional<MapSchema> locationOfClosestMonster = mapHelper.findLocationOfClosestMonster(character, enemyToHunt);
        if (locationOfClosestMonster.isEmpty()) {
            logger.error("Could not find location of closest monster ({})", enemyToHunt);
            return;
        }
        charHelper.moveToLocationSync(character, locationOfClosestMonster.get());

        charHelper.waitUntilCooldownDone(character.getName());
        FightRequestSchema fightRequest = new FightRequestSchema();
        myCharactersApi.actionFightMyNameActionFightPost(character.getName(), fightRequest
        );
    }

    private void cancelCurrentTaskIfTooHard(CharacterSchema character) {
        var task = character.getTask();
        if ("".equalsIgnoreCase(task)) {
            return;
        }
        if (!"monsters".equalsIgnoreCase(character.getTaskType())) {
            // not a killing task
            return;
        }
        CombatStats attacker = CombatStats.fromCharacter(character);
        MonsterSchema monster = caches.cachedMonsters.stream()
                                                     .filter(monsterSchema -> monsterSchema.getCode()
                                                                                           .equals(character.getTask()))
                                                     .findFirst()
                                                     .get()
                ;
        CombatStats defender = CombatStats.fromMonster(monster);
        if (combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 95)) {
            return;
        }
        logger.info("Too hard, canceling task");
        taskCancelTask.perform(character.getName());
    }

    private void depositNonFoodAtBankIfInventoryIsFull(CharacterSchema character) {
        int inventoryUsed = charHelper.cntAllItemsInInventory(character.getName());
        // store if more than 75% are used
        if (inventoryUsed <= character.getInventoryMaxItems() * 0.75) {
            return;
        }
        Optional<MapSchema> bank = mapHelper.findClosestLocation(character, "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getName());
        }
        charHelper.moveToLocationSync(character, bank.get());
        charHelper.waitUntilCooldownDone(character.getName());
        List<SimpleItemSchema> bankRequestSchema = character.getInventory()
                                                            .stream()
                                                            .filter(inventorySlot -> {
                                                                List<ItemSchema> food = caches.cachedItems.stream()
                                                                                                          .filter(itemSchema -> itemSchema.getCode()
                                                                                                                                          .equals(inventorySlot.getCode()))
                                                                                                          .filter(itemSchema -> !itemSchema.getSubtype()
                                                                                                                                           .equals("food"))
                                                                                                          .toList()
                                                                        ;
                                                                return !food.isEmpty();
                                                            })
                                                            .map(inventorySlot -> new SimpleItemSchema().code(inventorySlot.getCode())
                                                                                                        .quantity(inventorySlot.getQuantity()))
                                                            .toList()
                ;
        myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getName(), bankRequestSchema
        );
    }

    private void completeCurrentTaskIfDone(CharacterSchema character) {
        if (!TaskType.MONSTERS.getValue()
                              .equals(character.getTaskType())
                || character.getTaskProgress() < character.getTaskTotal()) {
            return;
        }
        Optional<MapSchema> closestLocation = mapHelper.findClosestLocation(character, "monsters");
        if (closestLocation.isEmpty()) {
            return;
        }
        boolean moved = charHelper.moveToLocationSync(character, closestLocation.get());
        if (moved) {
            return;
        }
        myCharactersApi.actionCompleteTaskMyNameActionTaskCompletePost(character.getName());
    }


    private String decideWhatEnemyToHunt(CharacterSchema character) {
        String monsterToHunt = null;
        if (TaskType.MONSTERS.getValue()
                             .equals(character.getTaskType())
                && character.getTaskProgress() < character.getTaskTotal()) {
            monsterToHunt = character.getTask();
        }
        if (monsterToHunt == null) {
            monsterToHunt = findHighestMonsterToHunt(character);
        }

        String finalMonsterToHunt = monsterToHunt;
        var monster = caches.cachedMonsters.stream()
                                           .filter(monsterSchema -> monsterSchema.getCode()
                                                                                 .equals(finalMonsterToHunt))
                                           .findFirst()
                                           .get()
                ;
        boolean canBeat = combatSimulator.winMoreThanXPercentAgainst(CombatStats.fromCharacter(character), CombatStats.fromMonster(monster), 95);
        if (!canBeat) {
            logger.warn("Monster {} is too strong for character {}, using fallback.", monsterToHunt, character.getName()
            );

            CombatStats attacker = CombatStats.fromCharacter(character);
            List<MonsterSchema> monsters = caches.cachedMonsters.stream()
                                                                .filter(monsterSchema -> {
                                                                    CombatStats defender = CombatStats.fromMonster(monsterSchema);
                                                                    return combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 95);
                                                                })
                                                                .toList()
                    ;
            logger.info("Monsters that can be hunted: {}", monsters.stream()
                                                                   .map(MonsterSchema::getName)
                                                                   .toList()
            );
            if (monsters.isEmpty()) {
                logger.warn("No monsters that can be hunted found for character {}", character.getName()
                );
                monsterToHunt = "chicken";
            } else {
                monsterToHunt = monsters.stream()
                                        .sorted(Comparator.comparingInt(MonsterSchema::getLevel))
                                        // use last of streams
                                        .reduce((o1, o2) -> o2)
                                        .get()
                                        .getCode();
            }
        }

        return monsterToHunt;
    }

    private String findHighestMonsterToHunt(CharacterSchema character) {
        CombatStats charCombatStats = CombatStats.fromCharacter(character);

        String rc = "green_slime";
        return rc;
    }

}
