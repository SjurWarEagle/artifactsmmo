package de.tkunkel.game.artifactsmmo.combat;

import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.MonsterSchema;

public class CombatStats {
    protected int hp;
    protected int dmg;
    protected int criticalStrike;
    protected int initiative;
    protected int attackEarth;
    protected int resEarth;
    protected int attackFire;
    protected int resFire;
    protected int attackAir;
    protected int resAir;
    protected int attackWater;
    protected int resWater;
    protected int dmgAir;
    protected int dmgEarth;
    protected int dmgWater;
    protected int dmgFire;

    public static CombatStats fromCharacter(CharacterSchema character) {
        var rc = new CombatStats();
        rc.hp = character.getMaxHp();
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

        rc.dmgEarth = character.getDmgEarth();
        rc.dmgAir = character.getDmgAir();
        rc.dmgWater = character.getDmgWater();
        rc.dmgFire = character.getDmgFire();
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

    @Override
    public String toString() {
        return "CombatStats{" +
                "hp=" + hp +
                ", criticalStrike=" + criticalStrike +
                ", initiative=" + initiative +
                ", attackEarth=" + attackEarth +
                ", resEarth=" + resEarth +
                ", attackFire=" + attackFire +
                ", resFire=" + resFire +
                ", attackAir=" + attackAir +
                ", resAir=" + resAir +
                ", attackWater=" + attackWater +
                ", resWater=" + resWater +
                '}';
    }

    public CombatStats() {
    }

    public CombatStats(CombatStats org) {
        this.hp = org.hp;
        this.criticalStrike = org.criticalStrike;
        this.dmg = org.dmg;

        this.dmgAir = org.dmgAir;
        this.dmgFire = org.dmgFire;
        this.dmgWater = org.dmgWater;
        this.dmgEarth = org.dmgEarth;

        this.resAir = org.resAir;
        this.resFire = org.resFire;
        this.resWater = org.resWater;
        this.resEarth = org.resEarth;

        this.attackAir = org.attackAir;
        this.attackFire = org.attackFire;
        this.attackWater = org.attackWater;
        this.attackEarth = org.attackEarth;
    }
}
