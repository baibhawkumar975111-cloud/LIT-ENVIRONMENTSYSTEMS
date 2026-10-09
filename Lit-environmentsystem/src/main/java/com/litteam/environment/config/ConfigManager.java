package com.litteam.environment.config;

import com.litteam.environment.LitEnvironment;
import com.litteam.environment.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class ConfigManager {

    private final LitEnvironment plugin;
    private FileConfiguration config;
    private FileConfiguration messages;

    public ConfigManager(LitEnvironment plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reload() {
        load();
    }

    public void save() {
        plugin.saveConfig();
    }

    public FileConfiguration config() {
        return config;
    }

    public void set(String path, Object value) {
        config.set(path, value);
    }

    /** messages.yml wins over config.yml; config.yml falls back to the defaults bundled in the jar. */
    public String message(String key) {
        String override = messages.getString(key);
        if (override != null) {
            return override;
        }
        return config.getString("messages." + key, key);
    }

    public String prefix() {
        return message("prefix");
    }

    /** Sends a configured message with the prefix. Placeholders are passed as key, value pairs. */
    public void send(CommandSender to, String key, String... placeholders) {
        String text = message(key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace(placeholders[i], placeholders[i + 1]);
        }
        sendText(to, text);
    }

    public void sendText(CommandSender to, String text) {
        to.sendMessage(MessageUtil.prefixed(prefix(), text));
    }

    public void sendRaw(CommandSender to, String text) {
        to.sendMessage(MessageUtil.parse(text));
    }
}
