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

public class FactoryGUI extends BaseGUI {

    private static final String SEC = "factory";

    public FactoryGUI(SandSimPlugin plugin) { super(plugin, SEC); }

    @Override
    protected void setupInventory(Player player) {
        inventory.clear();
        FileConfiguration cfg = plugin.getConfigManager().getGuiConfig();
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        applyFiller(SEC);
        inventory.setItem(slotFromConfig(SEC + ".back", 49), itemFromConfig(SEC + ".back"));

        if (!data.isFactoryUnlocked()) {
            buildUnlockItem(player, data, cfg);
        } else {
            buildCoreItem(data, cfg);
            buildFactoryUpgrade(player, data, "production-speed",  UpgradeType.FACTORY_PRODUCTION_SPEED,  29);
            buildFactoryUpgrade(player, data, "production-amount", UpgradeType.FACTORY_PRODUCTION_AMOUNT, 33);
        }
        applyPlaceholderItems(SEC);
    }

    private void buildUnlockItem(Player player, PlayerData data, FileConfiguration cfg) {
        String path      = SEC + ".unlock";
        String costStr   = formatNumber(plugin.getFactoryManager().getFactoryUnlockCost());
        int    reqLevel  = plugin.getFactoryManager().getFactoryUnlockLevel();
        boolean meetsLevel = plugin.getFactoryManager().meetsLevelRequirement(data);
        boolean canAfford  = data.getSand().compareTo(plugin.getFactoryManager().getFactoryUnlockCost()) >= 0;
        boolean canUnlock  = meetsLevel && canAfford;

        Material mat  = parseMaterial(cfg.getString(path + ".material", "BARRIER"), Material.BARRIER);
        String   name = cfg.getString(path + ".name", "&c&lFactory Locked");

        // Pick the most specific lore key available, falling back gracefully.
        String lorePath;
        if (!meetsLevel) {
            lorePath = path + ".lore-no-level";
        } else if (!canAfford) {
            lorePath = path + ".lore-cannot-afford";
        } else {
            lorePath = path + ".lore";
        }

        // If the specific key has no entries, fall back to the base lore key so
        // the GUI never shows an empty item.
        List<String> rawLore = cfg.getStringList(lorePath);
        if (rawLore.isEmpty()) rawLore = cfg.getStringList(path + ".lore");

        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            lore.add(applyPlaceholders(line,
                    "%cost%",      costStr,
                    "%req_level%", String.valueOf(reqLevel),
                    "%level%",     String.valueOf(data.getLevel())));
        }
        inventory.setItem(slotFromConfig(path, 13), createItem(mat, name, lore));
    }

    private void buildCoreItem(PlayerData data, FileConfiguration cfg) {
        String path   = SEC + ".core";
        double speed  = plugin.getUpgradeManager().getFactoryProductionSpeed(data);
        double amount = plugin.getUpgradeManager().getFactoryProductionAmount(data);
        long   timeMs = plugin.getFactoryManager().getTimeUntilNextProduction(data);
        Material mat  = parseMaterial(cfg.getString(path + ".material", "BLAST_FURNACE"), Material.BLAST_FURNACE);
        String   name = cfg.getString(path + ".name", "&6&lFactory Core");
        List<String> lore = new ArrayList<>();
        for (String line : cfg.getStringList(path + ".lore")) {
            lore.add(applyPlaceholders(line,
                    "%speed%",  String.format("%.2f", speed),
                    "%amount%", String.format("%.0f", amount),
                    "%time%",   String.format("%.1f", timeMs / 1000.0)));
        }
        inventory.setItem(slotFromConfig(path, 13), createItem(mat, name, lore));
    }

    private void buildFactoryUpgrade(Player player, PlayerData data, String key, UpgradeType type, int defaultSlot) {
        FileConfiguration cfg = plugin.getConfigManager().getGuiConfig();
        String path        = SEC + "." + key;
        int        currentLevel = data.getUpgradeLevel(type);
        int        maxLevel     = plugin.getUpgradeManager().getMaxLevel(type);
        BigDecimal cost         = plugin.getUpgradeManager().getUpgradeCost(type, currentLevel);
        boolean    canAfford    = data.getSandbucks().compareTo(cost) >= 0;
        boolean    isMaxed      = currentLevel >= maxLevel;
        double currentValue = plugin.getUpgradeManager().getUpgradeValue(type, currentLevel);
        double nextValue    = isMaxed ? 0 : plugin.getUpgradeManager().getUpgradeValue(type, currentLevel + 1);

        Material mat  = parseMaterial(cfg.getString(path + ".material", "STONE"), Material.STONE);
        String   name = cfg.getString(path + ".name", key);
        String lorePath = isMaxed ? path + ".lore-maxed" : (!canAfford ? path + ".lore-cannot-afford" : path + ".lore");
        List<String> lore = new ArrayList<>();
        for (String line : cfg.getStringList(lorePath)) {
            lore.add(applyPlaceholders(line,
                    "%level%",   String.valueOf(currentLevel),
                    "%max%",     String.valueOf(maxLevel),
                    "%current%", formatUpgradeValue(currentValue),
                    "%next%",    isMaxed ? "" : formatUpgradeValue(nextValue),
                    "%cost%",    formatNumber(cost)));
        }
        inventory.setItem(slotFromConfig(path, defaultSlot), createItem(mat, name, lore));
    }

    private String formatUpgradeValue(double val) {
        if (val == Math.floor(val) && !Double.isInfinite(val)) return String.valueOf((int) val);
        return String.valueOf(val);
    }

    @Override
    public void handleClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        if (slot == slotFromConfig(SEC + ".back", 49)) { new MenuGUI(plugin).open(player); return; }

        int unlockSlot = slotFromConfig(SEC + ".unlock", 4);
        if (slot == unlockSlot && !data.isFactoryUnlocked()) {
            // Level check first — gives a clearer message if the player can't unlock yet.
            if (!plugin.getFactoryManager().meetsLevelRequirement(data)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                plugin.getMessageManager().sendMessage(player, "messages.factory-level-required",
                        "%level%", String.valueOf(plugin.getFactoryManager().getFactoryUnlockLevel()));
                return;
            }
            if (plugin.getFactoryManager().unlockFactory(data)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                plugin.getMessageManager().sendMessage(player, "messages.factory-unlocked");
                setupInventory(player);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                plugin.getMessageManager().sendMessage(player, "messages.cannot-afford-factory");
            }
            return;
        }

        if (!data.isFactoryUnlocked()) return;

        UpgradeType type = null;
        if      (slot == slotFromConfig(SEC + ".production-speed",  29)) type = UpgradeType.FACTORY_PRODUCTION_SPEED;
        else if (slot == slotFromConfig(SEC + ".production-amount", 33)) type = UpgradeType.FACTORY_PRODUCTION_AMOUNT;
        if (type == null) return;

        if (plugin.getFactoryManager().purchaseFactoryUpgrade(data, type)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            plugin.getMessageManager().sendMessage(player, "messages.upgrade-purchased");
            setupInventory(player);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            plugin.getMessageManager().sendMessage(player, "messages.cannot-afford-upgrade");
        }
    }
}