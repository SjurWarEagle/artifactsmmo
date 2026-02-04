package de.tkunkel.game.artifactsmmo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication(scanBasePackages = "de.tkunkel.game.artifactsmmo")
@ConfigurationPropertiesScan
@EnableCaching
@EnableRetry
@EnableScheduling
public class Starter {
    private final Logger logger = LoggerFactory.getLogger(Starter.class.getName());
    private final Environment environment;


    public Starter(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {

        ApplicationContext context = SpringApplication.run(Starter.class, args);
        context.getBean(Starter.class)
               .start(context);
    }

    private void start(ApplicationContext context) {
        MDC.put("userId", "123");  // Seq field

        logger.warn("Starting");
        logger.info("Active profiles: {}", Arrays.toString(this.environment.getActiveProfiles()));
        logger.info("Configured server.port: {}", this.environment.getProperty("server.port"));
        AdventureManager adventureManager = context.getBean(AdventureManager.class);

        /*
         */
        if (this.environment.getActiveProfiles().length > 0
                && this.environment.getActiveProfiles()[0].equalsIgnoreCase("guiOnly")) {
        } else {
            // adventureManager.addAndStartAdventurer("Fin", AdventurerClass.FISHER);
            adventureManager.addAndStartAdventurer("Sjur", AdventurerClass.FIGHTER);
            // adventureManager.addAndStartAdventurer("Melanie", AdventurerClass.MINER);
            adventureManager.addAndStartAdventurer("Wolfgang", AdventurerClass.WOODWORKER);
            adventureManager.addAndStartAdventurer("Albrecht", AdventurerClass.ALCHEMIST);
        }
    }
}
