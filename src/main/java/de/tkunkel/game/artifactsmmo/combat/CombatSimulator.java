package de.tkunkel.game.artifactsmmo.combat;

import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.MonsterSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CombatSimulator {

    private final int MAX_ROUNDS = 100;
    private final Logger logger = LoggerFactory.getLogger(CombatSimulator.class.getName());

    //    @Cacheable(cacheNames = "combatSimulatorWinPercentage")
    public boolean winMoreThanXPercentAgainst(CombatStats attacker, CombatStats defender, int percent) {
        int wins = 0;
        for (int i = 0; i < 100; i++) {
            boolean result = attackerWinsSimulatedCombat(attacker, defender);
            if (result) {
                wins++;
            }
        }

        return wins >= percent;
    }

    public int simulateHowManyMonstersCanBeBeaten(CombatStats attacker, List<CombatStats> defenders) {
        AtomicInteger rc = new AtomicInteger(0);
        defenders.parallelStream()
                 .forEach(defender -> {
                     if (winMoreThanXPercentAgainst(attacker, defender, 90)) {
                         rc.incrementAndGet();
                     }
                 });
        return rc.get();

    }

    public boolean attackerWinsSimulatedCombat(CombatStats attacker, CombatStats defender) {
        int attackerHp = attacker.hp;
        int defenderHp = defender.hp;
        boolean attackerActsFirst = decideWhoGoesFirst(attacker, defender);
        for (int round = 1; round <= MAX_ROUNDS; round++) {
            int attackerDamage = calculateDamage(attacker, defender);
            int defenderDamage = calculateDamage(defender, attacker);

            if (attackerActsFirst) {
                defenderHp -= attackerDamage;
                if (defenderHp <= 0) {
                    return true;
                }
                attackerHp -= defenderDamage;
                if (attackerHp <= 0) {
                    return false;
                }
            } else {
                attackerHp -= defenderDamage;
                if (attackerHp <= 0) {
                    return false;
                }
                defenderHp -= attackerDamage;
                if (defenderHp <= 0) {
                    return true;
                }
            }
        }

        return attackerHp > defenderHp;
    }

    private int calculateDamage(CombatStats attacker, CombatStats defender) {
        double damage = 0;
        damage += attacker.attackWater * ((100f - defender.resWater) / 100) * (1 + defender.dmgWater / 100d);
        damage += attacker.attackAir * ((100f - defender.resAir) / 100) * (1 + defender.dmgAir / 100d);
        damage += attacker.attackEarth * ((100f - defender.resEarth) / 100) * (1 + defender.dmgEarth / 100d);
        damage += attacker.attackFire * ((100f - defender.resFire) / 100) * (1 + defender.dmgFire / 100d);
        damage *= (1 + defender.dmg / 100d);
        boolean isCritted = Math.random() <= (float) attacker.criticalStrike / 100;
        if (isCritted) {
            // TODO figure out it crit is done this way or if it is per element separately
            damage = Math.round(damage) * 1.5;
        }
        return Math.toIntExact(Math.round(damage));
    }

    private boolean decideWhoGoesFirst(CombatStats attacker, CombatStats defender) {
        if (attacker.initiative > defender.initiative) {
            return true;
        } else if (attacker.initiative < defender.initiative) {
            return false;
        } else {
            if (attacker.hp > defender.hp) {
                return true;
            } else if (attacker.hp == defender.hp) {
                return Math.random() > 0.5;
            } else {
                return false;
            }
        }

    }

    public boolean winMoreThanXPercentAgainst(CharacterSchema character, MonsterSchema monsterSchema, int percent) {
        CombatStats attacker = CombatStats.fromCharacter(character);
        CombatStats defender = CombatStats.fromMonster(monsterSchema);
        return winMoreThanXPercentAgainst(attacker, defender, percent);
    }
}
