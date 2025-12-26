package de.tkunkel.game.artifactsmmo.web;

import de.tkunkel.game.artifactsmmo.shopping.WishList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WishlistController {
    private final Logger logger = LoggerFactory.getLogger(WishlistController.class.getName());
    private final WishList wishList;

    public WishlistController(WishList wishList) {
        this.wishList = wishList;
    }

    @GetMapping("/wishlist")
    public String showWishlist(Model model) {
        model.addAttribute("wishlist", wishList.getAllWishes());
        return "wishlist";
    }
}
