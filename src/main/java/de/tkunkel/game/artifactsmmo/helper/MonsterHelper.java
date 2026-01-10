package de.tkunkel.game.artifactsmmo.helper;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.games.artifactsmmo.model.MonsterSchema;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonsterHelper {

    private final Caches caches;

    public MonsterHelper(Caches caches) {
        this.caches = caches;
    }

    public List<MonsterSchema> findMonstersThatDropThis(String code) {
        return caches.cachedMonsters.stream()
                                    .filter(monsterSchema -> monsterSchema.getDrops()
                                                                          .stream()
                                                                          .anyMatch(dropSchema -> dropSchema.getCode()
                                                                                                            .equals(code)))
                                    .toList();
    }


}
