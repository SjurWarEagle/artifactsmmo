package de.tkunkel.game.artifactsmmo;

import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import org.jetbrains.annotations.UnknownNullability;

public class CharHelper {
    public static boolean charHasRequiredSkillLevel(CharacterResponseSchema character, @UnknownNullability String requiredSkill, int requiredSkillLevel) {
        int charSkillLevel = 0;
        charSkillLevel = switch (requiredSkill) {
            case "alchemy" -> character.getData()
                                       .getAlchemyLevel();
            case "fishing" -> character.getData()
                                       .getFishingLevel();
            case "jewelrycrafting" -> character.getData()
                                               .getJewelrycraftingLevel();
            case "woodcutting" -> character.getData()
                                           .getWoodcuttingLevel();
            case "mining" -> character.getData()
                                      .getMiningLevel();
            case "weaponcrafting" -> character.getData()
                                              .getWeaponcraftingLevel();
            case "gearcrafting" -> character.getData()
                                            .getGearcraftingLevel();
            case "cooking" -> character.getData()
                                       .getCookingLevel();
            default -> throw new RuntimeException("unknown skill: " + requiredSkill);
        };
        return charSkillLevel >= requiredSkillLevel;
    }


}
