package de.tkunkel.game.artifactsmmo;

import de.tkunkel.game.artifactsmmo.api.AccountsApiWrapper;
import de.tkunkel.game.artifactsmmo.combat.CombatSimulator;
import de.tkunkel.game.artifactsmmo.combat.CombatStatsEditor;
import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.api.ItemsApi;
import de.tkunkel.games.artifactsmmo.api.MapsApi;
import de.tkunkel.games.artifactsmmo.api.MonstersApi;
import de.tkunkel.games.artifactsmmo.api.ResourcesApi;
import de.tkunkel.games.artifactsmmo.model.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class Caches {
    private final Logger logger = LoggerFactory.getLogger(Caches.class.getName());

    private final Config config;
    private MapsApi mapsApi;
    private ItemsApi itemsApi;
    private MonstersApi monstersApi;
    private ResourcesApi resourcesApi;
    private final AccountsApiWrapper accountsApi;
    private final CombatStatsEditor combatStatsEditor;
    private final CombatSimulator combatSimulator;

    public final List<MapSchema> cachedMap = new ArrayList<>();
    public final List<MonsterSchema> cachedMonsters = new ArrayList<>();
    public final List<ItemSchema> cachedItems = new ArrayList<>();
    public final List<ResourceSchema> cachedResources = new ArrayList<>();

    public Caches(Config config, AccountsApiWrapper accountsApi, CombatStatsEditor combatStatsEditor, CombatSimulator combatSimulator) {
        this.config = config;
        this.accountsApi = accountsApi;
        this.combatStatsEditor = combatStatsEditor;
        this.combatSimulator = combatSimulator;
    }

    @PostConstruct
    public void init() {
        // using its own ApiClient instance per api client, seems to work better with multithreading
        mapsApi = new MapsApi(createApiClient());
        itemsApi = new ItemsApi(createApiClient());
        monstersApi = new MonstersApi(createApiClient());
        resourcesApi = new ResourcesApi(createApiClient());
        fillCache();
    }

    private ApiClient createApiClient() {
        ApiClient rc = new ApiClient();
        rc.setBearerToken(config.token());
        rc.setBasePath("https://api.artifactsmmo.com");
        return rc;
    }

    public void fillCache() {
        try (ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
                                                                                   .availableProcessors() * 2)) {
            executorService.submit(this::cacheMap);
            executorService.submit(this::cacheItems);
            executorService.submit(this::cacheMonsters);
            executorService.submit(this::cacheResources);

            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private void cacheResources() {
        logger.info("Starting cache of resources");
        try {
            DataPageResourceSchema all = resourcesApi.getAllResourcesResourcesGet(null, null, null, null, 1, 100);
            if (all == null || all.getPages() == null) {
                throw new RuntimeException("No resources found");
            }
            int cntPages = all.getPages();
            logger.info("Caching resources page count {}", cntPages);
            for (Integer pageNr = 1; pageNr < cntPages + 1; pageNr++) {
                logger.info("Caching resources page {}", pageNr);
                all = resourcesApi.getAllResourcesResourcesGet(null, null, null, null, pageNr, 100);
                cachedResources.addAll(all.getData());
            }
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void cacheMonsters() {
        logger.info("Starting cache of monsters");
        try {
            DataPageMonsterSchema allMonstersMonstersGet = monstersApi.getAllMonstersMonstersGet(null, null, null, null, 1, 100);
            if (allMonstersMonstersGet.getPages() == null) {
                return;
            }
            int cntPages = allMonstersMonstersGet.getPages();
            logger.info("Caching monsters page count {}", cntPages);
            for (Integer pageNr = 1; pageNr < cntPages + 1; pageNr++) {
                logger.info("Caching monsters page {}", pageNr);
                allMonstersMonstersGet = monstersApi.getAllMonstersMonstersGet(null, null, null, null, pageNr, 100);
                cachedMonsters.addAll(allMonstersMonstersGet.getData());
            }
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void cacheItems() {
        logger.info("Starting cache of items");
        try {
            DataPageItemSchema allItemsItems = itemsApi.getAllItemsItemsGet(null, null, null, null, null, null, 1, 100);
            if (allItemsItems.getPages() == null) {
                return;
            }
            int cntPages = allItemsItems.getPages();
            logger.info("Caching item page count {}", cntPages);
            for (Integer pageNr = 1; pageNr < cntPages + 1; pageNr++) {
                logger.info("Caching item page {}", pageNr);
                allItemsItems = itemsApi.getAllItemsItemsGet(null, null, null, null, null, null, pageNr, 100);
                cachedItems.addAll(allItemsItems.getData());
            }
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void cacheMap() {
        logger.info("Starting cache of map");
        try {
            DataPageMapSchema allMapsMapsGet = mapsApi.getAllMapsMapsGet(null, null, null, true, 4, 100);
            if (allMapsMapsGet.getPages() == null) {
                return;
            }
            int cntPages = allMapsMapsGet.getPages();
            logger.info("Caching map page count {}", cntPages);
            for (Integer pageNr = 1; pageNr < cntPages + 1; pageNr++) {
                logger.info("Caching map page {}", pageNr);
                allMapsMapsGet = mapsApi.getAllMapsMapsGet(null, null, null, true, pageNr, 100);
                cachedMap.addAll(allMapsMapsGet.getData());
            }
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public String findHighestFarmableResourceForSkillLevel(Integer skillLevel, GatheringSkill skill) {
        Optional<ResourceSchema> resource = cachedResources.stream()
                                                           .filter(resourceSchema -> resourceSchema.getLevel() <= skillLevel)
                                                           .filter(resourceSchema -> resourceSchema.getSkill()
                                                                                                   .equals(skill))
                                                           .sorted((o1, o2) -> o2.getLevel() - o1.getLevel())
                                                           .findFirst()
                ;
        if (resource.isEmpty()) {
            throw new RuntimeException("No resource found for skill " + skill + " and level " + skillLevel);
        }
        return resource.get()
                       .getCode();
    }
}
