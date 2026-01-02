package de.tkunkel.game.artifactsmmo;

import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSlot;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.Optional;

public class CharHelper {
    public static boolean charHasRequiredSkillLevel(CharacterSchema character, @UnknownNullability String requiredSkill, int requiredSkillLevel) {
        int charSkillLevel = 0;
        charSkillLevel = switch (requiredSkill.toLowerCase()) {
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
        return charSkillLevel >= requiredSkillLevel;
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
}
