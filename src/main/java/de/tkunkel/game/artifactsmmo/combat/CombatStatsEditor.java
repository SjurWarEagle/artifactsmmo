package de.tkunkel.game.artifactsmmo.combat;

import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleEffectSchema;
import org.springframework.stereotype.Service;

@Service
public class CombatStatsEditor {

    /**
     * creates new combat stats with different equipment.
     */
    public CombatStats createManipulatedStats(CombatStats baseStats, ItemSchema originalGear, ItemSchema replacementGear) {
        try {
            CombatStats rc = (CombatStats) baseStats.clone();
            addGear(originalGear, rc);
            removeGear(originalGear, rc);
            return rc;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

    }

    private static CombatStats removeGear(ItemSchema originalGear, CombatStats rc) {
        return changeGear(originalGear, rc, -1);
    }

    private static CombatStats addGear(ItemSchema originalGear, CombatStats rc) {
        return changeGear(originalGear, rc, 1);
    }

    private static CombatStats changeGear(ItemSchema originalGear, CombatStats rc, int factor) {
        for (SimpleEffectSchema effect : originalGear.getEffects()) {
            switch (effect.getCode()) {

                case "hp" -> rc.hp += effect.getValue() * factor;
                case "dmg" -> rc.dmg += effect.getValue() * factor;
                case "critical_strike" -> rc.criticalStrike += effect.getValue() * factor;
                case "attack_fire" -> rc.attackFire += effect.getValue() * factor;
                case "attack_water" -> rc.attackWater += effect.getValue() * factor;
                case "attack_earth" -> rc.attackEarth += effect.getValue() * factor;
                case "attack_air" -> rc.attackAir += effect.getValue() * factor;
                case "res_fire" -> rc.resFire += effect.getValue() * factor;
                case "res_water" -> rc.resWater += effect.getValue() * factor;
                case "res_earth" -> rc.resEarth += effect.getValue() * factor;
                case "res_air" -> rc.resAir += effect.getValue() * factor;
                default -> throw new RuntimeException("unknown effect " + effect.getCode());
            }
        }
        return rc;
    }

}
