package de.tkunkel.game.artifactsmmo;

import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.brains.tier01.*;
import de.tkunkel.game.artifactsmmo.helper.TaskHelper;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import de.tkunkel.game.artifactsmmo.shopping.WishListWorker;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositGoldIfRichTask;
import de.tkunkel.game.artifactsmmo.tasks.GetBestItemForSlotTask;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.ItemSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AdventureManager {
    private final Logger logger = LoggerFactory.getLogger(AdventureManager.class.getName());

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ApiHolder apiHolder;
    private final BankDepositAllTask bankDepositAllTask;
    private final GetBestItemForSlotTask getBestItemForSlotTask;
    private final MinerT1Brain minerBrain;
    private final FighterT1Brain fighterBrain;
    private final FisherT1Brain fisherBrain;
    private final WoodworkerT1Brain woodworkerBrain;
    private final AlchemistT1Brain alchemistBrain;
    private final WishList wishList;
    private final WishListWorker wishListWorker;
    private final TaskHelper taskHelper;
    private final BankDepositGoldIfRichTask bankDepositGoldIfRichTask;
    private final CharactersApiWrapper charactersApi;

    public AdventureManager(ApiHolder apiHolder, BankDepositAllTask bankDepositAllTask,
                            GetBestItemForSlotTask getBestItemForSlotTask, MinerT1Brain minerBrain, FighterT1Brain fighterBrain,
                            FisherT1Brain fisherBrain, WoodworkerT1Brain woodworkerBrain, AlchemistT1Brain alchemistBrain,
                            WishList wishList, WishListWorker wishListWorker, TaskHelper taskHelper, BankDepositGoldIfRichTask bankDepositGoldIfRichTask, CharactersApiWrapper charactersApi) {
        this.apiHolder = apiHolder;
        this.bankDepositAllTask = bankDepositAllTask;
        this.getBestItemForSlotTask = getBestItemForSlotTask;
        this.minerBrain = minerBrain;
        this.fighterBrain = fighterBrain;
        this.fisherBrain = fisherBrain;
        this.woodworkerBrain = woodworkerBrain;
        this.alchemistBrain = alchemistBrain;
        this.wishList = wishList;
        this.wishListWorker = wishListWorker;
        this.taskHelper = taskHelper;
        this.bankDepositGoldIfRichTask = bankDepositGoldIfRichTask;
        this.charactersApi = charactersApi;
    }

    public void addAndStartAdventurer(String name, AdventurerClass adventurerClass) {
        executorService.submit(() -> {
            try {
                Thread current = Thread.currentThread();
                current.setName(name + "-" + adventurerClass.name());
                startLoop(name, adventurerClass);
            } catch (Exception e) {
                logger.error("Error starting adventurer", e);
            }
        });
    }

    public void startLoop(String characterName, AdventurerClass adventurerClass) {
        CharacterSchema character = apiHolder.charactersApi.getCharacterCharactersNameGet(characterName)
                                                           .getData();
        bankDepositAllTask.depositInventoryInBank(characterName);
        boolean stopLoop = false;
        while (true) {
            // noinspection ConstantValue
            if (stopLoop) {
                return;
            }
            getBestItemForSlotTask.equipOrRequestItemArmorForSlot(characterName, ItemSlot.BODY_ARMOR);
            getBestItemForSlotTask.equipOrRequestItemArmorForSlot(characterName, ItemSlot.HELMET);
            getBestItemForSlotTask.equipOrRequestItemArmorForSlot(characterName, ItemSlot.SHIELD);
            getBestItemForSlotTask.equipOrRequestItemArmorForSlot(characterName, ItemSlot.BOOTS);
            getBestItemForSlotTask.equipOrRequestItemArmorForSlot(characterName, ItemSlot.RING1);
            getBestItemForSlotTask.equipOrRequestItemArmorForSlot(characterName, ItemSlot.RING2);
            getBestItemForSlotTask.equipOrRequestItemArmorForSlot(characterName, ItemSlot.AMULET);
            getBestItemForSlotTask.equipOrRequestItemArmorForSlot(characterName, ItemSlot.LEG_ARMOR);

            if (wishListWorker.isHandlingHuntingWish(character)) {
                continue;
            }
            if (wishListWorker.isHandlingCraftingWish(character)) {
                continue;
            }
            if (wishListWorker.isHandlingCraftingWithBankWish(character)) {
                continue;
            }
            if (wishListWorker.isHandlingGatheringWish(character)) {
                continue;
            }
            if (wishListWorker.isHandlingBuyingWish(character)) {
                continue;
            }
            if (taskHelper.isHandlingTask(character)) {
                continue;
            }


            bankDepositGoldIfRichTask.depositGoldInBank(character);
            // nothing to craft, so use default
            switch (adventurerClass) {
                case MINER -> minerBrain.runBaseLoop(characterName);
                case FIGHTER -> fighterBrain.runBaseLoop(characterName);
                case WOODWORKER -> woodworkerBrain.runBaseLoop(characterName);
                case ALCHEMIST -> alchemistBrain.runBaseLoop(characterName);
                case FISHER -> fisherBrain.runBaseLoop(characterName);
            }
        }
    }
}
