package com.pallux.sandsim.commands;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MultiplierCommand implements CommandExecutor {
    private final SandSimPlugin plugin;
    public MultiplierCommand(SandSimPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only!"); return true; }
        if (!player.hasPermission("sandsim.player")) { plugin.getMessageManager().sendMessage(player, "messages.no-permission"); return true; }
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        double multiplier = plugin.getRebirthManager().getRebirthMultiplier(data);
        plugin.getMessageManager().sendMessage(player, "messages.multiplier-balance", "%amount%", String.format("%.2fx", multiplier));
        return true;
    }
}
