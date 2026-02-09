package de.tkunkel.game.artifactsmmo.web;

import de.tkunkel.game.artifactsmmo.api.AccountsApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.games.artifactsmmo.model.AccountAchievementSchema;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import de.tkunkel.games.artifactsmmo.model.SimpleItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
public class CharsController {
    private final Logger logger = LoggerFactory.getLogger(CharsController.class.getName());
    private final AccountsApiWrapper accountsApiWrapper;
    private final MyAccountApiWrapper myAccountApi;

    public CharsController(AccountsApiWrapper accountsApiWrapper, MyAccountApiWrapper myAccountApi) {
        this.accountsApiWrapper = accountsApiWrapper;
        this.myAccountApi = myAccountApi;
    }

    @GetMapping("/charSheets")
    public String showCharacterSheets(Model model) {
        List<AccountAchievementSchema> achievements = accountsApiWrapper.getAccountAchievementsAccountsAccountAchievementsGet(false)
                                                                        .getData();
        List<SimpleItemSchema> bankContent = myAccountApi.getBankItemsMyBankItemsGet(null, 1, 100)
                                                         .getData()
                                                         .stream()
                                                         .sorted(Comparator.comparing(SimpleItemSchema::getCode))
                                                         .toList()
                ;
        List<CharacterSchema> characters = accountsApiWrapper.getAccountCharactersAccountsAccountCharactersGet()
                                                             .getData();
        model.addAttribute("characters", characters);
        model.addAttribute("achievements", achievements);
        model.addAttribute("bankContent", bankContent);
        return "charSheets";
    }
}
