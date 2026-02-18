package com.pallux.sandsim.listeners;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.gui.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {

    private final SandSimPlugin plugin;

    public InventoryClickListener(SandSimPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof MenuGUI) {
            event.setCancelled(true);
            ((MenuGUI) holder).handleClick(event, player);
            return;
        } else if (holder instanceof UpgradesGUI) {
            event.setCancelled(true);
            ((UpgradesGUI) holder).handleClick(event, player);
            return;
        } else if (holder instanceof FactoryGUI) {
            event.setCancelled(true);
            ((FactoryGUI) holder).handleClick(event, player);
            return;
        } else if (holder instanceof LeaderboardGUI) {
            event.setCancelled(true);
            ((LeaderboardGUI) holder).handleClick(event, player);
            return;
        } else if (holder instanceof AdminGUI) {
            event.setCancelled(true);
            ((AdminGUI) holder).handleClick(event, player);
            return;
        } else if (holder instanceof AugmentsGUI) {
            event.setCancelled(true);
            ((AugmentsGUI) holder).handleClick(event, player);
            return;
        }

        if (isLockedItem(event.getCurrentItem()) || isLockedItem(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        if (event.isShiftClick() && isLockedItem(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (isLockedItem(event.getOldCursor())) event.setCancelled(true);
    }

    private boolean isLockedItem(ItemStack item) {
        if (item == null) return false;
        return plugin.getShovelManager().isSandShovel(item)
                || plugin.getMenuItemManager().isMenuItem(item);
    }
}