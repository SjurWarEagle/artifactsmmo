package de.tkunkel.game.artifactsmmo.helper;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.games.artifactsmmo.model.NPCItem;
import de.tkunkel.games.artifactsmmo.model.NPCSchema;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NpcHelper {

    private final Caches caches;

    public NpcHelper(Caches caches) {
        this.caches = caches;
    }

    public Optional<NPCSchema> findNpcThatSells(String code) {
        Optional<NPCItem> optionalNPCItem = caches.cachedNpcItems.stream()
                                                                 .filter(npcItem -> npcItem.getCode()
                                                                                           .equals(code))
                                                                 .findFirst()
                ;
        if (optionalNPCItem.isEmpty()) {
            return Optional.empty();
        }
        return caches.cachedNpcs.stream()
                                .filter(npcSchema -> optionalNPCItem.get()
                                                                    .getNpc()
                                                                    .equalsIgnoreCase(npcSchema.getCode()))
                                .findFirst();
    }

    // TODO I'm not sure about this one. it was created to exclude farmable items from buyable items, but I do not like the way I did it.
    public Optional<NPCSchema> findNpcThatSellsExcludeGold(String code) {
        Optional<NPCItem> optionalNPCItem = caches.cachedNpcItems.stream()
                                                                 .filter(npcItem -> npcItem.getCode()
                                                                                           .equals(code))
                                                                 .filter(npcItem -> !npcItem.getCurrency()
                                                                                            .equalsIgnoreCase("gold"))
                                                                 .findFirst()
                ;
        if (optionalNPCItem.isEmpty()) {
            return Optional.empty();
        }
        return caches.cachedNpcs.stream()
                                .filter(npcSchema -> optionalNPCItem.get()
                                                                    .getNpc()
                                                                    .equalsIgnoreCase(npcSchema.getCode()))
                                .findFirst();
    }

}
