package com.litteam.environment.startup;

import com.litteam.environment.LitEnvironment;
import com.litteam.environment.config.ConfigManager;
import com.litteam.environment.util.MessageUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The "LIT-Team presents" intro. The console gets a banner when the plugin starts,
 * and players get a title with a rising particle ring when they join.
 */
public final class StartupAnimation implements Listener {

    private static final String[] LOGO = {
            "   _        ___    _____ ",
            "  | |      |_ _|  |_   _|",
            "  | |       | |     | |  ",
            "  | |___    | |     | |  ",
            "  |_____|  |___|    |_|  "
    };

    // The server has not finished sending the world right after a join, give the client a moment
    private static final long JOIN_DELAY_TICKS = 40L;

    private final LitEnvironment plugin;
    private final ConfigManager config;
    private final Map<UUID, BukkitTask> running = new HashMap<>();

    public StartupAnimation(LitEnvironment plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void printBanner(String timezone, String mode, int worldCount) {
        if (!config.config().getBoolean("startup.console-banner", true)) {
            return;
        }

        ConsoleCommandSender console = plugin.getServer().getConsoleSender();
        console.sendMessage(MessageUtil.parse(""));
        for (String line : LOGO) {
            console.sendMessage(MessageUtil.parse("<gold>" + line));
        }
        console.sendMessage(MessageUtil.parse(""));
        console.sendMessage(MessageUtil.parse("  <gray>LIT-Team <white><bold>PRESENTS</bold></white>"));
        console.sendMessage(MessageUtil.parse("  <yellow>Environment System</yellow> <dark_gray>v"
                + plugin.getPluginMeta().getVersion()));
        console.sendMessage(MessageUtil.parse("  <gray>Timezone <white>" + MessageUtil.escape(timezone)
                + "</white> <dark_gray>|</dark_gray> Mode <white>" + mode
                + "</white> <dark_gray>|</dark_gray> Worlds <white>" + worldCount + "</white>"));
        console.sendMessage(MessageUtil.parse(""));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.config().getBoolean("startup.animation", true)) {
            return;
        }
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                play(player);
            }
        }, JOIN_DELAY_TICKS);
    }

    /** Plays the intro for one player. Also used by /lit intro so admins can preview it. */
    public void play(Player player) {
        cancel(player.getUniqueId());

        int duration = Math.max(20, config.config().getInt("startup.duration-ticks", 50));

        showTitle(player, duration);
        if (config.config().getBoolean("startup.sound", true)) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.3f);
        }

        if (!config.config().getBoolean("startup.particle-circle", true)) {
            return;
        }

        int points = Math.max(8, config.config().getInt("startup.points", 24));
        double radius = Math.max(0.5, config.config().getDouble("startup.radius", 1.4));
        Particle particle = readParticle(config.config().getString("startup.particle", "END_ROD"));
        UUID id = player.getUniqueId();

        BukkitTask task = new BukkitRunnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || elapsed >= duration) {
                    cancel();
                    running.remove(id);
                    return;
                }
                drawRing(player, particle, points, radius, (double) elapsed / duration, elapsed);
                elapsed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        running.put(id, task);
    }

    private void showTitle(Player player, int durationTicks) {
        long totalMillis = durationTicks * 50L;
        Title.Times times = Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofMillis(Math.max(500, totalMillis - 1000)),
                Duration.ofMillis(500));

        player.showTitle(Title.title(
                MessageUtil.parse(config.config().getString("startup.title", "<gold><bold>LIT-Team</bold></gold>")),
                MessageUtil.parse(config.config().getString("startup.subtitle", "<gray>P R E S E N T S</gray>")),
                times));
    }

    // The ring starts at the player's feet and climbs above their head while spinning.
    private void drawRing(Player player, Particle particle, int points, double radius, double progress, int tick) {
        Location base = player.getLocation();
        double y = base.getY() + 0.1 + progress * 2.0;
        double spin = tick * 0.25;

        for (int i = 0; i < points; i++) {
            double angle = spin + (Math.PI * 2.0 * i / points);
            double x = base.getX() + Math.cos(angle) * radius;
            double z = base.getZ() + Math.sin(angle) * radius;
            player.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    private Particle readParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown particle '" + name + "' in config.yml, using END_ROD.");
            return Particle.END_ROD;
        }
    }

    private void cancel(UUID id) {
        BukkitTask task = running.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    public void stop() {
        for (BukkitTask task : running.values()) {
            task.cancel();
        }
        running.clear();
    }
}
