package de.tkunkel.game.artifactsmmo.api;

import de.tkunkel.games.artifactsmmo.ApiClient;
import de.tkunkel.games.artifactsmmo.ApiException;
import de.tkunkel.games.artifactsmmo.api.MyCharactersApi;
import de.tkunkel.games.artifactsmmo.model.*;

import java.util.List;

public class MyCharactersApiWrapper {
    private final MyCharactersApi charactersApi;

    public MyCharactersApiWrapper(ApiClient apiClient) {
        charactersApi = new MyCharactersApi(apiClient);
    }

    public CharacterRestResponseSchema actionRestMyNameActionRestPost(String name) {
        try {
            return charactersApi.actionRestMyNameActionRestPost(name);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public UseItemResponseSchema actionUseItemMyNameActionUsePost(String name, SimpleItemSchema simpleItemSchema) {
        try {
            return charactersApi.actionUseItemMyNameActionUsePost(name, simpleItemSchema);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public EquipmentResponseSchema actionEquipItemMyNameActionEquipPost(String name, EquipSchema equipSchema) {
        try {
            return this.charactersApi.actionEquipItemMyNameActionEquipPost(name, equipSchema);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public SkillResponseSchema actionCraftingMyNameActionCraftingPost(String name, CraftingSchema craftingSchema) {
        try {
            return charactersApi.actionCraftingMyNameActionCraftingPost(name, craftingSchema);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public CharacterMovementResponseSchema actionMoveMyNameActionMovePost(String name, DestinationSchema destinationSchema) {
        try {
            return charactersApi.actionMoveMyNameActionMovePost(name, destinationSchema);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public BankItemTransactionResponseSchema actionWithdrawBankItemMyNameActionBankWithdrawItemPost(String name, List<SimpleItemSchema> simpleItemSchemas) {
        try {
            return charactersApi.actionWithdrawBankItemMyNameActionBankWithdrawItemPost(name, simpleItemSchemas);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public CharacterFightResponseSchema actionFightMyNameActionFightPost(String name, FightRequestSchema fightRequest) {
        try {
            return charactersApi.actionFightMyNameActionFightPost(name, fightRequest);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public TaskResponseSchema actionAcceptNewTaskMyNameActionTaskNewPost(String name) {
        try {
            return charactersApi.actionAcceptNewTaskMyNameActionTaskNewPost(name);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public SkillResponseSchema actionGatheringMyNameActionGatheringPost(String name) {
        try {
            return charactersApi.actionGatheringMyNameActionGatheringPost(name);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public BankItemTransactionResponseSchema actionDepositBankItemMyNameActionBankDepositItemPost(String name, List<SimpleItemSchema> itemsToDeposit) {
        try {
            return charactersApi.actionDepositBankItemMyNameActionBankDepositItemPost(name, itemsToDeposit);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public RewardDataResponseSchema actionCompleteTaskMyNameActionTaskCompletePost(String name) {
        try {
            return charactersApi.actionCompleteTaskMyNameActionTaskCompletePost(name);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public BankGoldTransactionResponseSchema actionWithdrawBankGoldMyNameActionBankWithdrawGoldPost(String name, DepositWithdrawGoldSchema depositWithdrawGoldSchema) {
        try {
            return charactersApi.actionWithdrawBankGoldMyNameActionBankWithdrawGoldPost(name, depositWithdrawGoldSchema);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public BankExtensionTransactionResponseSchema actionBuyBankExpansionMyNameActionBankBuyExpansionPost(String name) {
        try {
            return charactersApi.actionBuyBankExpansionMyNameActionBankBuyExpansionPost(name);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public BankGoldTransactionResponseSchema actionDepositBankGoldMyNameActionBankDepositGoldPost(String name, DepositWithdrawGoldSchema depositWithdrawGoldSchema) {
        try {
            return charactersApi.actionDepositBankGoldMyNameActionBankDepositGoldPost(name, depositWithdrawGoldSchema);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
