package de.tkunkel.game.artifactsmmo;

import de.tkunkel.game.artifactsmmo.brains.CommonBrain;
import de.tkunkel.game.artifactsmmo.tasks.BankDepositAllTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AdventureManager {
    private final Logger logger = LoggerFactory.getLogger(Starter.class.getName());

    private final List<Adventurer> adventurers = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Set<CommonBrain> brains;
    private final ApiHolder apiHolder;
    private final BankDepositAllTask bankDepositAllTask;

    public AdventureManager(Set<CommonBrain> brains, ApiHolder apiHolder, BankDepositAllTask bankDepositAllTask) {
        this.brains = brains;
        this.apiHolder = apiHolder;
        this.bankDepositAllTask = bankDepositAllTask;
    }

    public void addAndStartAdventurer(String name, AdventurerClass adventurerClass) {
        executorService.submit(() -> {
            try {
                Thread current = Thread.currentThread();
                current.setName(name + "-" + adventurerClass.name());
                Adventurer adventurer = new Adventurer(name, adventurerClass, apiHolder, bankDepositAllTask, brains);
                adventurers.add(adventurer);
                adventurer.startLoop();
            } catch (Exception e) {
                logger.error("Error starting adventurer", e);
                throw new RuntimeException(e);
            }
        });
    }
}
