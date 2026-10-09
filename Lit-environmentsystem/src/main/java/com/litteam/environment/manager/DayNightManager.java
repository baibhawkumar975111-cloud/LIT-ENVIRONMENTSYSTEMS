package com.litteam.environment.manager;

import com.litteam.environment.LitEnvironment;
import com.litteam.environment.config.ConfigManager;
import com.litteam.environment.util.SunCycle;
import com.litteam.environment.util.TimeUtil;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public final class DayNightManager {

    private final LitEnvironment plugin;
    private final ConfigManager config;
    private final WorldManager worlds;

    private boolean enabled;
    private boolean paused;
    private boolean accelerated;
    private double gameHoursPerMinute;
    private ZoneId zone;
    private SunCycle sunCycle;

    public DayNightManager(LitEnvironment plugin, ConfigManager config, WorldManager worlds) {
        this.plugin = plugin;
        this.config = config;
        this.worlds = worlds;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = config.config();
        enabled = cfg.getBoolean("system.enabled", true) && cfg.getBoolean("day-night.enabled", true);
        paused = cfg.getBoolean("day-night.pause", false);
        accelerated = "accelerated".equalsIgnoreCase(cfg.getString("day-night.mode", "realtime"));
        gameHoursPerMinute = Math.max(0.01, cfg.getDouble("day-night.accelerated.game-hours-per-real-minute", 1.0));
        zone = readZone(cfg.getString("day-night.timezone", "Asia/Kolkata"));
        sunCycle = readSunCycle(cfg.getString("day-night.sunrise", "06:00"), cfg.getString("day-night.sunset", "18:00"));
        refreshRules();
    }

    /** Runs every server tick. */
    public void tick() {
        if (!enabled || paused) {
            return;
        }

        long target = Math.round(currentTicks()) % TimeUtil.DAY_TICKS;
        for (World world : worlds.timeWorlds()) {
            if (worlds.isPaused(world)) {
                continue;
            }
            if (world.getTime() != target) {
                world.setTime(target);
            }
        }
    }

    /** Keeps the vanilla daylight cycle switched off while we are in control of the clock. */
    public void refreshRules() {
        worlds.holdVanillaTime(enabled);
    }

    /**
     * Sets the time in every managed world and pauses the sync, otherwise the next
     * tick would just put the clock back to the real time.
     */
    public void setTime(LocalTime clockTime) {
        long ticks = Math.round(sunCycle.ticksAt(clockTime)) % TimeUtil.DAY_TICKS;
        for (World world : worlds.timeWorlds()) {
            world.setTime(ticks);
        }
        setPaused(true);
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        config.set("day-night.pause", paused);
        config.save();
    }

    private double currentTicks() {
        if (accelerated) {
            double minutes = System.currentTimeMillis() / 60000.0;
            return (minutes * gameHoursPerMinute * 1000.0) % TimeUtil.DAY_TICKS;
        }
        return sunCycle.ticksAt(realTime());
    }

    private ZoneId readZone(String id) {
        try {
            return ZoneId.of(id);
        } catch (DateTimeException ex) {
            plugin.getLogger().warning("Unknown timezone '" + id + "', using the server timezone instead.");
            return ZoneId.systemDefault();
        }
    }

    private SunCycle readSunCycle(String sunrise, String sunset) {
        try {
            return new SunCycle(TimeUtil.parse(sunrise), TimeUtil.parse(sunset));
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid sunrise/sunset in config.yml, using 06:00 and 18:00.");
            return SunCycle.DEFAULT;
        }
    }

    public LocalTime realTime() {
        return ZonedDateTime.now(zone).toLocalTime();
    }

    /** What the given world's sky corresponds to on the real clock. */
    public LocalTime clockIn(World world) {
        return sunCycle.clockAt(world.getTime());
    }

    public LocalTime sunrise() {
        return sunCycle.sunrise();
    }

    public LocalTime sunset() {
        return sunCycle.sunset();
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String mode() {
        return accelerated ? "accelerated" : "realtime";
    }

    public String timezone() {
        return zone.getId();
    }
}
