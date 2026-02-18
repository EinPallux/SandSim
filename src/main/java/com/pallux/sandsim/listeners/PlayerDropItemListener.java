package com.pallux.sandsim.listeners;

import com.pallux.sandsim.SandSimPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropItemListener implements Listener {

    private final SandSimPlugin plugin;

    public PlayerDropItemListener(SandSimPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getShovelManager().isSandShovel(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "messages.cannot-drop-shovel");
        }
        if (plugin.getMenuItemManager().isMenuItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "messages.cannot-drop-menu");
        }
    }
}
