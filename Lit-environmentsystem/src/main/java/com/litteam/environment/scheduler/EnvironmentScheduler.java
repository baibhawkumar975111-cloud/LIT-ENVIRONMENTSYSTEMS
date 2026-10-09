package com.litteam.environment.scheduler;

import com.litteam.environment.LitEnvironment;
import com.litteam.environment.gui.GUIManager;
import com.litteam.environment.manager.DayNightManager;
import com.litteam.environment.manager.WeatherManager;
import org.bukkit.scheduler.BukkitTask;

public final class EnvironmentScheduler {

    private final LitEnvironment plugin;
    private final DayNightManager dayNight;
    private final WeatherManager weather;
    private final GUIManager gui;

    private BukkitTask task;
    private long ticks;

    public EnvironmentScheduler(LitEnvironment plugin, DayNightManager dayNight, WeatherManager weather, GUIManager gui) {
        this.plugin = plugin;
        this.dayNight = dayNight;
        this.weather = weather;
        this.gui = gui;
    }

    public void start() {
        stop();
        ticks = 0;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> run(), 1L, 1L);
    }

    public void restart() {
        start();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void run() {
        dayNight.tick();

        // everything below only needs to happen once a second
        if (++ticks % 20 != 0) {
            return;
        }
        weather.tick();
        dayNight.refreshRules();
        weather.refreshRules();
        gui.refreshLiveViews();
    }
}
