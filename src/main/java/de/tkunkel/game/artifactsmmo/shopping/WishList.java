package de.tkunkel.game.artifactsmmo.shopping;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.combat.CombatSimulator;
import de.tkunkel.game.artifactsmmo.combat.CombatStats;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.game.artifactsmmo.helper.MonsterHelper;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import de.tkunkel.game.artifactsmmo.tasks.BankFetchItemsAndCraftTask;
import de.tkunkel.game.artifactsmmo.tasks.HuntForItemTask;
import de.tkunkel.games.artifactsmmo.model.*;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WishList {
    private final Logger logger = LoggerFactory.getLogger(WishList.class.getName());
    private final Caches caches;
    private final ApiHolder apiHolder;
    private final ItemHelper itemHelper;
    private final CharHelper charHelper;
    private final HuntForItemTask huntForItemTask;
    private final CombatSimulator combatSimulator;
    private final Set<Wish> allWishes = new CopyOnWriteArraySet<>();
    private final BankDepositAllTask bankDepositAllTask;
    private final BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask;
    private final MyAccountApiWrapper myAccountApi;
    private final MonsterHelper monsterHelper;

    public WishList(Caches caches, ApiHolder apiHolder, ItemHelper itemHelper, CharHelper charHelper, HuntForItemTask huntForItemTask, CombatSimulator combatSimulator, BankDepositAllTask bankDepositAllTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask, MyAccountApiWrapper myAccountApi, MonsterHelper monsterHelper) {
        this.caches = caches;
        this.apiHolder = apiHolder;
        this.itemHelper = itemHelper;
        this.charHelper = charHelper;
        this.huntForItemTask = huntForItemTask;
        this.combatSimulator = combatSimulator;
        this.bankDepositAllTask = bankDepositAllTask;
        this.bankFetchItemsAndCraftTask = bankFetchItemsAndCraftTask;
        this.myAccountApi = myAccountApi;
        this.monsterHelper = monsterHelper;
    }


    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void fillStorage() {
        requestItemsForStorage("copper_ore", 500);
        requestItemsForStorage("copper_bar", 100);
    }

    public boolean isHandlingHuntingWish(CharacterResponseSchema character) {
        Optional<Wish> wishThatCanBeHuntedByMe = reserveWishThatCanBeHuntedByMe(character);
        if (wishThatCanBeHuntedByMe.isEmpty()) {
            return false;
        }
        Wish wish = wishThatCanBeHuntedByMe.get();
        boolean enoughInInventory = charHelper.cntItemsInInventory(character.getData()
                                                                            .getName(), wish.itemCode
        ) >= wish.amount;
        while (!enoughInInventory) {
            huntForItemTask.huntForItem(character.getData()
                                                 .getName(), wish.itemCode
            );
            enoughInInventory = charHelper.cntItemsInInventory(character.getData()
                                                                        .getName(), wish.itemCode
            ) >= wish.amount;
        }

        bankDepositAllTask.depositItemInBank(character, wish.itemCode, wish.amount);
        wish.fulfilled = true;
        wish.reservedBy = null;

        return false;
    }


    public void removePreviousWishes(String item) {
        List<Wish> toRemove = new ArrayList<>();
        allWishes.stream()
                 .filter(wish -> wish.reservedBy == null)
                 .filter(wish -> wish.itemCode.startsWith("storage " + item))
                 .forEach(toRemove::add)
        ;

        allWishes.removeAll(toRemove);
    }

    private boolean checkIfAllResourcesAreAvailable(CharacterResponseSchema character, @UnknownNullability Optional<Wish> optionalWish) {
        if (optionalWish.isEmpty()) {
            return false;
        }
        Wish wish = optionalWish.get();
        Optional<ItemSchema> itemDefinition = itemHelper.findItemDefinition(wish.itemCode);
        return itemDefinition.get()
                             .getCraft()
                             .getItems()
                             .stream()
                             .allMatch(resourceItem -> {
                                 boolean inInventory = character.getData()
                                                                .getInventory()
                                                                .stream()
                                                                .filter(inventorySlot -> inventorySlot.getCode()
                                                                                                      .equals(resourceItem.getCode()))
                                                                .findAny()
                                                                .isPresent()
                                         ;
                                 if (inInventory) {
                                     return true;
                                 }
                                 boolean inBank = myAccountApi.getBankItemsMyBankItemsGet(resourceItem.getCode(), 1, 100
                                                              )
                                                              .getData()
                                                              .size() > 0;
                                 return inBank;
                             })
                ;


    }


    public boolean isHandlingCraftingWish(CharacterResponseSchema character) {
        Optional<Wish> wishThatCanBeCraftedByMe = reserveWishThatCanBeCraftedByMe(character);
        boolean allResourcesAvailable = checkIfAllResourcesAreAvailable(character, wishThatCanBeCraftedByMe);
        if (allResourcesAvailable && wishThatCanBeCraftedByMe.isPresent()) {
            Wish wish = wishThatCanBeCraftedByMe.get();
            bankFetchItemsAndCraftTask.craftItemWithBankItems(character.getData(), wish.itemCode, wish.amount);
            wish.fulfilled = true;
            wish.reservedBy = null;
            return true;
        } else {
            return false;
        }
    }


    public void requestInSmallerPackages(String item, int remainingAmount) {
        // request in smaller packages
        int packages = remainingAmount / 10;
        for (int i = 0; i < packages; i++) {
            addRequest(new Wish("storage " + item + " " + i, item, 10), true);
        }
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
        itemDefinition.get()
                      .getCraft()
                      .getItems()
                      .forEach(component -> addRequest(new Wish(wish.characterName, component.getCode(), component.getQuantity())
                              , false
                      ))
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
                                   Optional<ItemSchema> item = itemHelper.findItemDefinition(wish.itemCode);
                                   if (item.isEmpty()) {
                                       return false;
                                   }
                                   return item.get()
                                              .getCraft() == null;
                               })
                               .filter(item -> {
                                   List<MonsterSchema> monsterSchemas = monsterHelper.findMonstersThatDropThis(item.itemCode);
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
                Optional<ItemSchema> itemDefinition = itemHelper.findItemDefinition(wish.itemCode);
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
                boolean charHasSkill = charHelper.charHasRequiredSkillLevel(character.getData(), requiredSkillName, requiredSkillLevel);

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

    public void requestItemsForStorage(String item, int amount) {
        removePreviousWishes(item);
        int inBank = charHelper.cntItemsInBank(item);
        // check what is already in bank and only request what is missing
        int remainingAmount = amount - inBank;

        if (remainingAmount > 0) {
            requestInSmallerPackages(item, remainingAmount);
        }
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
