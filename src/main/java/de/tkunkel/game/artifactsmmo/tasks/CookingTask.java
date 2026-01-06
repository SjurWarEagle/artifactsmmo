package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.Caches;
import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.api.CharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyAccountApiWrapper;
import de.tkunkel.game.artifactsmmo.api.MyCharactersApiWrapper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.CraftingSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CookingTask {
    private final Logger logger = LoggerFactory.getLogger(CookingTask.class.getName());
    private final CharactersApiWrapper charactersApiWrapper;
    private final MyAccountApiWrapper myAccountApiWrapper;
    private final MyCharactersApiWrapper myCharactersApi;
    private final Caches caches;
    private final CharHelper charHelper;
    private final MapHelper mapHelper;

    public CookingTask(CharactersApiWrapper charactersApiWrapper, MyAccountApiWrapper myAccountApiWrapper, MyCharactersApiWrapper myCharactersApi, Caches caches, CharHelper charHelper, MapHelper mapHelper) {
        this.charactersApiWrapper = charactersApiWrapper;
        this.myAccountApiWrapper = myAccountApiWrapper;
        this.myCharactersApi = myCharactersApi;
        this.caches = caches;
        this.charHelper = charHelper;
        this.mapHelper = mapHelper;
    }

    public void cookFoodIfHaveSome(CommonBrain brain, CharacterResponseSchema givenCharacter) {
        final CharacterResponseSchema character = charactersApiWrapper.getCharacterCharactersNameGet(givenCharacter.getData()
                                                                                                                   .getName());

        final int MIN_MOUNT_NEEDED = 5;
        var cookableFood = caches.cachedItems.stream()
                                             .filter(itemSchema -> itemSchema.getType()
                                                                             .equalsIgnoreCase("consumable"))
                                             .filter(itemSchema -> itemSchema.getSubtype()
                                                                             .equalsIgnoreCase("food"))
                                             .filter(itemSchema -> itemSchema.getCraft() != null)
                                             .filter(itemSchema ->
                                                             itemSchema.getCraft()
                                                                       .getItems()
                                                                       .stream()
                                                                       .allMatch(resource -> character.getData()
                                                                                                      .getInventory()
                                                                                                      .stream()
                                                                                                      .anyMatch(inventory -> inventory.getCode()
                                                                                                                                      .equalsIgnoreCase(resource.getCode())
                                                                                                              && inventory.getQuantity() >= resource.getQuantity() * MIN_MOUNT_NEEDED
                                                                                                      )
                                                                       )
                                             )
                                             .toList()
                ;
        if (cookableFood.isEmpty()) {
            return;
        }
        Optional<MapSchema> cooking = mapHelper.findClosestLocation(character, "cooking");
        charHelper.moveToLocation(character, cooking.get());
        charHelper.waitUntilCooldownDone(character);

        CraftingSchema craftingSchema = new CraftingSchema().code(cookableFood.get(0)
                                                                              .getCode())
                                                            .quantity(MIN_MOUNT_NEEDED);
        myCharactersApi.actionCraftingMyNameActionCraftingPost(character.getData()
                                                                        .getName(), craftingSchema
        );
        charHelper.waitUntilCooldownDone(character);
    }

}
