package com.pallux.sandsim.listeners;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.gui.MenuGUI;
import com.pallux.sandsim.gui.UpgradesGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final SandSimPlugin plugin;

    public PlayerInteractListener(SandSimPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        boolean isRightClick = event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK;

        if (isRightClick && plugin.getShovelManager().isSandShovel(item)) {
            event.setCancelled(true);
            new UpgradesGUI(plugin).open(player);
            return;
        }

        boolean isAnyClick = isRightClick
                || event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK;

        if (isAnyClick && plugin.getMenuItemManager().isMenuItem(item)) {
            event.setCancelled(true);
            new MenuGUI(plugin).open(player);
        }
    }
}
