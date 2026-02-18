package com.pallux.sandsim.commands;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.utils.NumberFormatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SandbucksCommand implements CommandExecutor {
    private final SandSimPlugin plugin;
    public SandbucksCommand(SandSimPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only!"); return true; }
        if (!player.hasPermission("sandsim.player")) { plugin.getMessageManager().sendMessage(player, "messages.no-permission"); return true; }
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        plugin.getMessageManager().sendMessage(player, "messages.sandbucks-balance", "%amount%", NumberFormatter.format(data.getSandbucks()));
        return true;
    }
}
