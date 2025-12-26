package de.tkunkel.game.artifactsmmo.shopping;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.model.DataPageSimpleItemSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WishList {
    private final Logger logger = LoggerFactory.getLogger(WishList.class.getName());
    private final Caches caches;
    private final ApiHolder apiHolder;
    private final Set<Wish> allWishes = new HashSet<>();

    public WishList(Caches caches, ApiHolder apiHolder) {
        this.caches = caches;
        this.apiHolder = apiHolder;
    }

    public void addRequest(Wish wish) {
        if (allWishes.contains(wish)) {
            return;
        }
        if (hasAlreadyInBank(wish)) {
            return;
        }

        this.allWishes.add(wish);
        addWishesForComponents(wish);
        // logger.info("Added wish for {}", wish.itemCode);
    }

    private boolean hasAlreadyInBank(Wish wish) {
        try {
            AtomicInteger totals = new AtomicInteger();
            DataPageSimpleItemSchema bankItemsMyBankItemsGet = apiHolder.myAccountApi.getBankItemsMyBankItemsGet(wish.itemCode, 1, 100);
            bankItemsMyBankItemsGet.getData()
                                   .stream()
                                   .forEach(bankItem -> {
                                       totals.addAndGet(bankItem.getQuantity());
                                   })
            ;
            if (totals.get() >= wish.amount) {
                return true;
            }
            return false;
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void addWishesForComponents(Wish wish) {
        Optional<ItemSchema> itemDefinition = caches.findItemDefinition(wish.itemCode);
        if (itemDefinition.isEmpty()) {
            logger.error("Item {} not found", wish.itemCode);
            return;
        }

        if (itemDefinition.get()
                          .getCraft() == null) {
            return;
        }
        itemDefinition.get()
                      .getCraft()
                      .getItems()
                      .forEach(component -> {
                          addRequest(new Wish(wish.characterName, component.getCode(), component.getQuantity()));
                      })
        ;
    }

    public Set<Wish> getAllWishes() {
        return Collections.unmodifiableSet(allWishes);
    }
}
