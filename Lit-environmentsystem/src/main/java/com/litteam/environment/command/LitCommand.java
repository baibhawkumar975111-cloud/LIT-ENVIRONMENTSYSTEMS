package com.litteam.environment.command;

import com.litteam.environment.LitEnvironment;
import com.litteam.environment.config.ConfigManager;
import com.litteam.environment.manager.DayNightManager;
import com.litteam.environment.manager.WeatherManager;
import com.litteam.environment.manager.WeatherManager.Weather;
import com.litteam.environment.util.MessageUtil;
import com.litteam.environment.util.TimeUtil;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LitCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_TIME = "lit.environment.time";
    private static final String PERM_WEATHER = "lit.environment.weather";
    private static final String PERM_RELOAD = "lit.environment.reload";
    private static final String PERM_ADMIN = "lit.environment.admin";

    private static final List<String> TIME_ACTIONS = List.of("status", "set", "pause", "resume");
    private static final List<String> WEATHER_ACTIONS = List.of("clear", "rain", "thunder", "automatic");
    private static final List<String> TIME_EXAMPLES = List.of("00:00", "06:00", "12:00", "18:00");

    private final LitEnvironment plugin;
    private final ConfigManager config;

    public LitCommand(LitEnvironment plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getGuiManager().openMain(player);
            } else {
                sendStatus(sender);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "time" -> handleTime(sender, args);
            case "weather" -> handleWeather(sender, args);
            case "reload" -> handleReload(sender);
            case "intro" -> handleIntro(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleTime(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_TIME)) {
            config.send(sender, "no-permission");
            return;
        }

        DayNightManager dayNight = plugin.getDayNightManager();
        String action = args.length > 1 ? args[1].toLowerCase() : "";

        switch (action) {
            case "" -> {
                if (sender instanceof Player player) {
                    plugin.getGuiManager().openTime(player);
                } else {
                    sendStatus(sender);
                }
            }
            case "status" -> sendStatus(sender);
            case "pause" -> {
                dayNight.setPaused(true);
                config.send(sender, "time-paused");
            }
            case "resume" -> {
                dayNight.setPaused(false);
                config.send(sender, "time-resumed");
            }
            case "set" -> setTime(sender, args);
            default -> config.sendText(sender, "<yellow>Usage: /lit time [status|set HH:mm|pause|resume]</yellow>");
        }
    }

    private void setTime(CommandSender sender, String[] args) {
        if (args.length < 3) {
            config.sendText(sender, "<red>Usage: /lit time set HH:mm</red>");
            return;
        }
        try {
            LocalTime time = TimeUtil.parse(args[2]);
            plugin.getDayNightManager().setTime(time);
            config.send(sender, "time-set", "{time}", TimeUtil.format(time));
        } catch (DateTimeParseException ex) {
            config.send(sender, "invalid-time");
        }
    }

    private void handleWeather(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_WEATHER)) {
            config.send(sender, "no-permission");
            return;
        }

        WeatherManager weather = plugin.getWeatherManager();

        if (args.length == 1) {
            if (sender instanceof Player player) {
                plugin.getGuiManager().openWeather(player);
            } else {
                sendStatus(sender);
            }
            return;
        }

        if (args[1].equalsIgnoreCase("automatic")) {
            boolean next = !weather.isAutomatic();
            if (args.length > 2) {
                if (args[2].equalsIgnoreCase("on")) {
                    next = true;
                } else if (args[2].equalsIgnoreCase("off")) {
                    next = false;
                } else {
                    config.sendText(sender, "<yellow>Usage: /lit weather automatic [on|off]</yellow>");
                    return;
                }
            }
            weather.setAutomatic(next);
            config.send(sender, "weather-automatic", "{state}", next ? "ON" : "OFF");
            return;
        }

        Optional<Weather> chosen = WeatherManager.parse(args[1]);
        if (chosen.isEmpty()) {
            config.sendText(sender, "<yellow>Usage: /lit weather [clear|rain|thunder|automatic]</yellow>");
            return;
        }
        weather.setAll(chosen.get());
        config.send(sender, "weather-set", "{weather}", chosen.get().name());
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(PERM_RELOAD)) {
            config.send(sender, "no-permission");
            return;
        }
        plugin.reloadPlugin();
        config.send(sender, "reloaded");
    }

    private void handleIntro(CommandSender sender) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            config.send(sender, "no-permission");
            return;
        }
        if (sender instanceof Player player) {
            plugin.getStartupAnimation().play(player);
        } else {
            config.send(sender, "player-only");
        }
    }

    private void sendStatus(CommandSender sender) {
        DayNightManager dayNight = plugin.getDayNightManager();

        config.sendText(sender, "<gold>Environment status</gold>");
        config.sendRaw(sender, "<gray>Country: <white>"
                + MessageUtil.escape(config.config().getString("day-night.country", "India")) + "</white>");
        config.sendRaw(sender, "<gray>Timezone: <white>" + dayNight.timezone() + "</white>");
        config.sendRaw(sender, "<gray>Real time: <white>" + TimeUtil.formatSeconds(dayNight.realTime()) + "</white>");
        config.sendRaw(sender, "<gray>Sync: <white>" + syncState(dayNight) + "</white>");

        if (sender instanceof Player player) {
            World world = player.getWorld();
            config.sendRaw(sender, "<gray>Sky matches: <white>" + TimeUtil.format(dayNight.clockIn(world)) + "</white>");
            config.sendRaw(sender, "<gray>Day/Night: <white>" + TimeUtil.dayOrNight(world.getTime()) + "</white>");
            config.sendRaw(sender, "<gray>Weather: <white>" + plugin.getWeatherManager().current(world) + "</white>");
        } else {
            config.sendRaw(sender, "<gray>Managed worlds: <white>" + plugin.getWorldManager().timeWorlds().size() + "</white>");
        }
    }

    private String syncState(DayNightManager dayNight) {
        if (!dayNight.isEnabled()) {
            return "DISABLED";
        }
        return dayNight.isPaused() ? "PAUSED" : "RUNNING";
    }

    private void sendHelp(CommandSender sender) {
        config.sendRaw(sender, "<gold>/lit</gold> <gray>- open the menu");
        config.sendRaw(sender, "<gold>/lit time status</gold> <gray>- show time status");
        config.sendRaw(sender, "<gold>/lit time set HH:mm</gold> <gray>- set the time and pause the sync");
        config.sendRaw(sender, "<gold>/lit time pause|resume</gold> <gray>- freeze or resume the sync");
        config.sendRaw(sender, "<gold>/lit weather clear|rain|thunder</gold> <gray>- set the weather");
        config.sendRaw(sender, "<gold>/lit weather automatic [on|off]</gold> <gray>- automatic weather");
        config.sendRaw(sender, "<gold>/lit reload</gold> <gray>- reload the configuration");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> roots = new ArrayList<>();
            if (sender.hasPermission(PERM_TIME)) {
                roots.add("time");
            }
            if (sender.hasPermission(PERM_WEATHER)) {
                roots.add("weather");
            }
            if (sender.hasPermission(PERM_RELOAD)) {
                roots.add("reload");
            }
            if (sender.hasPermission(PERM_ADMIN)) {
                roots.add("intro");
            }
            return matching(roots, args[0]);
        }

        String root = args[0].toLowerCase();

        if (root.equals("time") && sender.hasPermission(PERM_TIME)) {
            if (args.length == 2) {
                return matching(TIME_ACTIONS, args[1]);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
                return matching(TIME_EXAMPLES, args[2]);
            }
        }

        if (root.equals("weather") && sender.hasPermission(PERM_WEATHER)) {
            if (args.length == 2) {
                return matching(WEATHER_ACTIONS, args[1]);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("automatic")) {
                return matching(List.of("on", "off"), args[2]);
            }
        }

        return List.of();
    }

    private List<String> matching(List<String> options, String typed) {
        String prefix = typed.toLowerCase();
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
