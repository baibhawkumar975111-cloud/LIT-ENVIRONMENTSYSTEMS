package com.litteam.environment.listener;

import com.litteam.environment.LitEnvironment;
import com.litteam.environment.config.ConfigManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import com.litteam.environment.util.TimeUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Handles the "type a time in chat" step of the day/night menu. */
public final class InputListener implements Listener {

    private final LitEnvironment plugin;

    // chat events arrive on another thread, so a plain HashSet is not safe here
    private final Set<UUID> waitingForTime = ConcurrentHashMap.newKeySet();

    public InputListener(LitEnvironment plugin) {
        this.plugin = plugin;
    }

    public void requestTime(Player player) {
        waitingForTime.add(player.getUniqueId());
        plugin.getServer().getScheduler().runTask(plugin, () -> player.closeInventory());
        plugin.getConfigManager().send(player, "time-prompt");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!waitingForTime.contains(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);

        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> handleInput(player, input));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        waitingForTime.remove(event.getPlayer().getUniqueId());
    }

    private void handleInput(Player player, String input) {
        ConfigManager config = plugin.getConfigManager();

        if (input.equalsIgnoreCase("cancel")) {
            waitingForTime.remove(player.getUniqueId());
            plugin.getGuiManager().openTime(player);
            return;
        }

        try {
            LocalTime time = TimeUtil.parse(input);
            waitingForTime.remove(player.getUniqueId());
            plugin.getDayNightManager().setTime(time);
            config.send(player, "time-set", "{time}", TimeUtil.format(time));
            plugin.getGuiManager().openTime(player);
        } catch (DateTimeParseException ex) {
            // stay in input mode so they can try again
            config.send(player, "invalid-time");
        }
    }
}
