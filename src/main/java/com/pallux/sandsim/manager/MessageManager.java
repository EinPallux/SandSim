package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.utils.ColorUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageManager {

    private final SandSimPlugin plugin;

    public MessageManager(SandSimPlugin plugin) {
        this.plugin = plugin;
    }

    public String getMessage(String path) {
        String prefix  = plugin.getConfigManager().getMainConfig().getString("plugin.prefix", "&#FFD700&l[SandSim]&r ");
        String message = plugin.getConfigManager().getMessagesConfig().getString(path, "");
        if (!message.isEmpty()) return prefix + message;
        return message;
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }

    public void sendMessage(CommandSender sender, String path) {
        String message = getMessage(path);
        if (!message.isEmpty()) sender.sendMessage(ColorUtils.colorize(message));
    }

    public void sendMessage(CommandSender sender, String path, String... replacements) {
        String message = getMessage(path, replacements);
        if (!message.isEmpty()) sender.sendMessage(ColorUtils.colorize(message));
    }

    public void sendActionBar(Player player, String path) {
        String message = plugin.getConfigManager().getMessagesConfig().getString(path, "");
        if (!message.isEmpty()) player.sendActionBar(ColorUtils.toComponent(message));
    }

    public void sendActionBar(Player player, String path, String... replacements) {
        String message = plugin.getConfigManager().getMessagesConfig().getString(path, "");
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        if (!message.isEmpty()) player.sendActionBar(ColorUtils.toComponent(message));
    }

    public String getPrefix() {
        return plugin.getConfigManager().getMainConfig().getString("plugin.prefix", "&#FFD700&l[SandSim]&r ");
    }
}
