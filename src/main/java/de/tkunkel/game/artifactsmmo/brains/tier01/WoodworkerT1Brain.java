package de.tkunkel.game.artifactsmmo.brains.tier01;


import de.tkunkel.game.artifactsmmo.BrainCompletedException;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class WoodworkerT1Brain extends CommonBrain {
    private Logger logger = LoggerFactory.getLogger(WoodworkerT1Brain.class.getName());

    public WoodworkerT1Brain(Caches caches) {
        super(caches);
    }

    @Override
    public boolean shouldBeUsed(String characterName) {
        return false;
    }

    public boolean gatherWoodIfNotEnoughInInventory(String characterName, String woodName, int amount) {
        try {
            CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
            if (isCharCooldown(character)) {
                return false;
            }

            List<InventorySlot> inventory = character.getData()
                                                     .getInventory();

            int amountInInventory = inventory.stream()
                                             .filter(inventorySlot -> inventorySlot.getCode()
                                                                                   .equals(woodName))
                                             .mapToInt(InventorySlot::getQuantity)
                                             .sum()
                    ;
            if (amountInInventory >= amount) {
                return true;
            }

            Optional<ItemSchema> item = caches.cachedItems.stream()
                                                          .filter(itemSchema -> itemSchema.getCode()
                                                                                          .equals(woodName))
                                                          .findFirst()
                    ;
            if (item.isEmpty()) {
                logger.error("No item found");
                return false;
            }


            Optional<ResourceSchema> resource = caches.cachedResources.stream()
                                                                      .filter(resourceSchema -> resourceSchema.getDrops()
                                                                                                              .stream()
                                                                                                              .anyMatch(drop -> drop.getCode()
                                                                                                                                    .equals(woodName)))
                                                                      .findFirst()
                    ;
            if (resource.isEmpty()) {
                logger.error("No resource found");
                return false;
            }
            String itemSource = resource.get()
                                        .getCode();

            Optional<MapSchema> tree = findClosestLocation(character, itemSource);
            if (tree.isEmpty()) {
                logger.error("No tree found");
                return false;
            }
            moveToLocation(character, tree.get());
            gather(character);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        logger.info("Starting mineUntilReached");
        return true;
    }

    private void gather(CharacterResponseSchema character) {
        if (isCharCooldown(character)) {
            return;
        }
        try {
            myCharactersApi.actionGatheringMyNameActionGatheringPost(character.getData()
                                                                              .getName());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

    }

    public void cut(String characterName, String toCraft) {
        try {
            //TODO make this more generic, check with cached items where to craft the item
            CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
            Optional<MapSchema> woodcuter = findClosestLocation(character, "woodcutting");
            if (woodcuter.isEmpty()) {
                logger.error("No wood cutter found.");
                throw new RuntimeException("No wood cutter found");
            }
            if (isCharCooldown(character)) {
                return;
            }
            moveToLocation(character, woodcuter.get());
            if (isCharCooldown(character)) {
                return;
            }
            CraftingSchema craftingSchema = new CraftingSchema().code(toCraft)
                                                                .quantity(1);
            myCharactersApi.actionCraftingMyNameActionCraftingPost(character.getData()
                                                                            .getName(), craftingSchema
            );
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void runBaseLoop(String characterName) throws BrainCompletedException {
        while (true) {
            logger.info("looping woodworker brain");
            CharacterResponseSchema character = null;
            try {
                character = charactersApi.getCharacterCharactersNameGet(characterName);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }

            if (isCharCooldown(character)) {
                return;
            }
            destroyItemsIfInventoryIsFull(character);
            if (isCharCooldown(character)) {
                return;
            }

            boolean enoughInInventory = gatherWoodIfNotEnoughInInventory(character.getData()
                                                                                  .getName(), "ash_wood", 10
            );
            if (enoughInInventory) {
                logger.info("Enough in inventory, cutting");
                cut(character.getData()
                             .getName(), "ash_plank"
                );
            } else {
                logger.info("cut, but still not enough in inventory, waiting for next cycle");
            }
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void destroyItemsIfInventoryIsFull(CharacterResponseSchema character) {
        int cntItemsInInventory = cntItemsInInventory(character);
        if (cntItemsInInventory >= character.getData()
                                            .getInventoryMaxItems() * 0.75) {
            logger.info("Inventory if {} is full, destroying items", character.getData()
                                                                              .getName()
            );
            List<InventorySlot> itemsToDelete = character.getData()
                                                         .getInventory()
                                                         .stream()
                                                         .filter(inventorySlot -> {
                                                             Optional<ItemSchema> itemDefinition = caches.findItemDefinition(inventorySlot.getCode());
                                                             if (itemDefinition.isEmpty()) {
                                                                 return false;
                                                             }
                                                             return itemDefinition.get()
                                                                                  .getSubtype()
                                                                                  .equals("sap");
                                                         })
                                                         .toList()
                    ;
            logger.info("Items to delete: {}", itemsToDelete);
            SimpleItemSchema simpleItemSchema = new SimpleItemSchema().code(itemsToDelete.get(0)
                                                                                         .getCode())
                                                                      .quantity(itemsToDelete.get(0)
                                                                                             .getQuantity());
            try {
                myCharactersApi.actionDeleteItemMyNameActionDeletePost(character.getData()
                                                                                .getName(), simpleItemSchema
                );
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
