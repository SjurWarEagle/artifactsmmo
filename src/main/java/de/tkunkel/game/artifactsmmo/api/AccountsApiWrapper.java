package de.tkunkel.game.artifactsmmo.api;

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
    private final ApiClient apiClient;
    private final AccountsApi accountsApi;

    public AccountsApiWrapper(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.accountsApi = new AccountsApi(apiClient);
    }

    @Cacheable(cacheNames = "getAccountCharactersAccountsAccountCharactersGet")
    public CharactersListSchema getAccountCharactersAccountsAccountCharactersGet(String account) {
        try {
            return accountsApi.getAccountCharactersAccountsAccountCharactersGet(account);
        } catch (ApiException e) {
            logger.error("getAccountCharactersAccountsAccountCharactersGet", e);
            throw new RuntimeException(e);
        }
    }
}
