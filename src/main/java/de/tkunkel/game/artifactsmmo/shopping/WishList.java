package de.tkunkel.game.artifactsmmo.shopping;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.games.artifactsmmo.model.DataPageSimpleItemSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WishList {
    private final Logger logger = LoggerFactory.getLogger(WishList.class.getName());
    private final ApiHolder apiHolder;
    private final ItemHelper itemHelper;
    public final Set<Wish> allWishes = new CopyOnWriteArraySet<>();

    public WishList(ApiHolder apiHolder, ItemHelper itemHelper) {
        this.apiHolder = apiHolder;
        this.itemHelper = itemHelper;
    }


    @SuppressWarnings("unused")
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void fillStorage() {
        this.allWishes.stream();
        var cleanedWishes = allWishes.stream()
                                     .filter(wish -> !wish.fulfilled)
                                     .filter(wish -> wish.amount <= 0)
                                     .toList()
                ;
        allWishes.clear();
        allWishes.addAll(cleanedWishes);
    }

    public void addRequest(Wish wish, boolean ignoreBankCheck) {
        if (allWishes.stream()
                     .anyMatch(existingWish -> existingWish.itemCode.equals(wish.itemCode)
                             && existingWish.characterName.equalsIgnoreCase(wish.characterName)
                             && !existingWish.fulfilled
                     )) {
            return;
        }
        if (!ignoreBankCheck && hasAlreadyInBank(wish)) {
            return;
        }

        this.allWishes.add(wish);
        addWishesForComponents(wish);
    }

    private boolean hasAlreadyInBank(Wish wish) {
        AtomicInteger totals = new AtomicInteger();
        DataPageSimpleItemSchema bankItemsMyBankItemsGet = apiHolder.myAccountApi.getBankItemsMyBankItemsGet(null, 1, 100);
        bankItemsMyBankItemsGet.getData()
                               .stream()
                               .filter(simpleItemSchema -> simpleItemSchema.getCode()
                                                                           .equals(wish.itemCode))
                               .forEach(bankItem -> totals.addAndGet(bankItem.getQuantity()))
        ;

        return totals.get() >= wish.amount;
    }

    private void addWishesForComponents(Wish wish) {
        Optional<ItemSchema> itemDefinition = itemHelper.findItemDefinition(wish.itemCode);
        if (itemDefinition.isEmpty()) {
            logger.error("Item {} not found", wish.itemCode);
            return;
        }

        if (itemDefinition.get()
                          .getCraft() == null) {
            return;
        }
        List<SimpleItemSchema> neededItems = itemHelper.getRecursiveResourcesToCraft(wish.itemCode, wish.amount);
        neededItems.forEach(component -> addRequest(new Wish(wish.characterName, component.getCode(), component.getQuantity()), false));
    }

    public Set<Wish> getAllWishes() {
        return Collections.unmodifiableSet(allWishes);
    }

}
