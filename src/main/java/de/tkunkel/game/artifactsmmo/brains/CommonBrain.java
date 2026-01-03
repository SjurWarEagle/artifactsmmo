package de.tkunkel.game.artifactsmmo.brains;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.BrainCompletedException;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.shopping.Wish;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.tasks.BankFetchItemsAndCraftTask;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public abstract class CommonBrain implements Brain {
    public final Caches caches;
    protected final WishList wishList;
    public final ApiHolder apiHolder;
    public BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask;

    private final Logger logger = LoggerFactory.getLogger(CommonBrain.class.getName());

    protected CommonBrain(Caches caches, WishList wishList, ApiHolder apiHolder, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask) {
        this.caches = caches;
        this.wishList = wishList;
        this.apiHolder = apiHolder;
        this.bankFetchItemsAndCraftTask = bankFetchItemsAndCraftTask;
    }

    public boolean hasAllItemsInInventory(CharacterResponseSchema character, List<SimpleItemSchema> items) {
        for (SimpleItemSchema requiredItem : items) {
            if (cntSpecificItemsInInventory(character, requiredItem.getCode()) < requiredItem.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    public MapSchema findLocationToCraftItem(String itemToCraft) {
        Optional<ItemSchema> itemSchemaOptional = caches.cachedItems.stream()
                                                                    .filter(item -> item.getCode()
                                                                                        .equals(itemToCraft))
                                                                    .findFirst()
                ;
        if (itemSchemaOptional.isEmpty()) {
            throw new RuntimeException("Item " + itemToCraft + " not found");
        }
        @SuppressWarnings("DataFlowIssue") Optional<MapSchema> map = caches.cachedMap.stream()
                                                                                     .filter(mapSchema -> mapSchema.getInteractions()
                                                                                                                   .getContent() != null)
                                                                                     .filter(mapSchema -> mapSchema.getInteractions()
                                                                                                                   .getContent()
                                                                                                                   .getCode()
                                                                                                                   .equals(itemSchemaOptional.get()
                                                                                                                                             .getCraft()
                                                                                                                                             .getSkill()
                                                                                                                                             .getValue()))
                                                                                     .findFirst()
                ;
        if (map.isEmpty()) {
            throw new RuntimeException("No map found for skill " + itemSchemaOptional.get()
                                                                                     .getCraft()
                                                                                     .getSkill());
        }
        return map.get();
    }

    /**
     * find an item that can be crafted with the items in inventory and skill of the char.
     * Use  higest level
     *
     * @param character
     * @return
     */
    public Optional<String> findPossibleItemToCraft(CharacterResponseSchema character) {
        return caches.cachedItems.stream()
                                 .filter(
                                         item -> item.getCraft() != null)
                                 .filter(
                                         item -> item.getCraft()
                                                     .getItems() != null)
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
                                     return CharHelper.charHasRequiredSkillLevel(character.getData(), requiredSkill, requiredSkillLevel);
                                 })
                                 .sorted(Comparator.comparingInt(o -> o.getCraft()
                                                                       .getLevel()))
                                 .filter(item -> hasAllItemsInInventory(character, item.getCraft()
                                                                                       .getItems()
                                 ))
                                 .map(itemSchema -> itemSchema.getCode())
                                 .findFirst()
                ;
    }

    public int cntAllItemsInInventory(CharacterResponseSchema character) {
        return character.getData()
                        .getInventory()
                        .stream()
                        .mapToInt(InventorySlot::getQuantity)
                        .sum();
    }

    public int cntSpecificItemsInInventory(CharacterResponseSchema character, String itemCode) {
        return character.getData()
                        .getInventory()
                        .stream()
                        .filter(inventorySlot -> inventorySlot.getCode()
                                                              .equals(itemCode))
                        .mapToInt(InventorySlot::getQuantity)
                        .sum();
    }

    @Override
    public void waitUntilCooldownDone(CharacterResponseSchema character) {
        OffsetDateTime serverTime;
        try {
            serverTime = apiHolder.serverDetailsApi.getServerDetailsGet()
                                                   .getData()
                                                   .getServerTime();
            character = apiHolder.charactersApi.getCharacterCharactersNameGet(character.getData()
                                                                                       .getName());
            long timeToWait = character.getData()
                                       .getCooldownExpiration()
                                       .toEpochSecond() - serverTime.toEpochSecond();
            if (timeToWait > 0) {
                logger.info("Server time: {}", serverTime);
                logger.info("Character cooldown expiration: {}", character.getData()
                                                                          .getCooldownExpiration()
                );
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

    @Override
    public void runBaseLoop(String characterName) throws BrainCompletedException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void eatFoodOrRestIfNeeded(CharacterResponseSchema character) {
        logger.info("Checking if character {} needs to rest", character.getData()
                                                                       .getName()
        );
        // heal if 75% left
        // TODO change to calculate the max damage of my enemy
        if (character.getData()
                     .getHp() > character.getData()
                                         .getMaxHp() * 0.75) {
            return;
        }
        if (eatFoodIfHasFood(character)) {
            return;
        }
        waitUntilCooldownDone(character);
        apiHolder.myCharactersApi.actionRestMyNameActionRestPost(character.getData()
                                                                          .getName());
        waitUntilCooldownDone(character);
    }

    private boolean eatFoodIfHasFood(CharacterResponseSchema character) {
        for (InventorySlot inventorySlot : character.getData()
                                                    .getInventory()) {
            if (inventorySlot.getQuantity() >= 1
                    && (inventorySlot.getCode()
                                     .equalsIgnoreCase("apple")
                    || inventorySlot.getCode()
                                    .equalsIgnoreCase("cooked_chicken"))
            ) {
                SimpleItemSchema simpleItemSchema = new SimpleItemSchema().quantity(1)
                                                                          .code(inventorySlot.getCode());
                waitUntilCooldownDone(character);
                apiHolder.myCharactersApi.actionUseItemMyNameActionUsePost(character.getData()
                                                                                    .getName(), simpleItemSchema
                );
                return true;
            }

        }
        return false;
    }

    public Optional<MapSchema> findLocationOfClosestMonster(CharacterResponseSchema character, String monster) {
        logger.info("Starting findClosestMonster");
        AtomicReference<Optional<MapSchema>> rc = new AtomicReference<>(Optional.empty());

        int charX = character.getData()
                             .getX();
        int charY = character.getData()
                             .getY();
        caches.cachedMap.stream()
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent() != null)
//                .filter(mapSchema -> mapSchema.getInteractions().getContent().getType().equals(MapContentType.MONSTER))
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent()
                                                      .getCode()
                                                      .equals(monster))
                        .sorted((mapSchema1, mapSchema2) -> {
                            int distance1 = Math.abs(mapSchema1.getX() - charX) + Math.abs(mapSchema1.getY() - charY);
                            int distance2 = Math.abs(mapSchema2.getX() - charX) + Math.abs(mapSchema2.getY() - charY);
                            return distance2 - distance1;
                        })
                        .forEach(mapSchema -> {
                            rc.set(Optional.of(mapSchema));
                        })
        ;
        return rc.get();
    }

    public Optional<MapSchema> findClosesTaskMaster(CharacterResponseSchema character, String taskMasterType) {
        AtomicReference<Optional<MapSchema>> rc = new AtomicReference<>(Optional.empty());

        int charX = character.getData()
                             .getX();
        int charY = character.getData()
                             .getY();
        caches.cachedMap.stream()
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent() != null)
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent()
                                                      .getType() != null)
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent()
                                                      .getType()
                                                      .getValue()
                                                      .equals("tasks_master"))
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent()
                                                      .getCode()
                                                      .equals(taskMasterType))
                        .sorted((mapSchema1, mapSchema2) -> {
                            int distance1 = Math.abs(mapSchema1.getX() - charX) + Math.abs(mapSchema1.getY() - charY);
                            int distance2 = Math.abs(mapSchema2.getX() - charX) + Math.abs(mapSchema2.getY() - charY);
                            return distance2 - distance1;
                        })
                        .forEach(mapSchema -> {
                            rc.set(Optional.of(mapSchema));
                        })
        ;
        return rc.get();
    }

    public Optional<MapSchema> findClosestLocation(CharacterResponseSchema character, String activity) {
        AtomicReference<Optional<MapSchema>> rc = new AtomicReference<>(Optional.empty());

        int charX = character.getData()
                             .getX();
        int charY = character.getData()
                             .getY();
        caches.cachedMap.stream()
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent() != null)
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent()
                                                      .getCode()
                                                      .equals(activity))
                        .sorted((mapSchema1, mapSchema2) -> {
                            int distance1 = Math.abs(mapSchema1.getX() - charX) + Math.abs(mapSchema1.getY() - charY);
                            int distance2 = Math.abs(mapSchema2.getX() - charX) + Math.abs(mapSchema2.getY() - charY);
                            return distance2 - distance1;
                        })
                        .forEach(mapSchema -> {
                            rc.set(Optional.of(mapSchema));
                        })
        ;
        return rc.get();
    }

    public void equipGearIfNotEquipped(String characterName, String gear, ItemSlot itemSlot) {
        EquipSchema equipSchema = new EquipSchema().slot(itemSlot)
                                                   .code(gear);
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        boolean alreadyEquipped = checkIfEquipped(gear, itemSlot, character);
        if (alreadyEquipped) {
            return;
        }
        waitUntilCooldownDone(character);
        apiHolder.myCharactersApi.actionEquipItemMyNameActionEquipPost(character.getData()
                                                                                .getName(), equipSchema
        );
    }

    public boolean checkIfEquipped(String characterName, String gear, ItemSlot itemSlot) {
        CharacterResponseSchema character;
        character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        return checkIfEquipped(gear, itemSlot, character);
    }

    public boolean checkIfEquipped(String gear, ItemSlot itemSlot, CharacterResponseSchema character) {
        return switch (itemSlot) {
            case BOOTS -> character.getData()
                                   .getBootsSlot()
                                   .equalsIgnoreCase(gear)
            ;
            case SHIELD -> character.getData()
                                    .getShieldSlot()
                                    .equalsIgnoreCase(gear)
            ;
            case HELMET -> character.getData()
                                    .getHelmetSlot()
                                    .equalsIgnoreCase(gear)
            ;
            case WEAPON -> character.getData()
                                    .getWeaponSlot()
                                    .equalsIgnoreCase(gear)
            ;
            case BODY_ARMOR -> character.getData()
                                        .getBodyArmorSlot()
                                        .equalsIgnoreCase(gear)
            ;
            case LEG_ARMOR -> character.getData()
                                       .getLegArmorSlot()
                                       .equalsIgnoreCase(gear)
            ;
            case RING1 -> character.getData()
                                   .getRing1Slot()
                                   .equalsIgnoreCase(gear)
            ;
            case RING2 -> character.getData()
                                   .getRing2Slot()
                                   .equalsIgnoreCase(gear)
            ;
            case AMULET -> character.getData()
                                    .getAmuletSlot()
                                    .equalsIgnoreCase(gear)
            ;
            case ARTIFACT1 -> character.getData()
                                       .getArtifact1Slot()
                                       .equalsIgnoreCase(gear)
            ;
            case ARTIFACT2 -> character.getData()
                                       .getArtifact2Slot()
                                       .equalsIgnoreCase(gear)
            ;
            case ARTIFACT3 -> character.getData()
                                       .getArtifact3Slot()
                                       .equalsIgnoreCase(gear)
            ;
            case UTILITY1 -> character.getData()
                                      .getUtility1Slot()
                                      .equalsIgnoreCase(gear)
            ;
            case UTILITY2 -> character.getData()
                                      .getUtility2Slot()
                                      .equalsIgnoreCase(gear)
            ;
            case BAG -> character.getData()
                                 .getBagSlot()
                                 .equalsIgnoreCase(gear)
            ;
            case RUNE -> character.getData()
                                  .getRuneSlot()
                                  .equalsIgnoreCase(gear)
            ;
            default -> throw new RuntimeException("unknown slot " + itemSlot);
        };
    }

    public void craftGearIfNotAtCharacter(String characterName, String gear, String craftingStation, ItemSlot slot) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        boolean enoughResourcesToCraft = character.getData()
                                                  .getInventory()
                                                  .stream()
                                                  .filter(inventorySlot -> inventorySlot.getCode()
                                                                                        .equals("copper_bar"))
                                                  .mapToInt(InventorySlot::getQuantity)
                                                  .sum() >= 10;
        if (!enoughResourcesToCraft) {
            return;
        }
        waitUntilCooldownDone(character);
        boolean equipped = checkIfEquipped(gear, slot, character);
        if (equipped) {
            return;
        }

        Optional<MapSchema> closestLocation = findClosestLocation(character, craftingStation);
        if (closestLocation.isEmpty()) {
            logger.error("No location found for {}", craftingStation);
            return;
        }
        moveToLocation(character, closestLocation.get());
        waitUntilCooldownDone(character);
        CraftingSchema craftingSchema = new CraftingSchema().code(gear)
                                                            .quantity(1);
        apiHolder.myCharactersApi.actionCraftingMyNameActionCraftingPost(character.getData()
                                                                                  .getName(), craftingSchema
        );

    }

    public boolean moveToLocation(String characterName, MapSchema destination) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        return moveToLocation(character, destination);
    }

    public boolean moveToLocation(CharacterResponseSchema character, MapSchema destination) {
        character = apiHolder.charactersApi.getCharacterCharactersNameGet(character.getData()
                                                                                   .getName());
        boolean alreadyReached = destination.getX()
                                            .equals(character.getData()
                                                             .getX())
                && destination.getY()
                              .equals(character.getData()
                                               .getY());
        if (alreadyReached) {
            return false;
        }
        waitUntilCooldownDone(character.getData()
                                       .getName());
        DestinationSchema destinationSchema = new DestinationSchema().x(destination.getX())
                                                                     .y(destination.getY());
        apiHolder.myCharactersApi.actionMoveMyNameActionMovePost(character.getData()
                                                                          .getName(), destinationSchema
        );
        waitUntilCooldownDone(character.getData()
                                       .getName());
        return true;
    }

    public void waitUntilCooldownDone(String characterName) {
        CharacterResponseSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        waitUntilCooldownDone(character);
    }

    public MapSchema findLocationWhereToFarm(String resourceToFarm) {
        Optional<MapSchema> map = caches.cachedMap.stream()
                                                  .filter(mapSchema -> mapSchema.getInteractions()
                                                                                .getContent() != null)
                                                  .filter(mapSchema -> mapSchema.getInteractions()
                                                                                .getContent()
                                                                                .getCode()
                                                                                .equals(resourceToFarm))
                                                  // todo order by distance to character
                                                  .findFirst()
                ;
        if (map.isEmpty()) {
            throw new RuntimeException("No map found for resource " + resourceToFarm);
        }
        return map.get();
    }

    public String decideWhatResourceToFarm(String characterName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void equipOrRequestBestWeapon(String characterName) {
        CharacterResponseSchema character = null;
        character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);
        Optional<ItemSchema> bestForSlot = caches.findBestItemForSlotThatCanBeCraftedByAccount(ItemSlot.WEAPON.name(), character);
        if (bestForSlot.isEmpty()) {
            return;
        }
        ItemSlot itemSlot = ItemSlot.fromValue(bestForSlot.get()
                                                          .getType());
        if (checkIfEquipped(character.getData()
                                     .getName(), bestForSlot.get()
                                                            .getCode(), itemSlot
        )) {
            return;
        }
        Optional<InventorySlot> inventorySlot = character.getData()
                                                         .getInventory()
                                                         .stream()
                                                         .filter(innerInventorySlot -> innerInventorySlot.getCode()
                                                                                                         .equals(bestForSlot.get()
                                                                                                                            .getCode()))
                                                         .findFirst()
                ;
        boolean itemExistsInBank;
        itemExistsInBank = apiHolder.myAccountApi.getBankItemsMyBankItemsGet(bestForSlot.get()
                                                                                        .getCode(), 1, 100
                                    )
                                                 .getData()
                                                 .size() > 0;
        boolean itemExistsInInventory = inventorySlot.isPresent();

        boolean alreadyEquipped = checkIfEquipped(bestForSlot.get()
                                                             .getCode(), itemSlot, character
        );
        if (!itemExistsInInventory && !itemExistsInBank && !alreadyEquipped) {
            logger.info("Best item (%s) for %s not in inventory nor bank nor equipped, requesting"
                                .formatted(bestForSlot.get()
                                                      .getCode(), itemSlot.getValue()
                                ));
            wishList.addRequest(new Wish(character.getData()
                                                  .getName(), bestForSlot.get()
                                                                         .getCode()
                    , 1
            ));
            return;
        }
        if (!alreadyEquipped && itemExistsInBank) {
            fetchItemFromBank(character, bestForSlot.get()
                                                    .getCode()
            );
        }
        if (!alreadyEquipped) {
            equipGearIfNotEquipped(character.getData()
                                            .getName(), bestForSlot.get()
                                                                   .getCode(), itemSlot
            );
        }
    }

    public void equipOrRequestBestArmorForSlot(String characterName, String slotName) {
        CharacterResponseSchema character = null;
        character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName);

        Optional<ItemSchema> bestArmorForSkill = caches.findBestItemForSlotThatCanBeCraftedByAccount(slotName, character);
        if (bestArmorForSkill.isEmpty()) {
            return;
        }
        ItemSlot itemSlot = ItemSlot.fromValue(bestArmorForSkill.get()
                                                                .getType());
        if (checkIfEquipped(character.getData()
                                     .getName(), bestArmorForSkill.get()
                                                                  .getCode(), itemSlot
        )) {
            return;
        }
        //        logger.info("Equipping {}", bestArmorForSkill.get()
        //                                                  .getCode()
        //       );
        Optional<InventorySlot> inventorySlot = character.getData()
                                                         .getInventory()
                                                         .stream()
                                                         .filter(innerInventorySlot -> innerInventorySlot.getCode()
                                                                                                         .equals(bestArmorForSkill.get()
                                                                                                                                  .getCode()))
                                                         .findFirst()
                ;
        boolean itemExistsInBank;
        itemExistsInBank = apiHolder.myAccountApi.getBankItemsMyBankItemsGet(bestArmorForSkill.get()
                                                                                              .getCode(), 1, 100
                                    )
                                                 .getData()
                                                 .size() > 0;
        boolean itemExistsInInventory = inventorySlot.isPresent();

        boolean alreadyEquipped = checkIfEquipped(bestArmorForSkill.get()
                                                                   .getCode(), itemSlot, character
        );
        if (!itemExistsInInventory && !itemExistsInBank && !alreadyEquipped) {
            logger.info("Best tool (" + bestArmorForSkill.get()
                                                         .getCode() + ") not in inventory nor bank nor equipped, requesting");
            wishList.addRequest(new Wish(character.getData()
                                                  .getName(), bestArmorForSkill.get()
                                                                               .getCode()
                    , 1
            ));
            return;
        }
        if (!alreadyEquipped && itemExistsInBank) {
            fetchItemFromBank(character, bestArmorForSkill.get()
                                                          .getCode()
            );
        }
        if (!alreadyEquipped) {
            equipGearIfNotEquipped(character.getData()
                                            .getName(), bestArmorForSkill.get()
                                                                         .getCode(), itemSlot
            );
        }
    }

    public void equipOrRequestBestToolForSkill(CharacterResponseSchema character, String skillName) {
        Optional<ItemSchema> bestToolForSkill = caches.findBestToolForSkillThatCanBeCraftedByAccount(skillName, character.getData()
                                                                                                                         .getMiningLevel()
        );
        if (bestToolForSkill.isEmpty()) {
            return;
        }
        ItemSlot itemSlot = ItemSlot.fromValue(bestToolForSkill.get()
                                                               .getType());
        if (checkIfEquipped(character.getData()
                                     .getName(), bestToolForSkill.get()
                                                                 .getCode(), itemSlot
        )) {
            return;
        }
        //        logger.info("Equipping {}", bestToolForSkill.get()
        //                                                  .getCode()
        //       );
        Optional<InventorySlot> inventorySlot = character.getData()
                                                         .getInventory()
                                                         .stream()
                                                         .filter(innerInventorySlot -> innerInventorySlot.getCode()
                                                                                                         .equals(bestToolForSkill.get()
                                                                                                                                 .getCode()))
                                                         .findFirst()
                ;
        boolean itemExistsInBank = apiHolder.myAccountApi.getBankItemsMyBankItemsGet(bestToolForSkill.get()
                                                                                                     .getCode(), 1, 100
                                            )
                                                         .getData()
                                                         .size() > 0;
        boolean itemExistsInInventory = inventorySlot.isPresent();

        boolean alreadyEquipped = checkIfEquipped(bestToolForSkill.get()
                                                                  .getCode(), itemSlot, character
        );
        if (!itemExistsInInventory && !itemExistsInBank && !alreadyEquipped) {
            logger.info("Best tool (" + bestToolForSkill.get()
                                                        .getCode() + ") not in inventory nor bank nor equipped, requesting");
            wishList.addRequest(new Wish(character.getData()
                                                  .getName(), bestToolForSkill.get()
                                                                              .getCode()
                    , 1
            ));
            return;
        }
        if (!alreadyEquipped && itemExistsInBank) {
            fetchItemFromBank(character, bestToolForSkill.get()
                                                         .getCode()
            );
        }
        if (!alreadyEquipped) {
            equipGearIfNotEquipped(character.getData()
                                            .getName(), bestToolForSkill.get()
                                                                        .getCode(), itemSlot
            );
        }
    }

    private void fetchItemFromBank(CharacterResponseSchema character, String itemCode) {
        Optional<MapSchema> bank = findClosestLocation(character, "bank");
        if (bank.isEmpty()) {
            logger.error("Could not find bank for character %s".formatted(character.getData()
                                                                                   .getName()));
            throw new RuntimeException("Could not find bank for character " + character.getData()
                                                                                       .getName());
        }
        moveToLocation(character, bank.get());
        waitUntilCooldownDone(character);

        SimpleItemSchema simpleItemSchema = new SimpleItemSchema().code(itemCode)
                                                                  .quantity(1);

        apiHolder.myCharactersApi.actionWithdrawBankItemMyNameActionBankWithdrawItemPost(character.getData()
                                                                                                  .getName(), Collections.singletonList(simpleItemSchema)
        );
        waitUntilCooldownDone(character);
    }

}
