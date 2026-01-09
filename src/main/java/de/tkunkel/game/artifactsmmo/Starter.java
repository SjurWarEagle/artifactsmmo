package de.tkunkel.game.artifactsmmo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "de.tkunkel.game.artifactsmmo")
@ConfigurationPropertiesScan
@EnableCaching
@EnableRetry
@EnableScheduling
public class Starter {
    private final Logger logger = LoggerFactory.getLogger(Starter.class.getName());

    static void main() {
        ApplicationContext context = SpringApplication.run(Starter.class);
        context.getBean(Starter.class)
               .start(context);
    }

    private void start(ApplicationContext context) {
        logger.info("Starting");
        AdventureManager adventureManager = context.getBean(AdventureManager.class);

        /*
         */
        adventureManager.addAndStartAdventurer("Fin", AdventurerClass.FISHER);
        adventureManager.addAndStartAdventurer("Sjur", AdventurerClass.FIGHTER);
        adventureManager.addAndStartAdventurer("Melanie", AdventurerClass.MINER);
        adventureManager.addAndStartAdventurer("Albrecht", AdventurerClass.ALCHEMIST);
        adventureManager.addAndStartAdventurer("Wolfgang", AdventurerClass.WOODWORKER);
    }
}
