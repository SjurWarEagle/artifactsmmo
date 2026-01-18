package de.tkunkel.game.artifactsmmo.web;

import de.tkunkel.game.artifactsmmo.api.AccountsApiWrapper;
import de.tkunkel.games.artifactsmmo.model.CharacterSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class CharsController {
    private final Logger logger = LoggerFactory.getLogger(CharsController.class.getName());
    private final AccountsApiWrapper accountsApiWrapper;

    public CharsController(AccountsApiWrapper accountsApiWrapper) {
        this.accountsApiWrapper = accountsApiWrapper;
    }

    @GetMapping("/charSheets")
    public String showCharacterSheets(Model model) {
        List<CharacterSchema> characters = accountsApiWrapper.getAccountCharactersAccountsAccountCharactersGet()
                                                             .getData();
        model.addAttribute("characters", characters);
        return "charSheets";
    }
}
