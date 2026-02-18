package com.pallux.sandsim.listeners;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final SandSimPlugin plugin;

    public PlayerJoinListener(SandSimPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data (efficiency level must be available before shovel creation)
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        // Remove duplicate shovels then refresh (keeps enchant in sync)
        plugin.getShovelManager().removeDuplicateShovels(player);
        plugin.getShovelManager().refreshShovel(player);

        // Menu item
        plugin.getMenuItemManager().giveMenuItem(player);
        plugin.getMenuItemManager().removeDuplicateMenuItems(player);
        plugin.getMenuItemManager().ensureMenuItemInSlot8(player);

        // Show event bossbar
        plugin.getEventManager().showBossBarToPlayer(player);
    }
}
