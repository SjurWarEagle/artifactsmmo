package de.tkunkel.game.artifactsmmo;

import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import org.jetbrains.annotations.UnknownNullability;

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


}
