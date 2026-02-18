package com.pallux.sandsim.gui;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class MenuGUI extends BaseGUI {

    private static final String SEC = "menu";

    public MenuGUI(SandSimPlugin plugin) { super(plugin, SEC); }

    @Override
    protected void setupInventory(Player player) {
        inventory.clear();
        FileConfiguration cfg = plugin.getConfigManager().getGuiConfig();
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        applyFiller(SEC);

        // Stats
        List<String> statsLore = new ArrayList<>();
        for (String line : cfg.getStringList(SEC + ".stats.lore")) statsLore.add(replacePlaceholders(line, player));
        inventory.setItem(slotFromConfig(SEC + ".stats", 11),
                createItem(parseMaterial(cfg.getString(SEC + ".stats.material", "PLAYER_HEAD"), org.bukkit.Material.PLAYER_HEAD),
                        replacePlaceholders(cfg.getString(SEC + ".stats.name", "&e&lYour Stats"), player), statsLore));

        // Guide
        inventory.setItem(slotFromConfig(SEC + ".guide", 13), itemFromConfig(SEC + ".guide"));

        // Rebirth
        int maxRebirths = plugin.getRebirthManager().getMaxRebirths(data);
        String cost       = formatNumber(plugin.getRebirthManager().getRebirthCost());
        String multiplier = String.format("%.2fx", plugin.getRebirthManager().getRebirthMultiplier(data));
        List<String> rebirthLore = new ArrayList<>();
        for (String line : cfg.getStringList(SEC + ".rebirth.lore")) {
            rebirthLore.add(applyPlaceholders(line, "%cost%", cost, "%amount%", String.valueOf(maxRebirths), "%multiplier%", multiplier));
        }
        inventory.setItem(slotFromConfig(SEC + ".rebirth", 15),
                createItem(parseMaterial(cfg.getString(SEC + ".rebirth.material", "NETHER_STAR"), org.bukkit.Material.NETHER_STAR),
                        cfg.getString(SEC + ".rebirth.name", "&d&lRebirth"), rebirthLore));

        inventory.setItem(slotFromConfig(SEC + ".upgrades",    29), itemFromConfig(SEC + ".upgrades"));
        inventory.setItem(slotFromConfig(SEC + ".factory",     31), itemFromConfig(SEC + ".factory"));
        inventory.setItem(slotFromConfig(SEC + ".leaderboard", 33), itemFromConfig(SEC + ".leaderboard"));

        applyPlaceholderItems(SEC);
    }

    @Override
    public void handleClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        if      (slot == slotFromConfig(SEC + ".rebirth",     15)) { player.closeInventory(); player.performCommand("rebirth"); }
        else if (slot == slotFromConfig(SEC + ".upgrades",    29)) { new UpgradesGUI(plugin).open(player); }
        else if (slot == slotFromConfig(SEC + ".factory",     31)) { new FactoryGUI(plugin).open(player); }
        else if (slot == slotFromConfig(SEC + ".leaderboard", 33)) { new LeaderboardGUI(plugin).open(player); }
    }
}
