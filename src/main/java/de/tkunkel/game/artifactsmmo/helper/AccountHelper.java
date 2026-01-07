package de.tkunkel.game.artifactsmmo.helper;

import de.tkunkel.game.artifactsmmo.api.AccountsApiWrapper;
import de.tkunkel.games.artifactsmmo.model.ConditionSchema;
import de.tkunkel.games.artifactsmmo.model.DataPageAccountAchievementSchema;
import org.springframework.stereotype.Service;

@Service
public class AccountHelper {
    private final AccountsApiWrapper accountsApi;

    public AccountHelper(AccountsApiWrapper accountsApi) {
        this.accountsApi = accountsApi;
    }

    public boolean isFullfilled(ConditionSchema conditionSchema) {
        return switch (conditionSchema.getOperator()) {
            case ACHIEVEMENT_UNLOCKED -> hasAchievement(conditionSchema.getCode());
            default -> throw new IllegalArgumentException("Unsupported operator: " + conditionSchema.getOperator());
        };
    }

    public boolean hasAchievement(String achievementCode) {
        DataPageAccountAchievementSchema achievements = accountsApi.getAccountAchievementsAccountsAccountAchievementsGet(true);
        return achievements.getData()
                           .stream()
                           .anyMatch(accountAchievementSchema -> accountAchievementSchema.getCode()
                                                                                         .equalsIgnoreCase(achievementCode));
    }
}
