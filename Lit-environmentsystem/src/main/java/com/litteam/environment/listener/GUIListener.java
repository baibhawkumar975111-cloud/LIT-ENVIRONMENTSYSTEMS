package com.litteam.environment.listener;

import com.litteam.environment.LitEnvironment;
import com.litteam.environment.gui.GUIHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GUIListener implements Listener {

    private final LitEnvironment plugin;

    public GUIListener(LitEnvironment plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder holder)) {
            return;
        }

        // Nothing may be moved in or out of our menus, including shift-clicks from the player's own inventory
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!event.getView().getTopInventory().equals(event.getClickedInventory())) {
            return;
        }

        GUIHolder.Button button = holder.button(event.getRawSlot());
        if (button == null) {
            return;
        }
        if (button.permission() != null && !player.hasPermission(button.permission())) {
            plugin.getConfigManager().send(player, "no-permission");
            return;
        }
        button.action().accept(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GUIHolder) {
            event.setCancelled(true);
        }
    }
}
