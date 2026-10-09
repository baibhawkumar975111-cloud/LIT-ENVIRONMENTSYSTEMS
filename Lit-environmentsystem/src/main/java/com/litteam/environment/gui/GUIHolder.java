package com.litteam.environment.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Backs one of our menus. It remembers what every clickable slot does, so the
 * click listener never has to know which slot belongs to which button.
 */
public final class GUIHolder implements InventoryHolder {

    /** permission may be null when anyone who can see the menu may click it. */
    public record Button(String permission, Consumer<Player> action) {
    }

    private final String id;
    private final BiConsumer<GUIHolder, Player> filler;
    private final Map<Integer, Button> buttons = new HashMap<>();
    private Inventory inventory;

    public GUIHolder(String id, BiConsumer<GUIHolder, Player> filler) {
        this.id = id;
        this.filler = filler;
    }

    public void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    /** Clears the menu and builds it again, used both for opening and for refreshing in place. */
    public void fill(Player viewer) {
        buttons.clear();
        inventory.clear();
        filler.accept(this, viewer);
    }

    public void put(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    public void put(int slot, ItemStack item, String permission, Consumer<Player> action) {
        inventory.setItem(slot, item);
        buttons.put(slot, new Button(permission, action));
    }

    public Button button(int slot) {
        return buttons.get(slot);
    }

    public int size() {
        return inventory.getSize();
    }

    public String id() {
        return id;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
