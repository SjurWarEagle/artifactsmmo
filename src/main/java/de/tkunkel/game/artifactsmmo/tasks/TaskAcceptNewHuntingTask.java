package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.helper.MapHelper;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskAcceptNewHuntingTask {

    private final CharHelper charHelper;
    private final MapHelper mapHelper;

    public TaskAcceptNewHuntingTask(CharHelper charHelper, MapHelper mapHelper) {
        this.charHelper = charHelper;
        this.mapHelper = mapHelper;
    }

    public void getNewTaskIfCurrentTaskIsDone(CommonBrain brain, CharacterResponseSchema character) {
        if (!"".equalsIgnoreCase(character.getData()
                                          .getTask())) {
            // still has task
            return;
        }
        Optional<MapSchema> closestLocation = mapHelper.findClosestLocation(character.getData(), "monsters");
        if (closestLocation.isEmpty()) {
            return;
        }
        boolean moved = charHelper.moveToLocationSync(character.getData(), closestLocation.get());
        if (moved) {
            return;
        }
        brain.apiHolder.myCharactersApi.actionAcceptNewTaskMyNameActionTaskNewPost(character.getData()
                                                                                            .getName());
    }


}
