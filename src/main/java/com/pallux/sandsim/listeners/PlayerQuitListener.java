package com.pallux.sandsim.listeners;

import com.pallux.sandsim.SandSimPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final SandSimPlugin plugin;

    public PlayerQuitListener(SandSimPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getDataManager().unloadPlayerData(event.getPlayer());
    }
}
