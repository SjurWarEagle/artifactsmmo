package de.tkunkel.game.artifactsmmo.web;

import com.google.gson.Gson;
import de.tkunkel.game.artifactsmmo.shopping.WishList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashBoardController {
    private final Logger logger = LoggerFactory.getLogger(DashBoardController.class.getName());
    private final WishList wishList;

    public DashBoardController(WishList wishList) {
        this.wishList = wishList;
    }

    @GetMapping("/")
    public String getDashboard() {
        return new Gson().toJson(wishList.getAllWishes());
    }
}
