package de.tkunkel.game.artifactsmmo;

import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.api.*;
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
    private AccountsApi accountsApi;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    public final List<MapSchema> cachedMap = new ArrayList<>();
    public final List<MonsterSchema> cachedMonsters = new ArrayList<>();
    public final List<ItemSchema> cachedItems = new ArrayList<>();
    public final List<ResourceSchema> cachedResources = new ArrayList<>();

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
        accountsApi = new AccountsApi(createApiClient());
        fillCache();
    }

    private ApiClient createApiClient() {
        ApiClient rc = new ApiClient();
        rc.setBearerToken(config.API_TOKEN);
        rc.setBasePath("https://api.artifactsmmo.com");
        return rc;
    }

    public void fillCache() {
        executorService.submit(this::cacheMap);
        executorService.submit(this::cacheItems);
        executorService.submit(this::cacheMonsters);
        executorService.submit(this::cacheResources);
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    public Optional<ItemSchema> findBestToolForSkillThatCanBeCraftedByAccount(String skill, Integer level) {

        return cachedItems.stream()
                          .filter(itemSchema -> itemSchema.getEffects() != null)
                          .filter(itemSchema -> itemSchema.getEffects()
                                                          .stream()
                                                          .anyMatch(effectSchema -> effectSchema.getCode()
                                                                                                .equals(skill)))
                          .filter(itemSchema -> {
                              if (itemSchema.getCraft() == null) {
                                  return true;
                              } else {
                                  if (itemSchema.getCraft()
                                                .getSkill() == null) {
                                      // can be crated without any skill, so everyone can do it
                                      return true;
                                  }
                                  String requiredSkill = itemSchema.getCraft()
                                                                   .getSkill()
                                                                   .getValue()
                                          ;
                                  Integer requiredSkillLevel = itemSchema.getCraft()
                                                                         .getLevel();
                                  return aCharCanCraftThis(requiredSkill, requiredSkillLevel);
                              }
                          })
                          .filter(itemSchema -> itemSchema.getLevel() <= level)
                          .max((o1, o2) -> o1.getLevel() - o2.getLevel())
                ;
    }

    private boolean aCharCanCraftThis(String requiredSkill, Integer requiredSkillLevel) {

        try {
            CharactersListSchema characters = accountsApi.getAccountCharactersAccountsAccountCharactersGet(Config.ACCOUNT_NAME);
            for (CharacterSchema characterDatum : characters.getData()) {
                if (CharHelper.charHasRequiredSkillLevel(characterDatum, requiredSkill, requiredSkillLevel)) {
                    return true;
                }
            }
        } catch (ApiException e) {
            logger.warn("Failed to get characters", e);
            throw new RuntimeException(e);
        }
        return false;
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
