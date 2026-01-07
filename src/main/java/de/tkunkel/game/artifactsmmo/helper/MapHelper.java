package de.tkunkel.game.artifactsmmo.helper;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.api.AccountsApiWrapper;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.ConditionSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MapHelper {
    private final Caches caches;
    private final AccountsApiWrapper accountsApiWrapper;
    private AccountHelper accountHelper;

    public MapHelper(Caches caches, AccountsApiWrapper accountsApiWrapper, AccountHelper accountHelper) {
        this.caches = caches;
        this.accountsApiWrapper = accountsApiWrapper;
        this.accountHelper = accountHelper;
    }

    public Optional<MapSchema> findClosestLocationWhereMonsterSpawns(CharacterSchema character, String monsterCode) {
        int charX = character.getX();
        int charY = character.getY();
        return caches.cachedMap.stream()
                               .filter(mapSchema -> mapSchema.getInteractions()
                                                             .getContent() != null)
                               .filter(mapSchema -> mapSchema.getInteractions()
                                                             .getContent()
                                                             .getCode()
                                                             .equals(monsterCode))
                               .sorted((mapSchema1, mapSchema2) -> {
                                   int distance1 = Math.abs(mapSchema1.getX() - charX) + Math.abs(mapSchema1.getY() - charY);
                                   int distance2 = Math.abs(mapSchema2.getX() - charX) + Math.abs(mapSchema2.getY() - charY);
                                   return distance2 - distance1;
                               })
                               .findFirst()
                ;
    }

    public Optional<MapSchema> findClosestLocation(CharacterResponseSchema character, String activity) {
        int charX = character.getData()
                             .getX();
        int charY = character.getData()
                             .getY();
        return caches.cachedMap.stream()
                               .filter(mapSchema -> mapSchema.getInteractions()
                                                             .getContent() != null)
                               .filter(mapSchema -> mapSchema.getInteractions()
                                                             .getContent()
                                                             .getCode()
                                                             .equals(activity))
                               .filter(mapSchema -> canEnterRegion(mapSchema))
                               .sorted((mapSchema1, mapSchema2) -> {
                                   int distance1 = Math.abs(mapSchema1.getX() - charX) + Math.abs(mapSchema1.getY() - charY);
                                   int distance2 = Math.abs(mapSchema2.getX() - charX) + Math.abs(mapSchema2.getY() - charY);
                                   return distance2 - distance1;
                               })
                               .findFirst()
                ;
    }

    private boolean canEnterRegion(MapSchema mapSchema) {
        List<ConditionSchema> conditions = mapSchema.getAccess()
                                                    .getConditions();
        if (conditions == null) {
            return true;
        }
        return conditions.stream()
                         .allMatch(conditionSchema -> accountHelper.isFullfilled(conditionSchema));
    }


}
