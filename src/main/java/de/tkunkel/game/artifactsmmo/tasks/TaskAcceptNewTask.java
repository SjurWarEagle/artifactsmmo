package de.tkunkel.game.artifactsmmo.tasks;

import de.tkunkel.game.artifactsmmo.CharHelper;
import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.games.artifactsmmo.model.CharacterResponseSchema;
import de.tkunkel.games.artifactsmmo.model.MapSchema;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskAcceptNewTask {

    private final CharHelper charHelper;

    public TaskAcceptNewTask(CharHelper charHelper) {
        this.charHelper = charHelper;
    }

    public void getNewTaskIfCurrentTaskIsDone(CommonBrain brain, CharacterResponseSchema character) {
        if (!"".equalsIgnoreCase(character.getData()
                                          .getTask())) {
            // still has task
            return;
        }
        Optional<MapSchema> closestLocation = brain.findClosestLocation(character, "monsters");
        if (closestLocation.isEmpty()) {
            return;
        }
        boolean moved = charHelper.moveToLocation(character, closestLocation.get());
        if (moved) {
            return;
        }
        brain.apiHolder.myCharactersApi.actionAcceptNewTaskMyNameActionTaskNewPost(character.getData()
                                                                                            .getName());
    }


}
