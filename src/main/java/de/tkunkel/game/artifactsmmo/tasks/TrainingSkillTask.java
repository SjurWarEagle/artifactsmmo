package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.Skill;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

@Service
public class TrainingSkillTask {
    private final Caches caches;

    public TrainingSkillTask(Caches caches) {
        this.caches = caches;
    }

    public Optional<ItemSchema> findBestItemToHarvestAndCraft(CommonBrain brain, CharacterResponseSchema character, Skill... skills) {
        return caches.cachedItems.stream()
                                 .filter(itemSchema -> {
                                     Skill neededSkill = Skill.fromValue(itemSchema.getCraft()
                                                                                   .getSkill()
                                                                                   .getValue());
                                     return Arrays.stream(skills)
                                                  .anyMatch(skill -> skill == neededSkill);

                                 })
                                 .filter(itemSchema -> canCraft(character, itemSchema))
                                 .sorted(Comparator.comparingInt(ItemSchema::getLevel))
                                 .findFirst()
                ;
    }

    private boolean canCraft(CharacterResponseSchema character, ItemSchema itemSchema) {
        Skill neededSkill = Skill.fromValue(itemSchema.getCraft()
                                                      .getSkill()
                                                      .getValue());
        return itemSchema.getCraft() != null
                && charHasEnoughSkill(character, neededSkill, itemSchema.getCraft()
                                                                        .getLevel()
        );
    }

    private boolean charHasEnoughSkill(CharacterResponseSchema character, Skill skill, Integer skillLevel) {
        int skillOfChar = getSkillOfCharToCreate(character, skill);
        return skillOfChar >= skillLevel;
    }

    private int getSkillOfCharToCreate(CharacterResponseSchema character, Skill skill) {
        return switch (skill) {
            case WEAPONCRAFTING -> character.getData()
                                            .getWeaponcraftingLevel();
            case GEARCRAFTING -> character.getData()
                                          .getGearcraftingLevel();
            case JEWELRYCRAFTING -> character.getData()
                                             .getJewelrycraftingLevel();
            case COOKING -> character.getData()
                                     .getCookingLevel();
            case WOODCUTTING -> character.getData()
                                         .getWoodcuttingLevel();
            case MINING -> character.getData()
                                    .getMiningLevel();
            case ALCHEMY -> character.getData()
                                     .getAlchemyLevel();
            case FISHING -> character.getData()
                                     .getFishingLevel();
            default -> throw new RuntimeException("unknown skill " + skill.name());
        };
    }
}
