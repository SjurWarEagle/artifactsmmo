package de.tkunkel.game.artifactsmmo.combat;

import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.MonsterSchema;

public class CombatStats {
    public int hp;
    public int criticalStrike;
    public int initiative;
    public int attackEarth;
    public int resEarth;
    public int attackFire;
    public int resFire;
    public int attackAir;
    public int resAir;
    public int attackWater;
    public int resWater;

    public static CombatStats fromCharacter(CharacterSchema character) {
        var rc = new CombatStats();
        rc.hp = character.getHp();
        // TODO add support for dmg (overall bonus)
        rc.attackEarth = character.getAttackEarth();
        rc.attackAir = character.getAttackAir();
        rc.attackWater = character.getAttackWater();
        rc.attackFire = character.getAttackFire();

        rc.criticalStrike = character.getCriticalStrike();
        rc.initiative = character.getInitiative();

        rc.resEarth = character.getResEarth();
        rc.resAir = character.getResAir();
        rc.resWater = character.getResWater();
        rc.resFire = character.getResFire();
        return rc;
    }

    public static CombatStats fromMonster(MonsterSchema monster) {
        var rc = new CombatStats();
        rc.hp = monster.getHp();
        rc.attackEarth = monster.getAttackEarth();
        rc.attackAir = monster.getAttackAir();
        rc.attackWater = monster.getAttackWater();
        rc.attackFire = monster.getAttackFire();

        rc.criticalStrike = monster.getCriticalStrike();
        rc.initiative = monster.getInitiative();

        rc.resEarth = monster.getResEarth();
        rc.resAir = monster.getResAir();
        rc.resWater = monster.getResWater();
        rc.resFire = monster.getResFire();
        return rc;
    }
}
