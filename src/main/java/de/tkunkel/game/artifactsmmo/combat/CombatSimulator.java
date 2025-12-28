package de.tkunkel.game.artifactsmmo.combat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CombatSimulator {
    int maxRounds = 100;
    private final Logger logger = LoggerFactory.getLogger(CombatSimulator.class.getName());

    @Cacheable(cacheNames = "combatSimulatorWinPercentage")
    public boolean winMoreThanXPercentAgainst(CombatStats attacker, CombatStats defender, int percent) {
        int wins = 0;
        for (int i = 0; i < 100; i++) {
            boolean result = attackerWinsSimulatedCombat(attacker, defender);
            if (result) {
                wins++;
                wins++;
            }
        }

        return wins >= percent;
    }

    public boolean attackerWinsSimulatedCombat(CombatStats attacker, CombatStats defender) {
        int attackerHp = attacker.hp;
        int defenderHp = defender.hp;
        boolean attackerActsFirst = decideWhoGoesFirst(attacker, defender);
        for (int round = 1; round <= maxRounds; round++) {
            int attackerDamage = calculateDamage(attacker, defender);
            int defenderDamage = calculateDamage(defender, attacker);

            if (attackerActsFirst) {
                defenderHp -= attackerDamage;
                logger.info("turn {}: attacker hit for {}, Defender: {}/{}", round, attackerDamage, defenderHp, defender.hp);
                if (defenderHp <= 0) {
                    return true;
                }
                attackerHp -= defenderDamage;
                logger.info("turn {}: defender hit for {}, Attacker: {}/{}", round, defenderDamage, attackerHp, attacker.hp);
                if (attackerHp <= 0) {
                    return false;
                }
            } else {
                attackerHp -= defenderDamage;
                logger.info("turn {}: defender hit for {}, Attacker: {}/{}", round, defenderDamage, attackerHp, attacker.hp);
                if (attackerHp <= 0) {
                    return false;
                }
                defenderHp -= attackerDamage;
                logger.info("turn {}: attacker hit for {}, Defender: {}/{}", round, attackerDamage, defenderHp, defender.hp);
                if (defenderHp <= 0) {
                    return true;
                }
            }
        }

        return attackerHp > defenderHp;
    }

    private int calculateDamage(CombatStats attacker, CombatStats defender) {
        double damage = 0;
        damage += attacker.attackWater * ((100f - defender.resWater) / 100);
        damage += attacker.attackAir * ((100f - defender.resAir) / 100);
        damage += attacker.attackEarth * ((100f - defender.resEarth) / 100);
        damage += attacker.attackFire * ((100f - defender.resFire) / 100);
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
}
