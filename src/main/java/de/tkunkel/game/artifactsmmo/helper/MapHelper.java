package de.tkunkel.game.artifactsmmo.helper;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.ConditionSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MapHelper {
    private final Logger logger = LoggerFactory.getLogger(MapHelper.class.getName());

    private final Caches caches;
    private final AccountHelper accountHelper;

    public MapHelper(Caches caches, AccountHelper accountHelper) {
        this.caches = caches;
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

    public Optional<MapSchema> findClosestLocation(CharacterSchema character, String activity) {
        int charX = character.getX();
        int charY = character.getY();
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
                                   return Integer.compare(distance1, distance2);
                               })
                               .findFirst()
                ;
    }

    public Optional<MapSchema> findLocationOfClosestMonster(CharacterSchema character, String monster) {
        logger.info("Starting findClosestMonster");
        AtomicReference<Optional<MapSchema>> rc = new AtomicReference<>(Optional.empty());

        int charX = character.getX();
        int charY = character.getY();
        caches.cachedMap.stream()
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent() != null)
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent()
                                                      .getCode()
                                                      .equals(monster))
                        .sorted((mapSchema1, mapSchema2) -> {
                            int distance1 = Math.abs(mapSchema1.getX() - charX) + Math.abs(mapSchema1.getY() - charY);
                            int distance2 = Math.abs(mapSchema2.getX() - charX) + Math.abs(mapSchema2.getY() - charY);
                            return distance2 - distance1;
                        })
                        .forEach(mapSchema -> rc.set(Optional.of(mapSchema)))
        ;
        return rc.get();
    }

    public Optional<MapSchema> findClosesTaskMaster(CharacterResponseSchema character, String taskMasterType) {
        int charX = character.getData()
                             .getX();
        int charY = character.getData()
                             .getY();
        return caches.cachedMap.stream()
                               .filter(mapSchema -> mapSchema.getInteractions()
                                                             .getContent() != null)
                               .filter(mapSchema -> mapSchema.getInteractions()
                                                             .getContent()
                                                             .getType()
                                                             .getValue()
                                                             .equals("tasks_master"))
                               .filter(mapSchema -> mapSchema.getInteractions()
                                                             .getContent()
                                                             .getCode()
                                                             .equals(taskMasterType))
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
                         .allMatch(conditionSchema -> accountHelper.isFulfilled(conditionSchema));
    }


}
