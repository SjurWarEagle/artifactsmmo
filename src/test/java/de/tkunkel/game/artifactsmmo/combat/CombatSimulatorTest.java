package de.tkunkel.game.artifactsmmo.combat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CombatSimulatorTest {
    private final CombatSimulator combatSimulator = new CombatSimulator();

    @Test
    void easyFight() {
        CombatStats attacker = new CombatStats();
        attacker.hp = 225;
        attacker.attackAir = 6;
        attacker.criticalStrike = 35;
        CombatStats defender = new CombatStats();
        defender.hp = 80;
        defender.attackAir = 14;
        defender.resAir = 25;


        Assertions.assertTrue(combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 50));
    }

    @Test
    void cowFight() {
        CombatStats attacker = new CombatStats();
        attacker.hp = 215;
        attacker.initiative = 100;
        attacker.attackAir = 6;
        attacker.criticalStrike = 35;
        CombatStats defender = new CombatStats();
        defender.hp = 280;
        defender.initiative = 100;
        defender.attackAir = 21;
        defender.resEarth = -30;
        defender.resWater = 30;


        Assertions.assertTrue(combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 50));
    }

    @Test
    void hardFight() {
        CombatStats attacker = new CombatStats();
        attacker.hp = 215;
        attacker.initiative = 100;
        attacker.attackAir = 6;
        attacker.criticalStrike = 35;
        CombatStats defender = new CombatStats();
        defender.hp = 120;
        defender.initiative = 100;
        defender.attackWater = 15;
        defender.resWater = 25;

        int wins = 0;
        for (int i = 0; i < 100; i++) {
            boolean result = combatSimulator.attackerWinsSimulatedCombat(attacker, defender);
            if (result) {
                wins++;
                wins++;
            }
        }
        Assertions.assertFalse(wins > 50);
    }

}