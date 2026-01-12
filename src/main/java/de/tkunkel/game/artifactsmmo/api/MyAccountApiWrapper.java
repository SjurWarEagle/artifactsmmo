package de.tkunkel.game.artifactsmmo.api;

import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.api.MyAccountApi;
import de.tkunkel.games.artifactsmmo.model.BankResponseSchema;
import de.tkunkel.games.artifactsmmo.model.DataPageSimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.net.SocketException;

@Service
public class MyAccountApiWrapper {
    private final Logger logger = LoggerFactory.getLogger(MyAccountApiWrapper.class.getName());
    private final MyAccountApi myAccountApi;

    public MyAccountApiWrapper(ApiClient apiClient) {
        this.myAccountApi = new MyAccountApi(apiClient);
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    // TODO paging! - call all pages
    public DataPageSimpleItemSchema getBankItemsMyBankItemsGet(String itemCode, int page, int size) {
        try {
            return myAccountApi.getBankItemsMyBankItemsGet(itemCode, page, size);
        } catch (ApiException e) {
            logger.error("getBankItemsMyBankItemsGet failed", e);
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public BankResponseSchema getBankDetailsMyBankGet() {
        try {
            return myAccountApi.getBankDetailsMyBankGet();
        } catch (ApiException e) {
            logger.error("getBankDetailsMyBankGet failed", e);
            throw new RuntimeException(e);
        }
    }
}
