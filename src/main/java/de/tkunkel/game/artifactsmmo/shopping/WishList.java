package de.tkunkel.game.artifactsmmo.shopping;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.DataPageSimpleItemSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WishList {
    private final Logger logger = LoggerFactory.getLogger(WishList.class.getName());
    private final Caches caches;
    private final ApiHolder apiHolder;
    private final Set<Wish> allWishes = new CopyOnWriteArraySet<>();

    public WishList(Caches caches, ApiHolder apiHolder) {
        this.caches = caches;
        this.apiHolder = apiHolder;
    }

    public void addRequest(Wish wish) {
        if (allWishes.stream()
                     .anyMatch(existingWish -> existingWish.itemCode.equals(wish.itemCode)
                             && existingWish.characterName.equalsIgnoreCase(wish.characterName)
                             && !existingWish.fulfilled
                     )) {
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
        AtomicInteger totals = new AtomicInteger();
        // TODO add paging
        DataPageSimpleItemSchema bankItemsMyBankItemsGet = apiHolder.myAccountApi.getBankItemsMyBankItemsGet(null, 1, 100);
        bankItemsMyBankItemsGet.getData()
                               .stream()
                               .filter(simpleItemSchema -> simpleItemSchema.getCode()
                                                                           .equals(wish.itemCode))
                               .forEach(bankItem -> totals.addAndGet(bankItem.getQuantity()))
        ;
        if (totals.get() >= wish.amount) {
            return true;
        }
        return false;
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

    public synchronized Optional<Wish> reserveWishThatCanBeCraftedByMe(CharacterResponseSchema character) {
        Optional<Wish> existingReservedWish = allWishes.stream()
                                                       .filter(wish -> character.getData()
                                                                                .getName()
                                                                                .equalsIgnoreCase(wish.reservedBy))
                                                       .findFirst()
                ;
        if (existingReservedWish.isPresent()) {
            return existingReservedWish;
        }

        for (Wish wish : allWishes) {
            if (!wish.fulfilled
                    && wish.reservedBy == null) {
                Optional<ItemSchema> itemDefinition = caches.findItemDefinition(wish.itemCode);
                if (itemDefinition.isEmpty()) {
                    continue;
                }
                if (itemDefinition.get()
                                  .getCraft() == null) {
                    // nothing to craft, this one needs to be gathered
                    continue;
                }
                String requiredSkillName = itemDefinition.get()
                                                         .getCraft()
                                                         .getSkill()
                                                         .getValue()
                        ;
                int requiredSkillLevel = itemDefinition.get()
                                                       .getCraft()
                                                       .getLevel()
                        ;
                boolean charHasSkill = CharHelper.charHasRequiredSkillLevel(character.getData(), requiredSkillName, requiredSkillLevel);

                boolean isResourcesAtBank = areAllItemsInBank(itemDefinition.get()
                                                                            .getCraft()
                                                                            .getItems());
                if (charHasSkill && isResourcesAtBank) {
                    wish.reservedBy = character.getData()
                                               .getName();
                    return Optional.of(wish);
                }
            }
        }
        return Optional.empty();
    }

    private boolean areAllItemsInBank(List<SimpleItemSchema> items) {
        DataPageSimpleItemSchema bankItemsMyBankItemsGet = apiHolder.myAccountApi.getBankItemsMyBankItemsGet(null, 1, 100);
        return items.stream()
                    .allMatch(simpleItemSchema -> {
                        return bankItemsMyBankItemsGet.getData()
                                                      .stream()
                                                      .anyMatch(bankItem -> bankItem.getCode()
                                                                                    .equalsIgnoreCase(simpleItemSchema.getCode()));
                    });
    }
}
