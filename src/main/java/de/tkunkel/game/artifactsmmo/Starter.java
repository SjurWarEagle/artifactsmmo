package de.tkunkel.game.artifactsmmo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication(scanBasePackages = "de.tkunkel.game.artifactsmmo")
public class Starter {
    private final Logger logger = LoggerFactory.getLogger(Starter.class.getName());

    static void main() {
        ApplicationContext context = SpringApplication.run(Starter.class);
        context.getBean(Starter.class)
               .start(context);
    }

    private void start(ApplicationContext context) {
        logger.info("Starting");
        /*
        context.getBean(AdventureManager.class)
               .addAndStartAdventurer("Sjur", AdventurerClass.FIGHTER);
        context.getBean(AdventureManager.class)
               .addAndStartAdventurer("Melanie", AdventurerClass.MINER);
        context.getBean(AdventureManager.class)
               .addAndStartAdventurer("Wolfgang", AdventurerClass.WOODWORKER);
         */
        context.getBean(AdventureManager.class)
               .addAndStartAdventurer("Albrecht", AdventurerClass.ALCHEMIST);
        context.getBean(AdventureManager.class)
               .addAndStartAdventurer("Fin", AdventurerClass.FISHER);
    }
}
