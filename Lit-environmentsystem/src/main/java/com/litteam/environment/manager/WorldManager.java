package com.litteam.environment.manager;

import com.litteam.environment.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class WorldManager {

    private final ConfigManager config;

    // Worlds where we switched off the vanilla time/weather cycle, so we can switch it back on later
    private final Set<UUID> timeHeld = new HashSet<>();
    private final Set<UUID> weatherHeld = new HashSet<>();

    private boolean blacklist;
    private List<String> listed = new ArrayList<>();

    public WorldManager(ConfigManager config) {
        this.config = config;
        reload();
    }

    public void reload() {
        blacklist = "blacklist".equalsIgnoreCase(config.config().getString("worlds.mode", "blacklist"));
        listed = new ArrayList<>(config.config().getStringList("worlds.list"));
    }

    /** Only overworld-type worlds have a day cycle and weather, so the Nether and End are never managed. */
    public boolean isManaged(World world) {
        if (world == null || world.getEnvironment() != World.Environment.NORMAL) {
            return false;
        }
        boolean inList = listed.stream().anyMatch(name -> name.equalsIgnoreCase(world.getName()));
        return blacklist ? !inList : inList;
    }

    public boolean isWorldEnabled(World world) {
        return isManaged(world) && config.config().getBoolean(path(world, "enabled"), true);
    }

    public boolean isTimeEnabled(World world) {
        return isWorldEnabled(world) && config.config().getBoolean(path(world, "time-enabled"), true);
    }

    public boolean isWeatherEnabled(World world) {
        return isWorldEnabled(world) && config.config().getBoolean(path(world, "weather-enabled"), true);
    }

    public boolean isPaused(World world) {
        return config.config().getBoolean(path(world, "pause"), false)
                || config.config().getBoolean("day-night.pause", false);
    }

    public void setWorldEnabled(World world, boolean enabled) {
        config.set(path(world, "enabled"), enabled);
        config.save();
    }

    public List<World> timeWorlds() {
        List<World> result = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (isTimeEnabled(world)) {
                result.add(world);
            }
        }
        return result;
    }

    public List<World> weatherWorlds() {
        List<World> result = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (isWeatherEnabled(world)) {
                result.add(world);
            }
        }
        return result;
    }

    public List<String> configuredWorldNames() {
        return List.copyOf(listed);
    }

    /*
     * While we drive the clock ourselves the vanilla cycle has to be off, otherwise the
     * game advances time on its own and we keep snapping it back, which looks like stutter.
     */
    public void holdVanillaTime(boolean hold) {
        for (World world : Bukkit.getWorlds()) {
            applyHold(world, GameRules.ADVANCE_TIME, hold && isTimeEnabled(world), timeHeld);
        }
    }

    public void holdVanillaWeather(boolean hold) {
        for (World world : Bukkit.getWorlds()) {
            applyHold(world, GameRules.ADVANCE_WEATHER, hold && isWeatherEnabled(world), weatherHeld);
        }
    }

    /** Gives the vanilla cycles back to every world we touched. Called when the plugin shuts down. */
    public void releaseVanillaRules() {
        for (World world : Bukkit.getWorlds()) {
            applyHold(world, GameRules.ADVANCE_TIME, false, timeHeld);
            applyHold(world, GameRules.ADVANCE_WEATHER, false, weatherHeld);
        }
        timeHeld.clear();
        weatherHeld.clear();
    }

    private void applyHold(World world, GameRule<Boolean> rule, boolean hold, Set<UUID> held) {
        if (hold) {
            held.add(world.getUID());
            if (!Boolean.FALSE.equals(world.getGameRuleValue(rule))) {
                world.setGameRule(rule, false);
            }
        } else if (held.remove(world.getUID())) {
            world.setGameRule(rule, true);
        }
    }

    private String path(World world, String key) {
        return "worlds.per-world." + world.getName() + "." + key;
    }
}
