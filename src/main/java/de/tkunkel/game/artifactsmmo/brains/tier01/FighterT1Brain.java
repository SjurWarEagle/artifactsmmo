package de.tkunkel.game.artifactsmmo.brains.tier01;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.combat.CombatSimulator;
import de.tkunkel.game.artifactsmmo.combat.CombatStats;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
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
    private final TaskAcceptNewTask taskAcceptNewTask;
    private final CharactersApiWrapper charactersApi;
    private final CharHelper charHelper;
    private final GetBestItemForSlotTask getBestItemForSlot;
    private final MyCharactersApiWrapper myCharactersApi;
    private final Caches caches;
    private final MapHelper mapHelper;

    public FighterT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder,
                          BankUpgradeIfPossibleTask bankUpgradeIfPossibleTask, BankDepositGoldIfRichTask bankDepositGoldIfRichTask,
                          BankDepositAllTask bankDepositAllTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask,
                          ItemHelper itemHelper, CombatSimulator combatSimulator, TaskCancelTask taskCancelTask, CookingTask cookingTask,
                          TaskAcceptNewTask taskAcceptNewTask, CharHelper charHelper, MapHelper mapHelper, CharactersApiWrapper charactersApi, CharHelper charHelper1, GetBestItemForSlotTask getBestItemForSlot, MyCharactersApiWrapper myCharactersApi, Caches caches1, MapHelper mapHelper1) {
        this.bankUpgradeIfPossibleTask = bankUpgradeIfPossibleTask;
        this.bankDepositGoldIfRichTask = bankDepositGoldIfRichTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.combatSimulator = combatSimulator;
        this.taskCancelTask = taskCancelTask;
        this.cookingTask = cookingTask;
        this.taskAcceptNewTask = taskAcceptNewTask;
        this.charactersApi = charactersApi;
        this.charHelper = charHelper1;
        this.getBestItemForSlot = getBestItemForSlot;
        this.myCharactersApi = myCharactersApi;
        this.caches = caches1;
        this.mapHelper = mapHelper1;
    }

    public void runBaseLoop(String characterName) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        charHelper.waitUntilCooldownDone(character);
        bankDepositGoldIfRichTask.depositInventoryInBankIfInventoryIsFull(character);
        bankUpgradeIfPossibleTask.perform(character);
        depositNonFoodAtBankIfInventoryIsFull(character);
        cookingTask.cookFoodIfHaveSome(character);
        charHelper.healIfNeededSync(character.getData()
                                             .getName());
        getBestItemForSlot.equipOrRequestBestWeapon(characterName);

        completeCurrentTaskIfDone(character);
        cancelCurrentTaskIfTooHard(character);
        // taskAcceptNewTask.getNewTaskIfCurrentTaskIsDone(this, character);
        bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(character);

        charHelper.waitUntilCooldownDone(character);

        character = charactersApi.getCharacterCharactersNameGet(characterName);
        String enemyToHunt = decideWhatEnemyToHunt(character);
        Optional<MapSchema> locationOfClosestMonster = mapHelper.findLocationOfClosestMonster(character, enemyToHunt);
        if (locationOfClosestMonster.isEmpty()) {
            logger.error("Could not find location of closest monster ({})", enemyToHunt);
            return;
        }
        charHelper.moveToLocationSync(character.getData(), locationOfClosestMonster.get());

        charHelper.waitUntilCooldownDone(character);
        FightRequestSchema fightRequest = new FightRequestSchema();
        myCharactersApi.actionFightMyNameActionFightPost(character.getData()
                                                                  .getName(), fightRequest
        );
    }

    private void cancelCurrentTaskIfTooHard(CharacterResponseSchema character) {
        var task = character.getData()
                            .getTask();
        if (task == null || "".equalsIgnoreCase(task)) {
            return;
        }
        if (!"monsters".equalsIgnoreCase(character.getData()
                                                  .getTaskType())) {
            // not a killing task
            return;
        }
        if (character.getData()
                     .getTaskTotal() <= character.getData()
                                                 .getTaskProgress()) {
            // already done
        }
        CombatStats attacker = CombatStats.fromCharacter(character.getData());
        MonsterSchema monster = caches.cachedMonsters.stream()
                                                     .filter(monsterSchema -> monsterSchema.getCode()
                                                                                           .equals(character.getData()
                                                                                                            .getTask()))
                                                     .findFirst()
                                                     .get()
                ;
        CombatStats defender = CombatStats.fromMonster(monster);
        if (combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 95)) {
            return;
        }
        logger.info("Too hard, canceling task");
        taskCancelTask.perform(character.getData()
                                        .getName());
    }

    private void depositNonFoodAtBankIfInventoryIsFull(CharacterResponseSchema character) {
        int inventoryUsed = charHelper.cntAllItemsInInventory(character);
        // store if more than 75% are used
        if (inventoryUsed <= character.getData()
                                      .getInventoryMaxItems() * 0.75) {
            return;
        }
        Optional<MapSchema> bank = mapHelper.findClosestLocation(character.getData(), "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getData()
                                                                                       .getName());
        }
        charHelper.moveToLocationSync(character.getData(), bank.get());
        charHelper.waitUntilCooldownDone(character);
        List<SimpleItemSchema> bankRequestSchema = character.getData()
                                                            .getInventory()
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
        myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getData()
                                                                                      .getName(), bankRequestSchema
        );
    }

    private void completeCurrentTaskIfDone(CharacterResponseSchema character) {
        if (character.getData()
                     .getTask() == null || !TaskType.MONSTERS.getValue()
                                                             .equals(character.getData()
                                                                              .getTaskType()) || character.getData()
                                                                                                          .getTaskProgress() < character.getData()
                                                                                                                                        .getTaskTotal()) {
            return;
        }
        Optional<MapSchema> closestLocation = mapHelper.findClosestLocation(character.getData(), "monsters");
        if (closestLocation.isEmpty()) {
            return;
        }
        boolean moved = charHelper.moveToLocationSync(character.getData(), closestLocation.get());
        if (moved) {
            return;
        }
        myCharactersApi.actionCompleteTaskMyNameActionTaskCompletePost(character.getData()
                                                                                .getName());
    }


    private String decideWhatEnemyToHunt(CharacterResponseSchema character) {
        String monsterToHunt = null;
        if (character.getData()
                     .getTask() != null && TaskType.MONSTERS.getValue()
                                                            .equals(character.getData()
                                                                             .getTaskType()) && character.getData()
                                                                                                         .getTaskProgress() < character.getData()
                                                                                                                                       .getTaskTotal()) {
            monsterToHunt = character.getData()
                                     .getTask();
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
        boolean canBeat = combatSimulator.winMoreThanXPercentAgainst(CombatStats.fromCharacter(character.getData()), CombatStats.fromMonster(monster), 95);
        if (!canBeat) {
            logger.warn("Monster {} is too strong for character {}, using fallback.", monsterToHunt, character.getData()
                                                                                                              .getName()
            );

            CombatStats attacker = CombatStats.fromCharacter(character.getData());
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
            if (monsters.size() == 0) {
                logger.warn("No monsters that can be hunted found for character {}", character.getData()
                                                                                              .getName()
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

    private String findHighestMonsterToHunt(CharacterResponseSchema character) {
        CombatStats charCombatStats = CombatStats.fromCharacter(character.getData());

        String rc = "green_slime";
        // String rc = "chicken";
        return rc;
    }

}
