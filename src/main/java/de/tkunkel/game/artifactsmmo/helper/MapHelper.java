package de.tkunkel.game.artifactsmmo.helper;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MapHelper {
    private final Caches caches;

    public MapHelper(Caches caches) {
        this.caches = caches;
    }

    public Optional<MapSchema> findClosestLocation(CharacterResponseSchema character, String activity) {
        AtomicReference<Optional<MapSchema>> rc = new AtomicReference<>(Optional.empty());

        int charX = character.getData()
                             .getX();
        int charY = character.getData()
                             .getY();
        caches.cachedMap.stream()
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent() != null)
                        .filter(mapSchema -> mapSchema.getInteractions()
                                                      .getContent()
                                                      .getCode()
                                                      .equals(activity))
                        .sorted((mapSchema1, mapSchema2) -> {
                            int distance1 = Math.abs(mapSchema1.getX() - charX) + Math.abs(mapSchema1.getY() - charY);
                            int distance2 = Math.abs(mapSchema2.getX() - charX) + Math.abs(mapSchema2.getY() - charY);
                            return distance2 - distance1;
                        })
                        .forEach(mapSchema -> rc.set(Optional.of(mapSchema)))
        ;
        return rc.get();
    }

}
