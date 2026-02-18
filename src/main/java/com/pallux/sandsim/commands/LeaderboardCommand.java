package com.pallux.sandsim.commands;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.gui.LeaderboardGUI;
import com.pallux.sandsim.manager.LeaderboardManager.LeaderboardType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaderboardCommand implements CommandExecutor {
    private final SandSimPlugin plugin;
    public LeaderboardCommand(SandSimPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only!"); return true; }
        if (!player.hasPermission("sandsim.player")) { plugin.getMessageManager().sendMessage(player, "messages.no-permission"); return true; }
        LeaderboardType type = LeaderboardType.SAND;
        if (args.length > 0) {
            try { type = LeaderboardType.valueOf(args[0].toUpperCase()); }
            catch (IllegalArgumentException e) { plugin.getMessageManager().sendMessage(player, "messages.invalid-leaderboard-type"); return true; }
        }
        new LeaderboardGUI(plugin, type).open(player);
        return true;
    }
}
