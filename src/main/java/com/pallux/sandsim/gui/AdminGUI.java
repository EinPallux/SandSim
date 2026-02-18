package com.pallux.sandsim.gui;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.utils.ColorUtils;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class AdminGUI extends BaseGUI {

    private static final String SEC = "admin";

    public AdminGUI(SandSimPlugin plugin) { super(plugin, SEC); }

    @Override
    protected void setupInventory(Player player) {
        inventory.clear();
        FileConfiguration cfg = plugin.getConfigManager().getGuiConfig();
        applyFiller(SEC);
        inventory.setItem(slotFromConfig(SEC + ".reload",   11), itemFromConfig(SEC + ".reload"));
        inventory.setItem(slotFromConfig(SEC + ".commands", 13), itemFromConfig(SEC + ".commands"));

        String version = plugin.getDescription().getVersion();
        String author  = String.join(", ", plugin.getDescription().getAuthors());
        List<String> rawLore = cfg.getStringList(SEC + ".info.lore");
        List<String> infoLore = new ArrayList<>();
        for (String line : rawLore) infoLore.add(applyPlaceholders(line, "%version%", version, "%author%", author));

        inventory.setItem(slotFromConfig(SEC + ".info", 15),
                createItem(parseMaterial(cfg.getString(SEC + ".info.material", "BOOK"), org.bukkit.Material.BOOK),
                        cfg.getString(SEC + ".info.name", "&a&lPlugin Info"), infoLore));

        applyPlaceholderItems(SEC);
    }

    @Override
    public void handleClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        if (slot == slotFromConfig(SEC + ".reload", 11)) {
            player.closeInventory();
            plugin.reload();
            plugin.getMessageManager().sendMessage(player, "messages.reload-success");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } else if (slot == slotFromConfig(SEC + ".commands", 13)) {
            player.closeInventory();
            sendHelpMessage(player);
        } else if (slot == slotFromConfig(SEC + ".info", 15)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getMessage("messages.admin-help-header")));
        String p = plugin.getMessageManager().getPrefix();
        player.sendMessage(ColorUtils.colorize(p + "&e/sandsim reload &7- Reload the plugin"));
        player.sendMessage(ColorUtils.colorize(p + "&e/sandsim give <currency> <player> <amount> &7- Give currency"));
        player.sendMessage(ColorUtils.colorize(p + "&e/sandsim take <currency> <player> <amount> &7- Take currency"));
        player.sendMessage(ColorUtils.colorize(p + "&e/sandsim set <currency> <player> <amount> &7- Set currency"));
        player.sendMessage(ColorUtils.colorize(p + "&e/sandsim reset <currency> <player> &7- Reset currency"));
        player.sendMessage(ColorUtils.colorize(p + "&e/sandsim resetallcurrencies <player> &7- Reset all currencies"));
        player.sendMessage(ColorUtils.colorize(p + "&e/sandsim restart <player> &7- Complete player reset"));
        player.sendMessage(ColorUtils.colorize(p + "&e/sandsim upgrades set <upgrade> <player> <level> &7- Set upgrade level"));
        player.sendMessage(ColorUtils.colorize(p + "&e/sandsim admin &7- Open admin GUI"));
    }
}
