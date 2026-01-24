package de.tkunkel.game.artifactsmmo.shopping;

import de.tkunkel.game.artifactsmmo.ApiHolder;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.combat.CombatSimulator;
import de.tkunkel.game.artifactsmmo.combat.CombatStats;
import de.tkunkel.game.artifactsmmo.helper.ItemHelper;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.game.artifactsmmo.helper.MonsterHelper;
import de.tkunkel.game.artifactsmmo.helper.NpcHelper;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import de.tkunkel.game.artifactsmmo.tasks.BankFetchItemsAndCraftTask;
import de.tkunkel.game.artifactsmmo.tasks.FarmResourceTask;
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
    private final ApiHolder apiHolder;
    private final ItemHelper itemHelper;
    private final CharHelper charHelper;
    private final HuntForItemTask huntForItemTask;
    private final CombatSimulator combatSimulator;
    private final Set<Wish> allWishes = new CopyOnWriteArraySet<>();
    private final BankDepositAllTask bankDepositAllTask;
    private final BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask;
    private final FarmResourceTask farmResourceTask;
    private final MyAccountApiWrapper myAccountApi;
    private final MonsterHelper monsterHelper;
    private final NpcHelper npcHelper;
    private final MapHelper mapHelper;

    public WishList(ApiHolder apiHolder, ItemHelper itemHelper, CharHelper charHelper, HuntForItemTask huntForItemTask,
                    CombatSimulator combatSimulator, BankDepositAllTask bankDepositAllTask, BankFetchItemsAndCraftTask bankFetchItemsAndCraftTask,
                    FarmResourceTask farmResourceTask, MyAccountApiWrapper myAccountApi, MonsterHelper monsterHelper, NpcHelper npcHelper, MapHelper mapHelper
    ) {
        this.apiHolder = apiHolder;
        this.itemHelper = itemHelper;
        this.charHelper = charHelper;
        this.huntForItemTask = huntForItemTask;
        this.combatSimulator = combatSimulator;
        this.bankDepositAllTask = bankDepositAllTask;
        this.bankFetchItemsAndCraftTask = bankFetchItemsAndCraftTask;
        this.farmResourceTask = farmResourceTask;
        this.myAccountApi = myAccountApi;
        this.monsterHelper = monsterHelper;
        this.npcHelper = npcHelper;
        this.mapHelper = mapHelper;
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

    public boolean isHandlingHuntingWish(CharacterSchema character) {
        Optional<Wish> wishThatCanBeHuntedByMe = reserveWishThatCanBeHuntedByMe(character);
        if (wishThatCanBeHuntedByMe.isEmpty()) {
            return false;
        }
        Wish wish = wishThatCanBeHuntedByMe.get();
        boolean enoughInInventory = charHelper.cntItemsInInventory(character.getName(), wish.itemCode
        ) >= wish.amount;
        while (!enoughInInventory) {
            huntForItemTask.huntForItem(character.getName(), wish.itemCode
            );
            enoughInInventory = charHelper.cntItemsInInventory(character.getName(), wish.itemCode
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

    private boolean checkIfAllResourcesAreAvailable(CharacterSchema character, @UnknownNullability Optional<Wish> optionalWish) {
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
                                 boolean inInventory = character.getInventory()
                                                                .stream()
                                                                .anyMatch(inventorySlot -> inventorySlot.getCode()
                                                                                                        .equals(resourceItem.getCode()))
                                         ;
                                 if (inInventory) {
                                     return true;
                                 }
                                 boolean inBank = !myAccountApi.getBankItemsMyBankItemsGet(resourceItem.getCode(), 1, 100
                                                               )
                                                               .getData()
                                                               .isEmpty();
                                 return inBank;
                             })
                ;


    }


    public boolean isHandlingGatheringWish(CharacterSchema character) {
        Optional<Wish> wishThatCanBeGatheredByMe = reserveWishThatCanGatheredByMe(character);
        if (wishThatCanBeGatheredByMe.isPresent()) {
            Wish wish = wishThatCanBeGatheredByMe.get();
            farmResourceTask.farmResource(character.getName(), wish.itemCode, wish.amount);
            charHelper.waitUntilCooldownDone(character.getName());
            int inInventory = charHelper.cntSpecificItemsInInventory(character.getName(), wish.itemCode);
            if (inInventory >= 10) {
                charHelper.waitUntilCooldownDone(character.getName());
                bankDepositAllTask.depositItemInBank(character, wish.itemCode, 10);
                charHelper.waitUntilCooldownDone(character.getName());
                wish.amount -= 10;
                if (wish.amount <= 0) {
                    wish.amount = 0;
                    wish.fulfilled = true;
                    wish.reservedBy = null;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean isHandlingCraftingWithBankWish(CharacterSchema character) {
        Optional<Wish> wishThatCanBeCraftedByMe = reserveWishThatCanBeCraftedByMeWithBankSupport(character);
        boolean allResourcesAvailable = checkIfAllResourcesAreAvailable(character, wishThatCanBeCraftedByMe);
        if (allResourcesAvailable && wishThatCanBeCraftedByMe.isPresent()) {
            Wish wish = wishThatCanBeCraftedByMe.get();
            // crafting one by one so that the inventory is big enough
            bankFetchItemsAndCraftTask.craftItemWithBankItems(character, wish.itemCode, 1);
            wish.amount -= 1;
            if (wish.amount <= 0) {
                wish.fulfilled = true;
                wish.reservedBy = null;
            }
            return true;
        } else if (wishThatCanBeCraftedByMe.isPresent()) {
            List<SimpleItemSchema> neededItems = itemHelper.getRecursiveResourcesToCraft(wishThatCanBeCraftedByMe.get().itemCode, wishThatCanBeCraftedByMe.get().amount);
            return neededItems.stream()
                              .allMatch(neededItem -> {
                                  boolean canCraft = charHelper.couldCraft(character, neededItem.getCode());
                                  boolean canHarvest = charHelper.canCanGatherResources(character, neededItem.getCode());
                                  boolean itemAlreadyInBank = charHelper.cntItemsInBank(neededItem.getCode()) >= neededItem.getQuantity();
                                  return canCraft || canHarvest || itemAlreadyInBank;
                              });
        } else {
            return false;
        }
    }

    public boolean isHandlingCraftingWish(CharacterSchema character) {
        Optional<Wish> wishThatCanBeCraftedByMe = reserveWishThatCanBeCraftedByMe(character);
        boolean allResourcesAvailable = checkIfAllResourcesAreAvailable(character, wishThatCanBeCraftedByMe);
        if (allResourcesAvailable && wishThatCanBeCraftedByMe.isPresent()) {
            Wish wish = wishThatCanBeCraftedByMe.get();
            // crafting one by one so that the inventory is big enough
            bankFetchItemsAndCraftTask.craftItemWithBankItems(character, wish.itemCode, 1);
            wish.amount -= 1;
            if (wish.amount <= 0) {
                wish.fulfilled = true;
                wish.reservedBy = null;
            }
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
        List<SimpleItemSchema> neededItems = itemHelper.getRecursiveResourcesToCraft(wish.itemCode, wish.amount);
        neededItems.forEach(component -> addRequest(new Wish(wish.characterName, component.getCode(), component.getQuantity()), false));
    }

    public Set<Wish> getAllWishes() {
        return Collections.unmodifiableSet(allWishes);
    }

    public synchronized Optional<Wish> reserveWishThatCanBeHuntedByMe(CharacterSchema character) {
        Optional<Wish> existingReservedWish = getAlreadyReservedWish(character, "hunting");
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
                                   return canHuntForItem(character, item);
                               })
                               .findFirst()
                ;
        if (newWish.isEmpty()) {
            return Optional.empty();
        }
        newWish.get().reservedBy = character.getName();
        newWish.get().wishType = "hunting";
        return newWish;


    }

    private boolean canHuntForItem(CharacterSchema character, Wish item) {
        List<MonsterSchema> monsterSchemas = monsterHelper.findMonstersThatDropThis(item.itemCode);
        for (MonsterSchema monsterSchema : monsterSchemas) {
            CombatStats attacker = CombatStats.fromCharacter(character);
            CombatStats defender = CombatStats.fromMonster(monsterSchema);
            boolean canWin = combatSimulator.winMoreThanXPercentAgainst(attacker, defender, 90);
            return canWin;
        }
        return false;
    }

    public synchronized Optional<Wish> reserveWishThatCanGatheredByMe(CharacterSchema character) {
        Optional<Wish> existingReservedWish = getAlreadyReservedWish(character, "gathering");
        if (existingReservedWish.isPresent()) {
            return existingReservedWish;
        }

        for (Wish wish : allWishes) {
            if (!wish.fulfilled
                    && wish.reservedBy == null) {
                Optional<ItemSchema> itemDefinition = itemHelper.findItemDefinition(wish.itemCode);
                if (itemDefinition.isEmpty()) {
                    continue;
                }
                boolean isCraftable = itemDefinition.get()
                                                    .getCraft() != null;
                if (isCraftable) {
                    continue;
                }
                // FIXME better cange logic to check resorufes on map if farming place is available
                boolean isBuyable = npcHelper.findNpcThatSellsExcludeGold(wish.itemCode)
                                             .isPresent();
                if (isBuyable) {
                    continue;
                }
                boolean isFarmable = itemHelper.findLocationWhereToFarm(character, wish.itemCode)
                                               .isPresent();
                if (!isFarmable) {
                    continue;
                }
                String requiredSkillName = itemDefinition.get()
                                                         .getSubtype();
                int requiredSkillLevel = itemDefinition.get()
                                                       .getLevel();
                boolean charHasSkill = charHelper.charHasRequiredSkillLevel(character, requiredSkillName, requiredSkillLevel);

                if (charHasSkill) {
                    wish.reservedBy = character.getName();
                    wish.wishType = "gathering";
                    return Optional.of(wish);
                }
            }
        }
        return Optional.empty();
    }

    public synchronized Optional<Wish> reserveWishThatCanBeCraftedByMeWithBankSupport(CharacterSchema character) {
        Optional<Wish> existingReservedWish = getAlreadyReservedWish(character, "crafting");
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

                List<SimpleItemSchema> neededItems = itemHelper.getRecursiveResourcesToCraft(wish.itemCode, wish.amount);
                boolean canHandle = neededItems.stream()
                                               .allMatch(neededItem -> {
                                                   boolean canCraft = charHelper.couldCraft(character, neededItem.getCode());
                                                   boolean canHarvest = charHelper.canCanGatherResources(character, neededItem.getCode());
                                                   boolean itemAlreadyInBank = charHelper.cntItemsInBank(neededItem.getCode()) >= neededItem.getQuantity();
                                                   return canCraft || canHarvest || itemAlreadyInBank;
                                               });
                if (canHandle) {
                    wish.reservedBy = character.getName();
                    wish.wishType = "crafting";
                    return Optional.of(wish);
                }
            }
        }
        return Optional.empty();
    }

    public synchronized Optional<Wish> reserveWishThatCanBeCraftedByMe(CharacterSchema character) {
        Optional<Wish> existingReservedWish = getAlreadyReservedWish(character, "crafting");
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
                boolean charHasSkill = charHelper.charHasRequiredSkillLevel(character, requiredSkillName, requiredSkillLevel);

                boolean isResourcesAtBank = areAllItemsInBank(itemDefinition.get()
                                                                            .getCraft()
                                                                            .getItems());
                if (charHasSkill && isResourcesAtBank) {
                    wish.reservedBy = character.getName();
                    wish.wishType = "crafting";
                    return Optional.of(wish);
                }
            }
        }
        return Optional.empty();
    }

    private @NonNull Optional<Wish> getAlreadyReservedWish(CharacterSchema character, String wishType) {
        Optional<Wish> existingReservedWish = allWishes.stream()
                                                       .filter(wish -> !wish.fulfilled)
                                                       .filter(wish -> wish.reservedBy == null || wish.wishType.equals(wishType))
                                                       .filter(wish -> character.getName()
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
