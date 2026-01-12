package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.combat.CombatSimulator;
import de.tkunkel.game.artifactsmmo.combat.CombatStats;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.game.artifactsmmo.helper.MonsterHelper;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hunts a monsters until an item drops
 */
@Service
public class HuntForItemTask {
    private final Logger logger = LoggerFactory.getLogger(HuntForItemTask.class.getName());
    private final CharactersApiWrapper charactersApiWrapper;
    private final CharHelper charHelper;
    private final MapHelper mapHelper;
    private final MonsterHelper monsterHelper;
    private final MyCharactersApiWrapper myCharactersApi;
    private final CombatSimulator combatSimulator;

    public HuntForItemTask(CharactersApiWrapper charactersApiWrapper, CharHelper charHelper, MyCharactersApiWrapper myCharactersApi,
                           MapHelper mapHelper, MonsterHelper monsterHelper, CombatSimulator combatSimulator
    ) {
        this.charactersApiWrapper = charactersApiWrapper;
        this.charHelper = charHelper;
        this.myCharactersApi = myCharactersApi;
        this.mapHelper = mapHelper;
        this.monsterHelper = monsterHelper;
        this.combatSimulator = combatSimulator;
    }

    public int huntForItem(String characterName, String itemToHunt) {
        CharacterResponseSchema character = charactersApiWrapper.getCharacterCharactersNameGet(characterName);
        List<MonsterSchema> monstersThatDropThis = monsterHelper.findMonstersThatDropThis(itemToHunt);
        Optional<MonsterSchema> optionalTarget = monstersThatDropThis.stream()
                                                                     .filter(monsterSchema -> {
                                                                         CombatStats attacker = CombatStats.fromCharacter(character.getData());
                                                                         CombatStats defender = CombatStats.fromMonster(monsterSchema);
                                                                         return combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 90);
                                                                     })
                                                                     .findFirst()
                ;
        if (optionalTarget.isEmpty()) {
            return 0;
        }
        MonsterSchema target = optionalTarget.get();
        Optional<MapSchema> monsterSpawns = mapHelper.findClosestLocationWhereMonsterSpawns(character.getData(), target.getCode());
        if (monsterSpawns.isEmpty()) {
            return 0;
        }
        AtomicInteger totalDrops = new AtomicInteger();
        MapSchema spawnLocation = monsterSpawns.get();
        charHelper.waitUntilCooldownDone(characterName);
        charHelper.moveToLocationSync(characterName, spawnLocation);

        while (totalDrops.get() <= 0) {
            charHelper.waitUntilCooldownDone(characterName);
            charHelper.healIfNeededSync(characterName);
            charHelper.waitUntilCooldownDone(characterName);
            CharacterFightResponseSchema fightResponse = myCharactersApi.actionFightMyNameActionFightPost(characterName, new FightRequestSchema());
            charHelper.waitUntilCooldownDone(fightResponse.getData()
                                                          .getCooldown());
            for (CharacterMultiFightResultSchema characterMultiFightResultSchema : fightResponse.getData()
                                                                                                .getFight()
                                                                                                .getCharacters()) {
                for (DropSchema dropSchema : characterMultiFightResultSchema.getDrops()) {
                    if (dropSchema.getCode()
                                  .equalsIgnoreCase(itemToHunt)) {
                        totalDrops.addAndGet(dropSchema.getQuantity());
                    }
                }
            }
        }
        return totalDrops.get();
    }

}
