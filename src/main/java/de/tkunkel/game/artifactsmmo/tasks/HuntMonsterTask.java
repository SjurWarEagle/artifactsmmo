package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.combat.CombatSimulator;
import de.tkunkel.game.artifactsmmo.combat.CombatStats;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.game.artifactsmmo.helper.MonsterHelper;
import de.tkunkel.games.artifactsmmo.model.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Hunts a monsters until an item drops
 */
@Service
public class HuntMonsterTask {
    private final CharactersApiWrapper charactersApiWrapper;
    private final MyAccountApiWrapper myAccountApiWrapper;
    private final Caches caches;
    private final ItemHelper itemHelper;
    private final CharHelper charHelper;
    private final MapHelper mapHelper;
    private final MonsterHelper monsterHelper;
    private final MyCharactersApiWrapper myCharactersApi;
    private final CombatSimulator combatSimulator;

    public HuntMonsterTask(CharactersApiWrapper charactersApiWrapper, MyAccountApiWrapper myAccountApiWrapper, Caches caches, ItemHelper itemHelper, CharHelper charHelper, MyCharactersApiWrapper myCharactersApi, MapHelper mapHelper, MonsterHelper monsterHelper, CombatSimulator combatSimulator) {
        this.charactersApiWrapper = charactersApiWrapper;
        this.myAccountApiWrapper = myAccountApiWrapper;
        this.caches = caches;
        this.itemHelper = itemHelper;
        this.charHelper = charHelper;
        this.myCharactersApi = myCharactersApi;
        this.mapHelper = mapHelper;
        this.monsterHelper = monsterHelper;
        this.combatSimulator = combatSimulator;
    }

    public boolean hunt(String characterName, String monsterToHunt) {
        CharacterResponseSchema character = charactersApiWrapper.getCharacterCharactersNameGet(characterName);
        Optional<MonsterSchema> monster = caches.cachedMonsters.stream()
                                                               .filter(monsterSchema -> monsterSchema.getCode()
                                                                                                     .equalsIgnoreCase(monsterToHunt))
                                                               .filter(monsterSchema -> {
                                                                   CombatStats attacker = CombatStats.fromCharacter(character.getData());
                                                                   CombatStats defender = CombatStats.fromMonster(monsterSchema);
                                                                   return combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 90);
                                                               })
                                                               .findFirst()
                ;
        if (monster.isEmpty()) {
            return false;
        }
        MonsterSchema target = monster.get();
        Optional<MapSchema> monsterSpawns = mapHelper.findClosestLocationWhereMonsterSpawns(character.getData(), target.getCode());

        MapSchema spawnLocation = monsterSpawns.get();
        charHelper.waitUntilCooldownDone(characterName);
        charHelper.moveToLocationSync(characterName, spawnLocation);

        charHelper.waitUntilCooldownDone(characterName);
        charHelper.healIfNeededSync(characterName);
        charHelper.waitUntilCooldownDone(characterName);
        CharacterFightResponseSchema fightResponse = myCharactersApi.actionFightMyNameActionFightPost(characterName, new FightRequestSchema());
        charHelper.waitUntilCooldownDone(fightResponse.getData()
                                                      .getCooldown());
        return true;
    }

}
