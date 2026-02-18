package com.pallux.sandsim.commands;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RebirthCommand implements CommandExecutor {
    private final SandSimPlugin plugin;
    public RebirthCommand(SandSimPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only!"); return true; }
        if (!player.hasPermission("sandsim.player")) { plugin.getMessageManager().sendMessage(player, "messages.no-permission"); return true; }

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int maxRebirths = plugin.getRebirthManager().getMaxRebirths(data);

        if (maxRebirths <= 0) {
            plugin.getMessageManager().sendMessage(player, "messages.cannot-rebirth", "%cost%", plugin.getRebirthManager().getRebirthCost().toPlainString());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (plugin.getRebirthManager().performRebirth(data, maxRebirths)) {
            plugin.getMessageManager().sendMessage(player, "messages.rebirth-success", "%amount%", String.valueOf(maxRebirths));
            plugin.getMessageManager().sendMessage(player, "messages.rebirth-multiplier", "%multiplier%", String.format("%.2fx", plugin.getRebirthManager().getRebirthMultiplier(data)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } else {
            plugin.getMessageManager().sendMessage(player, "messages.rebirth-failed");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
        return true;
    }
}
