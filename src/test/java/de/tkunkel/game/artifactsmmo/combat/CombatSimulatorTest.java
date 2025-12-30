package de.tkunkel.game.artifactsmmo.combat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
    void redSlimeFight() {
        CombatStats attacker = new CombatStats();
        attacker.hp = 215;
        attacker.initiative = 100;
        attacker.attackAir = 6;
        attacker.criticalStrike = 35;
        attacker.resWater = 2;
        attacker.resFire = 2;
        attacker.resAir = 2;
        attacker.resEarth = 2;
        CombatStats defender = new CombatStats();
        defender.hp = 280;
        defender.initiative = 100;
        defender.attackAir = 21;
        defender.resEarth = -30;
        defender.resWater = 30;


        Assertions.assertFalse(combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 50));
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


        Assertions.assertFalse(combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 50));
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

    @Test
    void simulateHowManyMonstersCanBeBeaten_NoEnemy() {
        CombatStats attacker = new CombatStats();
        attacker.hp = 215;
        attacker.initiative = 100;
        attacker.attackAir = 6;
        attacker.criticalStrike = 35;

        List<CombatStats> defenders = new ArrayList<>();
        int result = combatSimulator.simulateHowManyMonstersCanBeBeaten(attacker, defenders);
        Assertions.assertEquals(0, result);
    }

    @Test
    void simulateHowManyMonstersCanBeBeaten_EasyEnemies() {
        CombatStats attacker = new CombatStats();
        attacker.hp = 215;
        attacker.initiative = 100;
        attacker.attackAir = 6;
        attacker.criticalStrike = 35;

        List<CombatStats> defenders = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            CombatStats defender = new CombatStats();
            defender.hp = i;
            defender.attackWater = 10;
            defenders.add(defender);
        }
        int result = combatSimulator.simulateHowManyMonstersCanBeBeaten(attacker, defenders);
        Assertions.assertEquals(100, result);
    }

}