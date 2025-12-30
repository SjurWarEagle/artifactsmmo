package de.tkunkel.game.artifactsmmo.brains.tier01;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.combat.CombatSimulator;
import de.tkunkel.game.artifactsmmo.combat.CombatStats;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositGoldIfRichTask;
import de.tkunkel.game.artifactsmmo.tasks.BankFetchItemsAndCraftTask;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class FighterT1Brain extends CommonBrain {
    private final Logger logger = LoggerFactory.getLogger(FighterT1Brain.class.getName());
    private final BankDepositGoldIfRichTask bankDepositGoldIfRichTask;
    private final BankDepositAllTask bankDepositAllTask;
    private final CombatSimulator combatSimulator;


    @Override
    public boolean shouldBeUsed(String characterName) {
        return false;
    }

    public FighterT1Brain(Caches caches, WishList wishList, ApiHolder apiHolder, BankDepositGoldIfRichTask bankDepositGoldIfRichTask, BankDepositAllTask bankDepositAllTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask, CombatSimulator combatSimulator) {
        super(caches, wishList, apiHolder, bankFetchItemsAndCraftTask);
        this.bankDepositGoldIfRichTask = bankDepositGoldIfRichTask;
        this.bankDepositAllTask = bankDepositAllTask;
        this.combatSimulator = combatSimulator;
    }

    @Override
    public void runBaseLoop(String characterName) {
        try {
            CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
            waitUntilCooldownDone(character);
            bankDepositGoldIfRichTask.depositInventoryInBankIfInventoryIsFull(this, character);
            depositNonFoodAtBankIfInventoryIsFull(character);
            cookFoodIfHaveSome(character);
            eatFoodOrRestIfNeeded(character);
            equipOrRequestBestWeapon(character);
            equipOrRequestBestArmorForSlot(character, "body_armor");
            equipOrRequestBestArmorForSlot(character, "helmet");
            equipOrRequestBestArmorForSlot(character, "shield");
            equipOrRequestBestArmorForSlot(character, "boots");
            equipOrRequestBestArmorForSlot(character, "leg_armor");

            completeCurrentTaskIfDone(character);
            getNewTaskIfCurrentTaskIsDone(character);
            bankDepositAllTask.depositInventoryInBankIfInventoryIsFull(this, character);

            waitUntilCooldownDone(character);

            String enemyToHunt = decideWhatEnemyToHunt(character);
            Optional<MapSchema> locationOfClosestMonster = findLocationOfClosestMonster(character, enemyToHunt);
            if (locationOfClosestMonster.isEmpty()) {
                logger.error("Could not find location of closest monster ({})", enemyToHunt);
                return;
            }
            boolean charAtDestination = false;

            charAtDestination = (locationOfClosestMonster.get()
                                                         .getX()
                                                         .equals(character.getData()
                                                                          .getX()) && locationOfClosestMonster.get()
                                                                                                              .getY()
                                                                                                              .equals(character.getData()
                                                                                                                               .getY()));

            if (!charAtDestination) {
                DestinationSchema destinationSchema = new DestinationSchema();
                destinationSchema.setX(locationOfClosestMonster.get()
                                                               .getX());
                destinationSchema.setY(locationOfClosestMonster.get()
                                                               .getY());
                apiHolder.myCharactersApi.actionMoveMyNameActionMovePost(character.getData()
                                                                                  .getName(), destinationSchema
                );
                logger.info("Moving to location of closest monster");
                return;
            } else {
                logger.info("Character {} is at location of closest monster", character.getData()
                                                                                       .getName()
                );
            }

            waitUntilCooldownDone(character);
            FightRequestSchema fightRequest = new FightRequestSchema();
            apiHolder.myCharactersApi.actionFightMyNameActionFightPost(character.getData()
                                                                                .getName(), fightRequest
            );
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void depositNonFoodAtBankIfInventoryIsFull(CharacterResponseSchema character) {
        int inventoryUsed = cntAllItemsInInventory(character);
        // store if more than 75% are used
        if (inventoryUsed <= character.getData()
                                      .getInventoryMaxItems() * 0.75) {
            return;
        }
        Optional<MapSchema> bank = findClosestLocation(character, "bank");
        if (bank.isEmpty()) {
            throw new RuntimeException("Could not find bank for character " + character.getData()
                                                                                       .getName());
        }
        moveToLocation(character, bank.get());
        waitUntilCooldownDone(character);
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
        try {
            apiHolder.myCharactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(character.getData()
                                                                                                    .getName(), bankRequestSchema
            );
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void getNewTaskIfCurrentTaskIsDone(CharacterResponseSchema character) {
        if (!"".equalsIgnoreCase(character.getData()
                                          .getTask())) {
            // still has task
            return;
        }
        try {
            Optional<MapSchema> closestLocation = findClosestLocation(character, "monsters");
            if (closestLocation.isEmpty()) {
                return;
            }
            boolean moved = moveToLocation(character, closestLocation.get());
            if (moved) {
                return;
            }
            apiHolder.myCharactersApi.actionAcceptNewTaskMyNameActionTaskNewPost(character.getData()
                                                                                          .getName());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
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
        try {
            Optional<MapSchema> closestLocation = findClosestLocation(character, "monsters");
            if (closestLocation.isEmpty()) {
                return;
            }
            boolean moved = moveToLocation(character, closestLocation.get());
            if (moved) {
                return;
            }
            apiHolder.myCharactersApi.actionCompleteTaskMyNameActionTaskCompletePost(character.getData()
                                                                                              .getName());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void cookFoodIfHaveSome(CharacterResponseSchema character) {
        Optional<InventorySlot> foodItem = character.getData()
                                                    .getInventory()
                                                    .stream()
                                                    .filter(inventorySlot -> inventorySlot.getCode()
                                                                                          .equalsIgnoreCase("raw_chicken"))
                                                    .filter(inventorySlot -> inventorySlot.getQuantity() >= 5)
                                                    // TODO check cooking-skill .filter(inventorySlot -> inventorySlot.getCode().equalsIgnoreCase("egg"))
                                                    .findAny()
                ;
        if (foodItem.isPresent()) {
            Optional<MapSchema> cooking = findClosestLocation(character, "cooking");
            if (cooking.isPresent()) {
                boolean moved = moveToLocation(character, cooking.get());
                if (moved) {
                    return;
                }
                // TODO get targetCode from item-definition
                String targetCode = switch (foodItem.get()
                                                    .getCode()) {
                    case "raw_chicken" -> "cooked_chicken";
                    default -> throw new IllegalStateException("Unexpected value: " + foodItem.get()
                                                                                              .getCode());
                };
                CraftingSchema craftingSchema = new CraftingSchema().code(targetCode)
                                                                    .quantity(foodItem.get()
                                                                                      .getQuantity());
                try {
                    apiHolder.myCharactersApi.actionCraftingMyNameActionCraftingPost(character.getData()
                                                                                              .getName(), craftingSchema
                    );
                } catch (ApiException e) {
                    throw new RuntimeException(e);
                }


            }
        }

    }

    private String decideWhatEnemyToHunt(CharacterResponseSchema character) {
        String monsterToHunt = "chicken";
        if (character.getData()
                     .getTask() != null && TaskType.MONSTERS.getValue()
                                                            .equals(character.getData()
                                                                             .getTaskType()) && character.getData()
                                                                                                         .getTaskProgress() < character.getData()
                                                                                                                                       .getTaskTotal()) {
            monsterToHunt = character.getData()
                                     .getTask();
        }

        String finalMonsterToHunt = monsterToHunt;
        var monster = caches.cachedMonsters.stream()
                                           .filter(monsterSchema -> monsterSchema.getCode()
                                                                                 .equals(finalMonsterToHunt))
                                           .findFirst()
                                           .get()
                ;
        boolean canBeat = combatSimulator.winMoreThanXPercentAgainst(CombatStats.fromCharacter(character.getData()), CombatStats.fromMonster(monster), 80);
        if (!canBeat) {
            logger.warn("Monster {} is too strong for character {}, using fallback.", monsterToHunt, character.getData()
                                                                                                              .getName()
            );

            CombatStats attacker = CombatStats.fromCharacter(character.getData());
            List<MonsterSchema> monsters = caches.cachedMonsters.stream()
                                                                .filter(monsterSchema -> {
                                                                    CombatStats defender = CombatStats.fromMonster(monsterSchema);
                                                                    return combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 80);
                                                                })
                                                                .toList()
                    ;
            logger.info("Monsters that can be hunted: {}", monsters.stream()
                                                                   .map(MonsterSchema::getName)
                                                                   .toList()
            );
            monsterToHunt = monsters.stream()
                                    .sorted(Comparator.comparingInt(MonsterSchema::getLevel))
                                    // use last of streams
                                    .reduce((o1, o2) -> o2)
                                    .get()
                                    .getCode();
        }

        return monsterToHunt;
    }

}
