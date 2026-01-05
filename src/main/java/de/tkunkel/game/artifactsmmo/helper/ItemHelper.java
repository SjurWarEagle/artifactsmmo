package de.tkunkel.game.artifactsmmo.helper;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
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

        ItemSchema itemToCraft = caches.findItemDefinition(itemCode)
                                       .get();

        if (itemToCraft.getCraft() != null) {
            for (SimpleItemSchema resource : itemToCraft.getCraft()
                                                        .getItems()) {
                Optional<ItemSchema> itemDefinition = caches.findItemDefinition(resource.getCode());
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
}
