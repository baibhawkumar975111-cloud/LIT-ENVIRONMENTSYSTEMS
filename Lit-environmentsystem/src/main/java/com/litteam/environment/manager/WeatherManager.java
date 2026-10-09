package com.litteam.environment.manager;

import com.litteam.environment.config.ConfigManager;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Optional;
import java.util.Random;

public final class WeatherManager {

    public enum Weather { CLEAR, RAIN, THUNDER }

    private final ConfigManager config;
    private final WorldManager worlds;
    private final Random random = new Random();

    private boolean enabled;
    private boolean automatic;
    private long cycleSeconds;
    private long secondsSinceChange;

    public WeatherManager(ConfigManager config, WorldManager worlds) {
        this.config = config;
        this.worlds = worlds;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = config.config();
        enabled = cfg.getBoolean("system.enabled", true) && cfg.getBoolean("weather.enabled", true);
        automatic = cfg.getBoolean("weather.automatic", true);
        cycleSeconds = Math.max(10, cfg.getLong("weather.automatic-cycle-seconds", 900));
        secondsSinceChange = 0;
        refreshRules();
    }

    /** Applies the configured default weather once, when the plugin starts. */
    public void applyDefault() {
        if (!enabled) {
            return;
        }
        Weather weather = parse(config.config().getString("weather.default", "clear")).orElse(Weather.CLEAR);
        for (World world : worlds.weatherWorlds()) {
            apply(world, weather);
        }
    }

    /** Runs once per second. */
    public void tick() {
        if (!enabled || !automatic) {
            return;
        }
        if (++secondsSinceChange < cycleSeconds) {
            return;
        }
        secondsSinceChange = 0;
        for (World world : worlds.weatherWorlds()) {
            apply(world, pickNext());
        }
    }

    /** The vanilla weather cycle would fight with us, so it stays off while the plugin is active. */
    public void refreshRules() {
        worlds.holdVanillaWeather(enabled);
    }

    public void setAll(Weather weather) {
        for (World world : worlds.weatherWorlds()) {
            apply(world, weather);
        }
        config.set("weather.default", weather.name().toLowerCase());
        config.save();
    }

    public void apply(World world, Weather weather) {
        if (world == null || !worlds.isWeatherEnabled(world)) {
            return;
        }

        // The vanilla timers are not ticking while we hold the cycle, but if the plugin is
        // ever removed they decide how long this weather lasts, so keep them sensible.
        int ticks = (int) Math.min(Integer.MAX_VALUE, cycleSeconds * 20L);

        switch (weather) {
            case CLEAR -> {
                world.setStorm(false);
                world.setThundering(false);
                world.setClearWeatherDuration(ticks);
            }
            case RAIN -> {
                world.setStorm(true);
                world.setThundering(false);
                world.setWeatherDuration(ticks);
            }
            case THUNDER -> {
                world.setStorm(true);
                world.setThundering(true);
                world.setWeatherDuration(ticks);
                world.setThunderDuration(ticks);
            }
        }
    }

    public Weather current(World world) {
        if (world.isThundering()) {
            return Weather.THUNDER;
        }
        if (world.hasStorm()) {
            return Weather.RAIN;
        }
        return Weather.CLEAR;
    }

    private Weather pickNext() {
        FileConfiguration cfg = config.config();
        int clear = Math.max(0, cfg.getInt("weather.chances.clear", 60));
        int rain = Math.max(0, cfg.getInt("weather.chances.rain", 30));
        int thunder = Math.max(0, cfg.getInt("weather.chances.thunder", 10));

        int total = clear + rain + thunder;
        if (total == 0) {
            return Weather.CLEAR;
        }
        int roll = random.nextInt(total);
        if (roll < clear) {
            return Weather.CLEAR;
        }
        if (roll < clear + rain) {
            return Weather.RAIN;
        }
        return Weather.THUNDER;
    }

    public static Optional<Weather> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        return switch (input.toLowerCase()) {
            case "clear", "sun" -> Optional.of(Weather.CLEAR);
            case "rain" -> Optional.of(Weather.RAIN);
            case "thunder", "storm" -> Optional.of(Weather.THUNDER);
            default -> Optional.empty();
        };
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
        this.secondsSinceChange = 0;
        config.set("weather.automatic", automatic);
        config.save();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long cycleSeconds() {
        return cycleSeconds;
    }

    public void setCycleSeconds(long seconds) {
        this.cycleSeconds = Math.max(10, seconds);
        this.secondsSinceChange = 0;
        config.set("weather.automatic-cycle-seconds", cycleSeconds);
        config.save();
    }
}
