package com.pallux.sandsim.gui;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.data.PlayerData.UpgradeType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class UpgradesGUI extends BaseGUI {

    private static final String SEC = "upgrades";

    public UpgradesGUI(SandSimPlugin plugin) { super(plugin, SEC); }

    @Override
    protected void setupInventory(Player player) {
        inventory.clear();
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        applyFiller(SEC);
        inventory.setItem(slotFromConfig(SEC + ".back", 49), itemFromConfig(SEC + ".back"));

        buildUpgrade(player, data, "sand-multiplier",       UpgradeType.SAND_MULTIPLIER,       10);
        buildUpgrade(player, data, "sand-explosion-chance", UpgradeType.SAND_EXPLOSION_CHANCE,  12);
        buildUpgrade(player, data, "sand-explosion-radius", UpgradeType.SAND_EXPLOSION_RADIUS,  14);
        buildUpgrade(player, data, "sand-cooldown",         UpgradeType.SAND_COOLDOWN,          16);
        buildUpgrade(player, data, "gem-chance",            UpgradeType.GEM_CHANCE,             28);
        buildUpgrade(player, data, "efficiency",            UpgradeType.EFFICIENCY,             31);
        buildUpgrade(player, data, "gem-multiplier",        UpgradeType.GEM_MULTIPLIER,         34);

        applyPlaceholderItems(SEC);
    }

    private void buildUpgrade(Player player, PlayerData data, String key, UpgradeType type, int defaultSlot) {
        FileConfiguration cfg = plugin.getConfigManager().getGuiConfig();
        String path = SEC + "." + key;
        int        currentLevel = data.getUpgradeLevel(type);
        int        maxLevel     = plugin.getUpgradeManager().getMaxLevel(type);
        BigDecimal cost         = plugin.getUpgradeManager().getUpgradeCost(type, currentLevel);
        boolean    canAfford    = data.getSand().compareTo(cost) >= 0;
        boolean    isMaxed      = currentLevel >= maxLevel;

        Material mat  = parseMaterial(cfg.getString(path + ".material", "STONE"), Material.STONE);
        String   name = cfg.getString(path + ".name", key);
        String lorePath = isMaxed ? path + ".lore-maxed" : (!canAfford ? path + ".lore-cannot-afford" : path + ".lore");
        List<String> lore = new ArrayList<>();
        for (String line : cfg.getStringList(lorePath)) {
            lore.add(applyPlaceholders(line,
                    "%level%",   String.valueOf(currentLevel),
                    "%max%",     String.valueOf(maxLevel),
                    "%current%", formatValue(type, currentLevel),
                    "%next%",    isMaxed ? "" : formatValue(type, currentLevel + 1),
                    "%cost%",    formatNumber(cost)));
        }
        inventory.setItem(slotFromConfig(path, defaultSlot), createItem(mat, name, lore));
    }

    private String formatValue(UpgradeType type, int level) {
        if (type == UpgradeType.EFFICIENCY) {
            int v = (int) plugin.getUpgradeManager().getUpgradeValue(type, level);
            return v == 0 ? "None" : String.valueOf(v);
        }
        double val = plugin.getUpgradeManager().getUpgradeValue(type, level);
        if (val == Math.floor(val) && !Double.isInfinite(val)) return String.valueOf((int) val);
        return String.valueOf(val);
    }

    @Override
    public void handleClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        if (slot == slotFromConfig(SEC + ".back", 49)) { new MenuGUI(plugin).open(player); return; }

        UpgradeType type = resolveUpgradeType(slot);
        if (type == null) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        if (plugin.getUpgradeManager().purchaseUpgrade(data, type)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            plugin.getMessageManager().sendMessage(player, "messages.upgrade-purchased");
            if (type == UpgradeType.EFFICIENCY) plugin.getShovelManager().refreshShovel(player);
            setupInventory(player);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            plugin.getMessageManager().sendMessage(player, "messages.cannot-afford-upgrade");
        }
    }

    private UpgradeType resolveUpgradeType(int slot) {
        if (slot == slotFromConfig(SEC + ".sand-multiplier",       10)) return UpgradeType.SAND_MULTIPLIER;
        if (slot == slotFromConfig(SEC + ".sand-explosion-chance", 12)) return UpgradeType.SAND_EXPLOSION_CHANCE;
        if (slot == slotFromConfig(SEC + ".sand-explosion-radius", 14)) return UpgradeType.SAND_EXPLOSION_RADIUS;
        if (slot == slotFromConfig(SEC + ".sand-cooldown",         16)) return UpgradeType.SAND_COOLDOWN;
        if (slot == slotFromConfig(SEC + ".gem-chance",            28)) return UpgradeType.GEM_CHANCE;
        if (slot == slotFromConfig(SEC + ".efficiency",            31)) return UpgradeType.EFFICIENCY;
        if (slot == slotFromConfig(SEC + ".gem-multiplier",        34)) return UpgradeType.GEM_MULTIPLIER;
        return null;
    }
}
