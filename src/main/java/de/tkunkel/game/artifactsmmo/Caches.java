package de.tkunkel.game.artifactsmmo;

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

@Component
public class Caches {
    private Logger logger = LoggerFactory.getLogger(Caches.class.getName());

    protected final ApiClient apiClient = new ApiClient();
    protected MapsApi mapsApi;
    protected ItemsApi itemsApi;
    protected MonstersApi monstersApi;
    protected ResourcesApi resourcesApi;

    public final List<MapSchema> cachedMap = new ArrayList<>();
    public final List<MonsterSchema> cachedMonsters = new ArrayList<MonsterSchema>();
    public final List<ItemSchema> cachedItems = new ArrayList<ItemSchema>();
    public final List<ResourceSchema> cachedResources = new ArrayList<ResourceSchema>();

    @PostConstruct
    public void init() {
        apiClient.setBearerToken(Config.API_TOKEN);
        apiClient.setBasePath("https://api.artifactsmmo.com");
        mapsApi = new MapsApi(apiClient);
        itemsApi = new ItemsApi(apiClient);
        monstersApi = new MonstersApi(apiClient);
        resourcesApi = new ResourcesApi(apiClient);
        fillCache();
    }

    public void fillCache() {
        cacheMap();
        cacheItems();
        cacheMonsters();
        cacheResources();
    }

    private void cacheResources() {
        logger.info("Starting cache of resources");
        try {
            DataPageResourceSchema allResourcesResourcesGet = resourcesApi.getAllResourcesResourcesGet(null, null, null, null, 1, 100);
            int cntPages = allResourcesResourcesGet.getPages();
            logger.info("Caching resources page count {}", cntPages);
            for (Integer pageNr = 1; pageNr < cntPages + 1; pageNr++) {
                logger.info("Caching resources page {}", pageNr);
                allResourcesResourcesGet = resourcesApi.getAllResourcesResourcesGet(null, null, null, null, pageNr, 100);
                cachedResources.addAll(allResourcesResourcesGet.getData());
            }
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

    }

    private void cacheMonsters() {
        logger.info("Starting cache of monsters");
        try {
            DataPageMonsterSchema allMonstersMonstersGet = monstersApi.getAllMonstersMonstersGet(null, null, null, null, 1, 100);
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

    public Optional<ItemSchema> findItemDefinition(String code) {
        return cachedItems.stream()
                          .filter(itemSchema -> itemSchema.getCode()
                                                          .equals(code))
                          .findFirst();
    }
}
