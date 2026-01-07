package de.tkunkel.game.artifactsmmo.api;

import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.api.MyCharactersApi;
import de.tkunkel.games.artifactsmmo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.net.SocketException;
import java.util.List;

@Service
public class MyCharactersApiWrapper {
    private final MyCharactersApi charactersApi;
    private final Logger logger = LoggerFactory.getLogger(MyCharactersApiWrapper.class.getName());

    public MyCharactersApiWrapper(ApiClient apiClient) {
        charactersApi = new MyCharactersApi(apiClient);
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public CharacterRestResponseSchema actionRestMyNameActionRestPost(String name) {
        try {
            return charactersApi.actionRestMyNameActionRestPost(name);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public UseItemResponseSchema actionUseItemMyNameActionUsePost(String name, SimpleItemSchema simpleItemSchema) {
        try {
            return charactersApi.actionUseItemMyNameActionUsePost(name, simpleItemSchema);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public EquipmentResponseSchema actionEquipItemMyNameActionEquipPost(String name, EquipSchema equipSchema) {
        try {
            return this.charactersApi.actionEquipItemMyNameActionEquipPost(name, equipSchema);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public SkillResponseSchema actionCraftingMyNameActionCraftingPost(String name, CraftingSchema craftingSchema) {
        try {
            var rc = charactersApi.actionCraftingMyNameActionCraftingPost(name, craftingSchema);
            logger.info("[XP] {} crafted {} and got {} xp", name, craftingSchema.getCode(), rc.getData()
                                                                                              .getDetails()
                                                                                              .getXp()
            );
            return rc;
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public CharacterMovementResponseSchema actionMoveMyNameActionMovePost(String name, DestinationSchema destinationSchema) {
        try {
            return charactersApi.actionMoveMyNameActionMovePost(name, destinationSchema);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public BankItemTransactionResponseSchema actionWithdrawBankItemMyNameActionBankWithdrawItemPost(String name, List<SimpleItemSchema> simpleItemSchemas) {
        try {
            return charactersApi.actionWithdrawBankItemMyNameActionBankWithdrawItemPost(name, simpleItemSchemas);
        } catch (ApiException e) {
            logger.warn("{} cannot withdraw {} from bank because it is not there", name, simpleItemSchemas);
            logger.warn("actionWithdrawBankItemMyNameActionBankWithdrawItemPost, ignoring with the assumption another char was faster", e);
        }
        return null;
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public CharacterFightResponseSchema actionFightMyNameActionFightPost(String name, FightRequestSchema fightRequest) {
        try {
            var rc = charactersApi.actionFightMyNameActionFightPost(name, fightRequest);
            logger.info("[XP] {} fought {} and got {} xp", name, rc.getData()
                                                                   .getFight()
                                                                   .getOpponent(), rc.getData()
                                                                                     .getFight()
                                                                                     .getCharacters()
                                                                                     .get(0)
                                                                                     .getXp()
            );
            return rc;
        } catch (ApiException e) {
            logger.warn("Problems with actionFightMyNameActionFightPost(" + fightRequest.toJson() + ")", e);
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public TaskResponseSchema actionAcceptNewTaskMyNameActionTaskNewPost(String name) {
        try {
            return charactersApi.actionAcceptNewTaskMyNameActionTaskNewPost(name);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public SkillResponseSchema actionGatheringMyNameActionGatheringPost(String name) {
        try {
            var rc = charactersApi.actionGatheringMyNameActionGatheringPost(name);
            logger.info("[XP] {} gathered and got {} xp", name, rc.getData()
                                                                  .getDetails()
                                                                  .getXp()
            );
            return rc;
        } catch (ApiException e) {
            logger.error("could not gather resources", e);
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public BankItemTransactionResponseSchema actionDepositBankItemMyNameActionBankDepositItemPost(String name, List<SimpleItemSchema> itemsToDeposit) {
        try {
            return charactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(name, itemsToDeposit);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public RewardDataResponseSchema actionCompleteTaskMyNameActionTaskCompletePost(String name) {
        try {
            return charactersApi.actionCompleteTaskMyNameActionTaskCompletePost(name);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public BankGoldTransactionResponseSchema actionWithdrawBankGoldMyNameActionBankWithdrawGoldPost(String name, DepositWithdrawGoldSchema depositWithdrawGoldSchema) {
        try {
            return charactersApi.actionWithdrawBankGoldMyNameActionBankWithdrawGoldPost(name, depositWithdrawGoldSchema);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public BankExtensionTransactionResponseSchema actionBuyBankExpansionMyNameActionBankBuyExpansionPost(String name) {
        try {
            return charactersApi.actionBuyBankExpansionMyNameActionBankBuyExpansionPost(name);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public BankGoldTransactionResponseSchema actionDepositBankGoldMyNameActionBankDepositGoldPost(String name, DepositWithdrawGoldSchema depositWithdrawGoldSchema) {
        try {
            return charactersApi.actionDepositBankGoldMyNameActionBankDepositGoldPost(name, depositWithdrawGoldSchema);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(retryFor = SocketException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public TaskCancelledResponseSchema actionTaskCancelMyNameActionTaskCancelPost(String name) {
        try {
            return charactersApi.actionTaskCancelMyNameActionTaskCancelPost(name);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
