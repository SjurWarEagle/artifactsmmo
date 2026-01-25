package de.tkunkel.game.artifactsmmo;

import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.api.*;
import de.tkunkel.games.artifactsmmo.model.*;
import jakarta.annotation.PostConstruct;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
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
    private NpcsApi npcsApi;

    public final List<MapSchema> cachedMap = new ArrayList<>();
    public final List<MonsterSchema> cachedMonsters = new ArrayList<>();
    public final List<ItemSchema> cachedItems = new ArrayList<>();
    public final List<ResourceSchema> cachedResources = new ArrayList<>();
    public final List<NPCSchema> cachedNpcs = new ArrayList<>();
    public final List<NPCItem> cachedNpcItems = new ArrayList<>();

    public Caches(Config config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        // using its own ApiClient instance per api client, seems to work better with multithreading
        mapsApi = new MapsApi(createApiClient());
        itemsApi = new ItemsApi(createApiClient());
        monstersApi = new MonstersApi(createApiClient());
        resourcesApi = new ResourcesApi(createApiClient());
        npcsApi = new NpcsApi(createApiClient());
        fillCache();
    }

    private ApiClient createApiClient() {
        ApiClient rc = new ApiClient();
        // rc.setDebugging(true);
        rc.setBearerToken(config.token());
        rc.setBasePath("https://api.artifactsmmo.com");
        configureTimeouts(rc);
        return rc;
    }

    private static void configureTimeouts(ApiClient apiClient) {
        OkHttpClient httpClient = null;
        Method setter = null;

        for (Method method : apiClient.getClass()
                                      .getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType()
                                                         .equals(OkHttpClient.class)) {
                try {
                    httpClient = (OkHttpClient) method.invoke(apiClient);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(OkHttpClient.class)) {
                setter = method;
            }
        }

        if (httpClient == null || setter == null) {
            throw new IllegalStateException("Unable to configure OkHttp timeouts for ApiClient");
        }

        OkHttpClient tunedClient = httpClient.newBuilder()
                                             .connectTimeout(Duration.ofSeconds(300))
                                             .readTimeout(Duration.ofSeconds(300))
                                             .writeTimeout(Duration.ofSeconds(3000))
                                             .callTimeout(Duration.ofSeconds(300))
                                             .build()
                ;

        try {
            setter.invoke(apiClient, tunedClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void fillCache() {
        try (ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
                                                                                   .availableProcessors() * 2)) {
            executorService.submit(this::cacheMap);
            executorService.submit(this::cacheItems);
            executorService.submit(this::cacheMonsters);
            executorService.submit(this::cacheResources);
            executorService.submit(this::cacheNpcs);
            executorService.submit(this::cacheNpcItems);

            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private void cacheNpcs() {
        logger.info("Starting cache of npcs");
        try {
            DataPageNPCSchema all = npcsApi.getAllNpcsNpcsDetailsGet(null, null, 1, 100);
            if (all == null || all.getPages() == null) {
                throw new RuntimeException("No npcs found");
            }
            int cntPages = all.getPages();
            logger.info("Caching npcs page count {}", cntPages);
            for (Integer pageNr = 1; pageNr < cntPages + 1; pageNr++) {
                logger.info("Caching npcs page {}", pageNr);
                all = npcsApi.getAllNpcsNpcsDetailsGet(null, null, 1, 100);
                cachedNpcs.addAll(all.getData());
            }
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void cacheNpcItems() {
        logger.info("Starting cache of cacheNpcItems");
        try {
            DataPageNPCItem all = npcsApi.getAllNpcsItemsNpcsItemsGet(null, null, null, 1, 100);
            if (all == null || all.getPages() == null) {
                throw new RuntimeException("No npcitems found");
            }
            int cntPages = all.getPages();
            logger.info("Caching npcitems page count {}", cntPages);
            for (Integer pageNr = 1; pageNr < cntPages + 1; pageNr++) {
                logger.info("Caching npcitems page {}", pageNr);
                all = npcsApi.getAllNpcsItemsNpcsItemsGet(null, null, null, 1, 100);
                cachedNpcItems.addAll(all.getData());
            }
        } catch (ApiException e) {
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
