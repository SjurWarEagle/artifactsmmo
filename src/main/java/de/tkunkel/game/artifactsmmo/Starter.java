package de.tkunkel.game.artifactsmmo;

import de.tkunkel.game.artifactsmmo.brains.Brain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication(scanBasePackages = "de.tkunkel.game.artifactsmmo")
public class Starter {
    private Logger logger = LoggerFactory.getLogger(Starter.class.getName());
    private Brain brain;
    private List<Brain> allBrains = new ArrayList<>();

    static void main() {
        ApplicationContext context = SpringApplication.run(Starter.class);
        context.getBean(Starter.class)
               .start(context);
    }

    private void collectBrains() {
    }

    private void start(ApplicationContext context) {
        context.getBean(AdventureManager.class)
               .addAndStartAdventurer("Sjur", AdventurerClass.FIGHTER);
        context.getBean(AdventureManager.class)
               .addAndStartAdventurer("Melanie", AdventurerClass.MINER);
        context.getBean(AdventureManager.class)
               .addAndStartAdventurer("Wolfgang", AdventurerClass.WOODWORKER);
    }
}
