package de.tkunkel.game.artifactsmmo.shopping;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.combat.CombatSimulator;
import de.tkunkel.game.artifactsmmo.combat.CombatStats;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.game.artifactsmmo.tasks.HuntForItemTask;
import de.tkunkel.games.artifactsmmo.model.*;
import org.jspecify.annotations.NonNull;
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
    private final ItemHelper itemHelper;
    private final HuntForItemTask huntForItemTask;
    private final CombatSimulator combatSimulator;
    private final Set<Wish> allWishes = new CopyOnWriteArraySet<>();

    public WishList(Caches caches, ApiHolder apiHolder, ItemHelper itemHelper, HuntForItemTask huntForItemTask, CombatSimulator combatSimulator) {
        this.caches = caches;
        this.apiHolder = apiHolder;
        this.itemHelper = itemHelper;
        this.huntForItemTask = huntForItemTask;
        this.combatSimulator = combatSimulator;
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
                      .forEach(component -> addRequest(new Wish(wish.characterName, component.getCode(), component.getQuantity())))
        ;
    }

    public Set<Wish> getAllWishes() {
        return Collections.unmodifiableSet(allWishes);
    }

    public synchronized Optional<Wish> reserveWishThatCanBeHuntedByMe(CharacterResponseSchema character) {
        Optional<Wish> existingReservedWish = getAlreadyReservedWish(character);
        if (existingReservedWish.isPresent()) {
            return existingReservedWish;
        }
        var newWish = allWishes.stream()
                               .filter(wish -> !wish.fulfilled)
                               .filter(wish -> wish.reservedBy == null)
                               .filter(wish -> {
                                   Optional<ItemSchema> item = caches.findItemDefinition(wish.itemCode);
                                   if (item.isEmpty()) {
                                       return false;
                                   }
                                   return item.get()
                                              .getCraft() == null;
                               })
                               .filter(item -> {
                                   List<MonsterSchema> monsterSchemas = caches.findMonstersThatDropThis(item.itemCode);
                                   for (MonsterSchema monsterSchema : monsterSchemas) {
                                       CombatStats attacker = CombatStats.fromCharacter(character.getData());
                                       CombatStats defender = CombatStats.fromMonster(monsterSchema);
                                       boolean canWin = combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 90);
                                       return canWin;
                                   }
                                   return false;
                               })
                               .findFirst()
                ;
        if (newWish.isEmpty()) {
            return Optional.empty();
        }
        newWish.get().reservedBy = character.getData()
                                            .getName();

        return newWish;


    }

    public synchronized Optional<Wish> reserveWishThatCanBeCraftedByMe(CharacterResponseSchema character) {
        Optional<Wish> existingReservedWish = getAlreadyReservedWish(character);
        if (existingReservedWish.isPresent()) {
            return existingReservedWish;
        }

        for (Wish wish : allWishes) {
            if (!wish.fulfilled
                    && wish.reservedBy == null) {
                Optional<ItemSchema> itemDefinition = caches.findItemDefinition(wish.itemCode);
                if (itemDefinition.isEmpty()
                        || itemDefinition.get()
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

    private @NonNull Optional<Wish> getAlreadyReservedWish(CharacterResponseSchema character) {
        Optional<Wish> existingReservedWish = allWishes.stream()
                                                       .filter(wish -> !wish.fulfilled)
                                                       .filter(wish -> character.getData()
                                                                                .getName()
                                                                                .equalsIgnoreCase(wish.reservedBy))
                                                       .findFirst()
                ;
        return existingReservedWish;
    }

    private boolean areAllItemsInBank(List<SimpleItemSchema> items) {
        DataPageSimpleItemSchema bankItems = apiHolder.myAccountApi.getBankItemsMyBankItemsGet(null, 1, 100);
        return items.stream()
                    .allMatch(simpleItemSchema -> bankItems.getData()
                                                           .stream()
                                                           .anyMatch(bankItem -> bankItem.getCode()
                                                                                         .equalsIgnoreCase(simpleItemSchema.getCode())
                                                                   && bankItem.getQuantity() >= simpleItemSchema.getQuantity()
                                                           )
                    );
    }
}
