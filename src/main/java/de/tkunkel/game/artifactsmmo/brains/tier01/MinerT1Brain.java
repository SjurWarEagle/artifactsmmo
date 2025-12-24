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
public class MinerT1Brain extends CommonBrain {
    private final Logger logger = LoggerFactory.getLogger(MinerT1Brain.class.getName());

    public MinerT1Brain(Caches caches) {
        super(caches);
    }

    public boolean hasMaxCraftableGearEquipped(String characterName) {
        return checkIfEquipped(characterName, "copper_dagger", ItemSlot.WEAPON)
                && checkIfEquipped(characterName, "copper_helmet", ItemSlot.HELMET)
                && checkIfEquipped(characterName, "copper_boots", ItemSlot.BOOTS)
                && checkIfEquipped(characterName, "copper_ring", ItemSlot.RING1);
    }

    @Override
    public boolean shouldBeUsed(String characterName) {
        boolean isMissingGear = !hasMaxCraftableGearEquipped(characterName);
        return isMissingGear;
    }

    public boolean mineIfNotEnoughInInventory(String characterName, String oreName, int amount) {
        try {
            CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
            if (isCharCooldown(character)) {
                return false;
            }

            List<InventorySlot> inventory = character.getData()
                                                     .getInventory();

            int amountInInventory = inventory.stream()
                                             .filter(inventorySlot -> inventorySlot.getCode()
                                                                                   .equals(oreName))
                                             .mapToInt(InventorySlot::getQuantity)
                                             .sum()
                    ;
            if (amountInInventory >= amount) {
                return true;
            }
            Optional<MapSchema> mine = findClosestLocation(character, "copper_rocks");
            if (mine.isEmpty()) {
                logger.error("No mine found");
                return false;
            }
            moveToLocation(character, mine.get());
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

    public void smelt(String characterName, String toCraft) {
        try {
            CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
            Optional<MapSchema> smelter = findClosestLocation(character, "mining");
            if (smelter.isEmpty()) {
                logger.error("No smelter found.");
                throw new RuntimeException("no smelter found");
            }
            if (isCharCooldown(character)) {
                return;
            }
            moveToLocation(character, smelter.get());
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
            logger.info("looping miner brain");
            CharacterResponseSchema character = null;
            try {
                character = charactersApi.getCharacterCharactersNameGet(characterName);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }

            if (isCharCooldown(character)) {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }

            equipGearIfNotEquipped(character.getData()
                                            .getName(), "copper_boots", ItemSlot.BOOTS
            );
            equipGearIfNotEquipped(character.getData()
                                            .getName(), "copper_helmet", ItemSlot.HELMET
            );
            equipGearIfNotEquipped(character.getData()
                                            .getName(), "copper_dagger", ItemSlot.WEAPON
            );
            equipGearIfNotEquipped(character.getData()
                                            .getName(), "copper_ring", ItemSlot.RING1
            );

            craftGearIfNotAtCharacter(character.getData()
                                               .getName(), "copper_dagger", "weaponcrafting", ItemSlot.WEAPON
            );
            //todo chnage material if possible
//            boolean allCopperEquipped = hasMaxCraftableGearEquipped(character.getData().getName());
//            if (allCopperEquipped) {
//                throw new BrainCompletedException("All copper equipped");
//            }
            craftGearIfNotAtCharacter(character.getData()
                                               .getName(), "copper_helmet", "gearcrafting", ItemSlot.HELMET
            );
            craftGearIfNotAtCharacter(character.getData()
                                               .getName(), "copper_boots", "gearcrafting", ItemSlot.BOOTS
            );
            craftGearIfNotAtCharacter(character.getData()
                                               .getName(), "copper_dagger", "gearcrafting", ItemSlot.WEAPON
            );
            craftGearIfNotAtCharacter(character.getData()
                                               .getName(), "copper_ring", "jewelrycrafting", ItemSlot.RING1
            );
            boolean enoughInInventory = mineIfNotEnoughInInventory(character.getData()
                                                                            .getName(), "copper_ore", 10
            );
            if (enoughInInventory) {
                logger.info("Enough in inventory, smelting");
                smelt(character.getData()
                               .getName(), "copper_bar"
                );
            } else {
                logger.info("mined, but still not enough in inventory, waiting for next cycle");
            }
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
