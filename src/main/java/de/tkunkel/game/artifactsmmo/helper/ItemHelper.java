package de.tkunkel.game.artifactsmmo.helper;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.games.artifactsmmo.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ItemHelper {
    private final Caches caches;

    public ItemHelper(Caches caches) {
        this.caches = caches;
    }

    public List<SimpleItemSchema> getRecursiveResourcesToCraft(String itemCode, Integer quantity) {
        List<SimpleItemSchema> rc = new ArrayList<>();
        rc.add(new SimpleItemSchema().code(itemCode)
                                     .quantity(quantity));

        ItemSchema itemToCraft = findItemDefinition(itemCode)
                .get();

        if (itemToCraft.getCraft() != null) {
            for (SimpleItemSchema resource : itemToCraft.getCraft()
                                                        .getItems()) {
                Optional<ItemSchema> itemDefinition = findItemDefinition(resource.getCode());
                if (itemDefinition.get()
                                  .getCraft() == null) {
                    rc.add(new SimpleItemSchema().code(itemDefinition.get()
                                                                     .getCode())
                                                 .quantity(quantity * resource.getQuantity()));
                } else {
                    rc.addAll(getRecursiveResourcesToCraft(resource.getCode(), resource.getQuantity() * quantity));
                }
            }
        }

        rc = collapseSameItems(rc);


        return rc;
    }

    private List<SimpleItemSchema> collapseSameItems(List<SimpleItemSchema> items) {
        List<SimpleItemSchema> rc = new ArrayList<>();

        for (SimpleItemSchema item : items) {
            Optional<SimpleItemSchema> existing = rc.stream()
                                                    .filter(simpleItemSchema -> simpleItemSchema.getCode()
                                                                                                .equals(item.getCode()))
                                                    .findFirst()
                    ;
            if (existing
                    .isPresent()) {
                existing.get()
                        .setQuantity(existing.get()
                                             .getQuantity() + item.getQuantity());
            } else {
                rc.add(item);
            }
        }

        return rc;
    }

    public MapSchema findLocationToCraftItem(String itemToCraft) {
        Optional<ItemSchema> itemSchemaOptional = caches.cachedItems.stream()
                                                                    .filter(item -> item.getCode()
                                                                                        .equals(itemToCraft))
                                                                    .findFirst()
                ;
        if (itemSchemaOptional.isEmpty()) {
            throw new RuntimeException("Item " + itemToCraft + " not found");
        }
        @SuppressWarnings("DataFlowIssue") Optional<MapSchema> map = caches.cachedMap.stream()
                                                                                     .filter(mapSchema -> mapSchema.getInteractions()
                                                                                                                   .getContent() != null)
                                                                                     .filter(mapSchema -> mapSchema.getInteractions()
                                                                                                                   .getContent()
                                                                                                                   .getCode()
                                                                                                                   .equals(itemSchemaOptional.get()
                                                                                                                                             .getCraft()
                                                                                                                                             .getSkill()
                                                                                                                                             .getValue()))
                                                                                     .findFirst()
                ;
        if (map.isEmpty()) {
            throw new RuntimeException("No map found for skill " + itemSchemaOptional.get()
                                                                                     .getCraft()
                                                                                     .getSkill());
        }
        return map.get();
    }

    public Optional<MapSchema> findLocationWhereToFarm(CharacterSchema character, String resourceToFarm) {
        int charX = character.getX();
        int charY = character.getY();
        Optional<ResourceSchema> resourceHavingItem = caches.cachedResources.stream()
                                                                            .filter(resourceSchema -> {
                                                                                boolean isCorrectResource = resourceSchema.getCode()
                                                                                                                          .equalsIgnoreCase(resourceToFarm);
                                                                                boolean hasResource = resourceSchema.getDrops()
                                                                                                                    .stream()
                                                                                                                    .anyMatch(dropRateSchema -> dropRateSchema.getCode()
                                                                                                                                                              .equalsIgnoreCase(resourceToFarm))
                                                                                        ;
                                                                                return isCorrectResource || hasResource;
                                                                            })
                                                                            .findFirst()
                ;
        if (resourceHavingItem.isEmpty()) {
            return Optional.empty();
        }
        Optional<MapSchema> map = caches.cachedMap.stream()
                                                  .filter(mapSchema -> mapSchema.getInteractions()
                                                                                .getContent() != null)
                                                  .filter(mapSchema -> mapSchema.getInteractions()
                                                                                .getContent()
                                                                                .getCode()
                                                                                .equals(resourceHavingItem.get()
                                                                                                          .getCode()))
                                                  .sorted((mapSchema1, mapSchema2) -> {
                                                      int distance1 = Math.abs(mapSchema1.getX() - charX) + Math.abs(mapSchema1.getY() - charY);
                                                      int distance2 = Math.abs(mapSchema2.getX() - charX) + Math.abs(mapSchema2.getY() - charY);
                                                      return distance2 - distance1;
                                                  })

                                                  .findFirst()
                ;
        return map;
    }

    public boolean isHealingOutOfCombatItem(ItemSchema item) {
        return item.getEffects()
                   .stream()
                   .anyMatch(effectSchema -> effectSchema.getCode()
                                                         .equalsIgnoreCase("heal")
                   );

    }

    public Optional<ItemSchema> findItemDefinition(String code) {
        return caches.cachedItems.stream()
                                 .filter(itemSchema -> itemSchema.getCode()
                                                                 .equals(code))
                                 .findFirst();
    }


    public int getHealAmount(ItemSchema item) {
        if (!isHealingOutOfCombatItem(item)) {
            // TODO also respect items inside combat
            return 0;
        }

        return item.getEffects()
                   .stream()
                   .filter(effectSchema -> effectSchema.getCode()
                                                       .equalsIgnoreCase("restore")
                           || effectSchema.getCode()
                                          .equalsIgnoreCase("heal")
                   )
                   .mapToInt(effectSchema -> effectSchema.getValue())
                   .sum();
    }
}
