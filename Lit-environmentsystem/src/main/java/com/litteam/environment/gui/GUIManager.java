package com.litteam.environment.gui;

import com.litteam.environment.LitEnvironment;
import com.litteam.environment.config.ConfigManager;
import com.litteam.environment.manager.DayNightManager;
import com.litteam.environment.manager.WeatherManager;
import com.litteam.environment.manager.WeatherManager.Weather;
import com.litteam.environment.manager.WorldManager;
import com.litteam.environment.util.MessageUtil;
import com.litteam.environment.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public final class GUIManager {

    private static final String PERM_USE = "lit.environment.use";
    private static final String PERM_TIME = "lit.environment.time";
    private static final String PERM_WEATHER = "lit.environment.weather";
    private static final String PERM_RELOAD = "lit.environment.reload";
    private static final String PERM_ADMIN = "lit.environment.admin";

    // menus that show a clock, so they get refreshed every second while someone has them open
    private static final Set<String> LIVE_VIEWS = Set.of("time", "status");

    private static final long[] CYCLE_STEPS = {300, 900, 1800, 3600};

    private final LitEnvironment plugin;
    private final ConfigManager config;
    private final WorldManager worlds;
    private final DayNightManager dayNight;
    private final WeatherManager weather;

    public GUIManager(LitEnvironment plugin, ConfigManager config, WorldManager worlds,
                      DayNightManager dayNight, WeatherManager weather) {
        this.plugin = plugin;
        this.config = config;
        this.worlds = worlds;
        this.dayNight = dayNight;
        this.weather = weather;
    }


    public void openMain(Player player) {
        open(player, "main", 27, title("main-title", "LIT-Team Environment"), this::fillMain);
    }

    public void openTime(Player player) {
        open(player, "time", 36, title("time-title", "LIT-Team • Day/Night"), this::fillTime);
    }

    public void openWeather(Player player) {
        open(player, "weather", 27, title("weather-title", "LIT-Team • Weather"), this::fillWeather);
    }

    public void openWorlds(Player player) {
        // one slot per world plus the back button, in rows of nine
        int needed = Bukkit.getWorlds().size() + 1;
        int size = Math.max(27, Math.min(54, ((needed + 8) / 9) * 9));
        open(player, "worlds", size, title("world-title", "LIT-Team • Worlds"), this::fillWorlds);
    }

    public void openConfiguration(Player player) {
        open(player, "config", 27, title("config-title", "LIT-Team • Configuration"), this::fillConfiguration);
    }

    public void openStatus(Player player) {
        open(player, "status", 27, title("status-title", "LIT-Team • Status"), this::fillStatus);
    }

    /** Rebuilds the menu the player currently has open, without closing it. */
    public void refresh(Player player) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof GUIHolder holder) {
            holder.fill(player);
        }
    }

    public void refreshLiveViews() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof GUIHolder holder && LIVE_VIEWS.contains(holder.id())) {
                holder.fill(player);
            }
        }
    }

    private void open(Player player, String id, int size, String title, BiConsumer<GUIHolder, Player> filler) {
        GUIHolder holder = new GUIHolder(id, (h, p) -> {
            background(h);
            filler.accept(h, p);
        });
        Inventory inventory = Bukkit.createInventory(holder, size, MessageUtil.parse(title));
        holder.attach(inventory);
        holder.fill(player);
        player.openInventory(inventory);
    }


    private void fillMain(GUIHolder holder, Player player) {
        holder.put(10, icon(Material.CLOCK, "<gold>Day/Night System</gold>",
                "<gray>Control real-time world time.</gray>", "<yellow>Click to open</yellow>"),
                PERM_TIME, this::openTime);

        holder.put(12, icon(Material.WATER_BUCKET, "<aqua>Weather System</aqua>",
                "<gray>Control rain and thunder.</gray>", "<yellow>Click to open</yellow>"),
                PERM_WEATHER, this::openWeather);

        holder.put(14, icon(Material.GRASS_BLOCK, "<green>World Settings</green>",
                "<gray>Choose which worlds are managed.</gray>", "<yellow>Click to open</yellow>"),
                PERM_ADMIN, this::openWorlds);

        holder.put(16, icon(Material.COMPASS, "<white>Current Status</white>",
                "<gray>View environment status.</gray>", "<yellow>Click to open</yellow>"),
                PERM_USE, this::openStatus);

        holder.put(20, icon(Material.PAPER, "<light_purple>Configuration</light_purple>",
                "<gray>View the active configuration.</gray>", "<yellow>Click to open</yellow>"),
                PERM_USE, this::openConfiguration);

        holder.put(22, icon(Material.REDSTONE, "<red>Reload</red>",
                "<gray>Reload config.yml and messages.yml.</gray>", "<yellow>Click to reload</yellow>"),
                PERM_RELOAD, p -> {
                    plugin.reloadPlugin();
                    config.send(p, "reloaded");
                    refresh(p);
                });
    }

    private void fillTime(GUIHolder holder, Player player) {
        World world = player.getWorld();
        boolean paused = dayNight.isPaused();

        holder.put(10, icon(dayNight.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                dayNight.isEnabled() ? "<green>Sync enabled</green>" : "<red>Sync disabled</red>",
                "<gray>Day/night synchronization.</gray>"));

        holder.put(11, icon(Material.SUNFLOWER, "<yellow>Real Time</yellow>",
                "<gray>Mode: <white>" + dayNight.mode() + "</white></gray>",
                "<gray>Zone: <white>" + dayNight.timezone() + "</white></gray>",
                "<gray>Now: <white>" + TimeUtil.formatSeconds(dayNight.realTime()) + "</white></gray>"));

        holder.put(12, icon(Material.CLOCK, "<gold>Minecraft Time</gold>",
                "<gray>World: <white>" + MessageUtil.escape(world.getName()) + "</white></gray>",
                "<gray>Sky matches: <white>" + TimeUtil.format(dayNight.clockIn(world)) + "</white></gray>",
                "<gray>Ticks: <white>" + world.getTime() + "</white></gray>"));

        holder.put(14, icon(paused ? Material.REDSTONE_TORCH : Material.TORCH,
                paused ? "<red>Resume Sync</red>" : "<green>Pause Sync</green>",
                paused ? "<gray>The clock is frozen. Click to follow real time.</gray>"
                       : "<gray>Click to freeze the clock where it is.</gray>"),
                PERM_TIME, p -> {
                    boolean nowPaused = !dayNight.isPaused();
                    dayNight.setPaused(nowPaused);
                    config.send(p, nowPaused ? "time-paused" : "time-resumed");
                    refresh(p);
                });

        holder.put(16, icon(Material.DAYLIGHT_DETECTOR, "<yellow>Set Day</yellow>",
                "<gray>Noon (12:00). Pauses the sync.</gray>"),
                PERM_TIME, p -> holdTime(p, LocalTime.NOON));

        holder.put(19, icon(Material.SOUL_LANTERN, "<aqua>Set Night</aqua>",
                "<gray>Midnight (00:00). Pauses the sync.</gray>"),
                PERM_TIME, p -> holdTime(p, LocalTime.MIDNIGHT));

        holder.put(21, icon(Material.NAME_TAG, "<white>Set Custom Time</white>",
                "<gray>Click, then type HH:mm in chat.</gray>"),
                PERM_TIME, p -> plugin.getInputListener().requestTime(p));

        holder.put(23, icon(Material.SUNFLOWER, "<gold>Sunrise</gold>",
                "<gray>Configured: <white>" + TimeUtil.format(dayNight.sunrise()) + "</white></gray>"));

        holder.put(25, icon(Material.REDSTONE_TORCH, "<red>Sunset</red>",
                "<gray>Configured: <white>" + TimeUtil.format(dayNight.sunset()) + "</white></gray>"));

        back(holder, 31);
    }

    private void fillWeather(GUIHolder holder, Player player) {
        Weather current = weather.current(player.getWorld());

        holder.put(10, weatherIcon(Material.SUNFLOWER, "<yellow>Clear</yellow>", current == Weather.CLEAR),
                PERM_WEATHER, p -> changeWeather(p, Weather.CLEAR));

        holder.put(12, weatherIcon(Material.WATER_BUCKET, "<aqua>Rain</aqua>", current == Weather.RAIN),
                PERM_WEATHER, p -> changeWeather(p, Weather.RAIN));

        holder.put(14, weatherIcon(Material.LIGHTNING_ROD, "<red>Thunder</red>", current == Weather.THUNDER),
                PERM_WEATHER, p -> changeWeather(p, Weather.THUNDER));

        boolean automatic = weather.isAutomatic();
        holder.put(16, icon(automatic ? Material.LIME_DYE : Material.GRAY_DYE,
                automatic ? "<green>Automatic Weather: ON</green>" : "<gray>Automatic Weather: OFF</gray>",
                "<gray>Click to toggle.</gray>"),
                PERM_WEATHER, p -> {
                    weather.setAutomatic(!weather.isAutomatic());
                    refresh(p);
                });

        holder.put(25, icon(Material.CLOCK, "<gold>Automatic Cycle</gold>",
                "<gray>Picks new weather every: <white>" + describeSeconds(weather.cycleSeconds()) + "</white></gray>",
                "<yellow>Click to change</yellow>"),
                PERM_WEATHER, p -> {
                    weather.setCycleSeconds(nextCycleStep(weather.cycleSeconds()));
                    refresh(p);
                });

        back(holder, 22);
    }

    private void fillWorlds(GUIHolder holder, Player player) {
        int backSlot = holder.size() - 1;
        int slot = 0;

        for (World world : Bukkit.getWorlds()) {
            if (slot >= backSlot) {
                break;
            }
            boolean managed = worlds.isManaged(world);
            boolean enabled = managed && worlds.isWorldEnabled(world);
            Material material = enabled ? Material.LIME_WOOL : (managed ? Material.YELLOW_WOOL : Material.GRAY_WOOL);
            String name = MessageUtil.escape(world.getName());

            ItemStack icon = icon(material, (enabled ? "<green>" : "<gray>") + name,
                    "<gray>Managed: <white>" + (managed ? "YES" : "NO") + "</white></gray>",
                    "<gray>Environment: <white>" + (enabled ? "ENABLED" : "DISABLED") + "</white></gray>",
                    "<gray>Sky matches: <white>" + TimeUtil.format(dayNight.clockIn(world)) + "</white></gray>",
                    managed ? "<yellow>Click to toggle.</yellow>" : "<dark_gray>Not managed by this plugin.</dark_gray>");

            holder.put(slot++, icon, PERM_ADMIN, p -> {
                if (worlds.isManaged(world)) {
                    worlds.setWorldEnabled(world, !worlds.isWorldEnabled(world));
                    refresh(p);
                }
            });
        }

        back(holder, backSlot);
    }

    private void fillConfiguration(GUIHolder holder, Player player) {
        holder.put(10, icon(Material.CLOCK, "<gold>Time</gold>",
                "<gray>Mode: <white>" + dayNight.mode() + "</white></gray>",
                "<gray>Timezone: <white>" + dayNight.timezone() + "</white></gray>"));

        holder.put(12, icon(Material.WATER_BUCKET, "<aqua>Weather</aqua>",
                "<gray>Automatic: <white>" + (weather.isAutomatic() ? "ON" : "OFF") + "</white></gray>",
                "<gray>Cycle: <white>" + describeSeconds(weather.cycleSeconds()) + "</white></gray>"));

        holder.put(14, icon(Material.GRASS_BLOCK, "<green>Worlds</green>",
                "<gray>Mode: <white>" + config.config().getString("worlds.mode", "blacklist") + "</white></gray>",
                "<gray>Listed: <white>" + worlds.configuredWorldNames().size() + "</white></gray>"));

        holder.put(16, icon(Material.FIREWORK_ROCKET, "<yellow>Startup</yellow>",
                "<gray>Join intro: <white>" + (config.config().getBoolean("startup.animation", true) ? "ON" : "OFF") + "</white></gray>"));

        back(holder, 22);
    }

    private void fillStatus(GUIHolder holder, Player player) {
        World world = player.getWorld();

        holder.put(10, icon(Material.CLOCK, "<gold>Time Status</gold>",
                "<gray>Country: <white>" + MessageUtil.escape(config.config().getString("day-night.country", "India")) + "</white></gray>",
                "<gray>Timezone: <white>" + dayNight.timezone() + "</white></gray>",
                "<gray>Real time: <white>" + TimeUtil.formatSeconds(dayNight.realTime()) + "</white></gray>",
                "<gray>Sky matches: <white>" + TimeUtil.format(dayNight.clockIn(world)) + "</white></gray>",
                "<gray>Day/Night: <white>" + TimeUtil.dayOrNight(world.getTime()) + "</white></gray>",
                "<gray>Sync: <white>" + (dayNight.isPaused() ? "PAUSED" : "RUNNING") + "</white></gray>"));

        holder.put(14, icon(Material.WATER_BUCKET, "<aqua>Weather</aqua>",
                "<gray>World: <white>" + MessageUtil.escape(world.getName()) + "</white></gray>",
                "<gray>Weather: <white>" + weather.current(world) + "</white></gray>"));

        holder.put(16, icon(Material.LIME_DYE, "<green>System</green>",
                "<gray>Day/Night: <white>" + (dayNight.isEnabled() ? "ENABLED" : "DISABLED") + "</white></gray>",
                "<gray>Weather: <white>" + (weather.isEnabled() ? "ENABLED" : "DISABLED") + "</white></gray>"));

        back(holder, 22);
    }


    private void holdTime(Player player, LocalTime time) {
        dayNight.setTime(time);
        config.send(player, "time-set", "{time}", TimeUtil.format(time));
        refresh(player);
    }

    private void changeWeather(Player player, Weather type) {
        weather.setAll(type);
        config.send(player, "weather-set", "{weather}", type.name());
        refresh(player);
    }

    private long nextCycleStep(long current) {
        for (long step : CYCLE_STEPS) {
            if (step > current) {
                return step;
            }
        }
        return CYCLE_STEPS[0];
    }

    private String describeSeconds(long seconds) {
        if (seconds % 60 == 0) {
            return (seconds / 60) + " min";
        }
        return seconds + " s";
    }


    private String title(String key, String fallback) {
        return config.config().getString("gui." + key, fallback);
    }

    private void background(GUIHolder holder) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        for (int slot = 0; slot < holder.size(); slot++) {
            holder.put(slot, pane);
        }
    }

    private void back(GUIHolder holder, int slot) {
        holder.put(slot, icon(Material.ARROW, "<gray>Back</gray>"), null, this::openMain);
    }

    private ItemStack weatherIcon(Material material, String name, boolean active) {
        ItemStack stack = icon(material, name,
                active ? "<green>Currently active</green>" : "<gray>Click to apply to all managed worlds.</gray>");
        if (active) {
            ItemMeta meta = stack.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack icon(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(MessageUtil.item(name));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(MessageUtil.item(line));
            }
            meta.lore(lines);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
