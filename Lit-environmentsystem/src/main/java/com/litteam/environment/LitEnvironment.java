package com.litteam.environment;

import com.litteam.environment.command.LitCommand;
import com.litteam.environment.config.ConfigManager;
import com.litteam.environment.gui.GUIManager;
import com.litteam.environment.listener.GUIListener;
import com.litteam.environment.listener.InputListener;
import com.litteam.environment.manager.DayNightManager;
import com.litteam.environment.manager.WeatherManager;
import com.litteam.environment.manager.WorldManager;
import com.litteam.environment.scheduler.EnvironmentScheduler;
import com.litteam.environment.startup.StartupAnimation;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LitEnvironment extends JavaPlugin {

    private ConfigManager configManager;
    private WorldManager worldManager;
    private DayNightManager dayNightManager;
    private WeatherManager weatherManager;
    private GUIManager guiManager;
    private InputListener inputListener;
    private StartupAnimation startupAnimation;
    private EnvironmentScheduler scheduler;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        worldManager = new WorldManager(configManager);
        dayNightManager = new DayNightManager(this, configManager, worldManager);
        weatherManager = new WeatherManager(configManager, worldManager);
        guiManager = new GUIManager(this, configManager, worldManager, dayNightManager, weatherManager);
        inputListener = new InputListener(this);
        startupAnimation = new StartupAnimation(this, configManager);
        scheduler = new EnvironmentScheduler(this, dayNightManager, weatherManager, guiManager);

        PluginCommand command = getCommand("lit");
        if (command == null) {
            getLogger().severe("The /lit command is missing from plugin.yml, disabling the plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        LitCommand handler = new LitCommand(this);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(inputListener, this);
        getServer().getPluginManager().registerEvents(startupAnimation, this);

        weatherManager.applyDefault();
        scheduler.start();
        startupAnimation.printBanner(dayNightManager.timezone(), dayNightManager.mode(), worldManager.timeWorlds().size());
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.stop();
        }
        if (startupAnimation != null) {
            startupAnimation.stop();
        }
        // hand the vanilla day/night and weather cycles back to the server
        if (worldManager != null) {
            worldManager.releaseVanillaRules();
        }
    }

    public void reloadPlugin() {
        configManager.reload();
        worldManager.reload();
        dayNightManager.reload();
        weatherManager.reload();
        scheduler.restart();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public DayNightManager getDayNightManager() {
        return dayNightManager;
    }

    public WeatherManager getWeatherManager() {
        return weatherManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public InputListener getInputListener() {
        return inputListener;
    }

    public StartupAnimation getStartupAnimation() {
        return startupAnimation;
    }
}
