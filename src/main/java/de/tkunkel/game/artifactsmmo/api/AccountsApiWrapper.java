package de.tkunkel.game.artifactsmmo.api;

import de.tkunkel.game.artifactsmmo.Config;
import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.api.AccountsApi;
import de.tkunkel.games.artifactsmmo.model.CharactersListSchema;
import de.tkunkel.games.artifactsmmo.model.DataPageAccountAchievementSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.net.SocketException;

@Service
public class AccountsApiWrapper {
    private final Logger logger = LoggerFactory.getLogger(AccountsApiWrapper.class.getName());
    private final AccountsApi accountsApi;
    private final Config config;

    public AccountsApiWrapper(ApiClient apiClient, Config config) {
        this.accountsApi = new AccountsApi(apiClient);
        this.config = config;
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 10, backoff = @Backoff(delay = 1000))
    @Cacheable(cacheNames = "getAccountCharactersAccountsAccountCharactersGet")
    public CharactersListSchema getAccountCharactersAccountsAccountCharactersGet() {
        try {
            return accountsApi.getAccountCharactersAccountsAccountCharactersGet(config.accountName());
        } catch (ApiException e) {
            logger.error("getAccountCharactersAccountsAccountCharactersGet", e);
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 10, backoff = @Backoff(delay = 1000))
    @Cacheable(cacheNames = "getAccountAchievementsAccountsAccountAchievementsGet")
    public DataPageAccountAchievementSchema getAccountAchievementsAccountsAccountAchievementsGet(boolean completed) {
        try {
            return accountsApi.getAccountAchievementsAccountsAccountAchievementsGet(config.accountName(), null, completed, null, null);
        } catch (ApiException e) {
            logger.error("getAccountCharactersAccountsAccountCharactersGet", e);
            throw new RuntimeException(e);
        }
    }
}
