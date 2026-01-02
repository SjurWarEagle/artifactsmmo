package de.tkunkel.game.artifactsmmo;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "artifactsmmo.api")
@Validated
public record Config(@NotNull String accountName, @NotNull String token) {
}
