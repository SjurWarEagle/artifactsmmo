package de.tkunkel.game.artifactsmmo.api;

import de.tkunkel.game.artifactsmmo.Config;
import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.api.AccountsApi;
import de.tkunkel.games.artifactsmmo.model.CharactersListSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AccountsApiWrapper {
    private final Logger logger = LoggerFactory.getLogger(AccountsApiWrapper.class.getName());
    private final AccountsApi accountsApi;
    private final Config config;

    public AccountsApiWrapper(ApiClient apiClient, Config config) {
        this.accountsApi = new AccountsApi(apiClient);
        this.config = config;
    }

    @Cacheable(cacheNames = "getAccountCharactersAccountsAccountCharactersGet")
    public CharactersListSchema getAccountCharactersAccountsAccountCharactersGet() {
        try {
            return accountsApi.getAccountCharactersAccountsAccountCharactersGet(config.accountName());
        } catch (ApiException e) {
            logger.error("getAccountCharactersAccountsAccountCharactersGet", e);
            throw new RuntimeException(e);
        }
    }
}
