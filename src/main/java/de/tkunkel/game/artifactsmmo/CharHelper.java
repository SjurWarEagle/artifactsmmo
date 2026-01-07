package de.tkunkel.game.artifactsmmo;

import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CharHelper {
    private final Logger logger = LoggerFactory.getLogger(CharHelper.class.getName());

    private final ServerDetailsApiWrapper serverDetailsApi;
    private final CharactersApiWrapper charactersApi;
    private final MyCharactersApiWrapper myCharactersApi;

    public CharHelper(ServerDetailsApiWrapper serverDetailsApi, CharactersApiWrapper charactersApi, MyCharactersApiWrapper myCharactersApi) {
        this.serverDetailsApi = serverDetailsApi;
        this.charactersApi = charactersApi;
        this.myCharactersApi = myCharactersApi;
    }

    public static int getSkillLevelForSkill(CharacterSchema character, Skill requiredSkill) {
        return getSkillLevelForSkill(character, requiredSkill.name());
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

    public static boolean charHasRequiredSkillLevel(CharacterSchema character, String requiredSkill, int requiredSkillLevel) {
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


    public static Optional<ItemSchema> getEquippedItemOfSlot(List<ItemSchema> cachedItems, CharacterResponseSchema character, ItemSlot itemSlot) {
        String itemCodeInSlot = switch (itemSlot) {
            case WEAPON -> character.getData()
                                    .getWeaponSlot();
            case SHIELD -> character.getData()
                                    .getShieldSlot();
            case HELMET -> character.getData()
                                    .getHelmetSlot();
            case BODY_ARMOR -> character.getData()
                                        .getBodyArmorSlot();
            case LEG_ARMOR -> character.getData()
                                       .getLegArmorSlot();
            case BOOTS -> character.getData()
                                   .getBootsSlot();
            case RING1 -> character.getData()
                                   .getRing1Slot();
            case RING2 -> character.getData()
                                   .getRing2Slot();
            case AMULET -> character.getData()
                                    .getAmuletSlot();
            case ARTIFACT1 -> character.getData()
                                       .getArtifact1Slot();
            case ARTIFACT2 -> character.getData()
                                       .getArtifact2Slot();
            case ARTIFACT3 -> character.getData()
                                       .getArtifact3Slot();
            case UTILITY1 -> character.getData()
                                      .getUtility1Slot();
            case UTILITY2 -> character.getData()
                                      .getUtility2Slot();
            case BAG -> character.getData()
                                 .getBagSlot();
            case RUNE -> character.getData()
                                  .getRuneSlot();
        };
        if (itemCodeInSlot == null) {
            return Optional.empty();
        }
        return cachedItems.stream()
                          .filter(itemSchema -> itemSchema.getCode()
                                                          .equalsIgnoreCase(itemCodeInSlot))
                          .findFirst();
    }

    public boolean moveToLocationSync(String characterName, MapSchema destination) {
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
        return moveToLocationSync(character, destination);
    }

    public boolean moveToLocationSync(CharacterResponseSchema character, MapSchema destination) {
        character = charactersApi.getCharacterCharactersNameGet(character.getData()
                                                                         .getName());
        boolean alreadyReached = destination.getX()
                                            .equals(character.getData()
                                                             .getX())
                && destination.getY()
                              .equals(character.getData()
                                               .getY());
        waitUntilCooldownDone(character.getData()
                                       .getName());
        if (alreadyReached) {
            return false;
        }
        DestinationSchema destinationSchema = new DestinationSchema().x(destination.getX())
                                                                     .y(destination.getY());
        CharacterMovementResponseSchema characterMovementResponseSchema = myCharactersApi.actionMoveMyNameActionMovePost(character.getData()
                                                                                                                                  .getName(), destinationSchema
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


    public void healIfNeededSync(String characterName) {
        waitUntilCooldownDone(characterName);
        CharacterResponseSchema character = charactersApi.getCharacterCharactersNameGet(characterName);
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
}
