package com.pallux.sandsim.commands;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.gui.AugmentsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AugmentsCommand implements CommandExecutor {

    private final SandSimPlugin plugin;

    public AugmentsCommand(SandSimPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        if (!player.hasPermission("sandsim.player")) {
            plugin.getMessageManager().sendMessage(player, "messages.no-permission");
            return true;
        }
        new AugmentsGUI(plugin).open(player);
        return true;
    }
}