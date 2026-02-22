package com.pallux.sandsim.gui;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.data.PlayerData.UpgradeType;
import com.pallux.sandsim.manager.UpgradeManager.Currency;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class UpgradesGUI extends BaseGUI {

    private static final String SEC = "upgrades-gui";

    public UpgradesGUI(SandSimPlugin plugin) {
        super(plugin, SEC, plugin.getConfigManager().getUpgradesGuiConfig());
    }

    @Override
    protected void setupInventory(Player player) {
        inventory.clear();
        FileConfiguration cfg = plugin.getConfigManager().getUpgradesGuiConfig();
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        applyFiller(SEC, cfg);
        inventory.setItem(slotFromConfig(SEC + ".back", cfg, 49), itemFromConfig(SEC + ".back", cfg));

        // Sand-cost upgrades
        buildUpgrade(player, data, cfg, "sand-multiplier",       UpgradeType.SAND_MULTIPLIER,       11);
        buildUpgrade(player, data, cfg, "sand-explosion-chance", UpgradeType.SAND_EXPLOSION_CHANCE,  13);
        buildUpgrade(player, data, cfg, "sand-explosion-radius", UpgradeType.SAND_EXPLOSION_RADIUS,  14);
        buildUpgrade(player, data, cfg, "sand-cooldown",         UpgradeType.SAND_COOLDOWN,          12);
        buildUpgrade(player, data, cfg, "gem-chance",            UpgradeType.GEM_CHANCE,             15);
        buildUpgrade(player, data, cfg, "efficiency",            UpgradeType.EFFICIENCY,             10);
        buildUpgrade(player, data, cfg, "gem-multiplier",        UpgradeType.GEM_MULTIPLIER,         16);
        buildUpgrade(player, data, cfg, "speed",                 UpgradeType.SPEED,                  22);

        // Sandbucks-cost upgrades
        buildUpgrade(player, data, cfg, "sand-jackpot",          UpgradeType.SAND_JACKPOT,           20);
        buildUpgrade(player, data, cfg, "gem-jackpot",           UpgradeType.GEM_JACKPOT,            24);

        applyPlaceholderItems(SEC, cfg);
    }

    private void buildUpgrade(Player player, PlayerData data, FileConfiguration cfg,
                              String key, UpgradeType type, int defaultSlot) {
        String path = SEC + "." + key;
        int        currentLevel = data.getUpgradeLevel(type);
        int        maxLevel     = plugin.getUpgradeManager().getMaxLevel(type);
        BigDecimal cost         = plugin.getUpgradeManager().getUpgradeCost(type, currentLevel);
        Currency   currency     = plugin.getUpgradeManager().getUpgradeCurrency(type);
        boolean    canAfford    = switch (currency) {
            case SAND      -> data.getSand().compareTo(cost) >= 0;
            case SANDBUCKS -> data.getSandbucks().compareTo(cost) >= 0;
            case GEMS      -> data.getGems().compareTo(cost) >= 0;
        };
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
        inventory.setItem(slotFromConfig(path, cfg, defaultSlot), createItem(mat, name, lore));
    }

    private String formatValue(UpgradeType type, int level) {
        if (type == UpgradeType.EFFICIENCY) {
            int v = (int) plugin.getUpgradeManager().getUpgradeValue(type, level);
            return v == 0 ? "None" : String.valueOf(v);
        }
        if (type == UpgradeType.SPEED) {
            return switch (level) {
                case 0 -> "None";
                case 1 -> "I";
                case 2 -> "II";
                default -> String.valueOf(level);
            };
        }
        // Jackpot — show as "0.001%" style
        if (type == UpgradeType.SAND_JACKPOT || type == UpgradeType.GEM_JACKPOT) {
            double val = plugin.getUpgradeManager().getUpgradeValue(type, level);
            return String.format("%.3f%%", val);
        }
        double val = plugin.getUpgradeManager().getUpgradeValue(type, level);
        if (val == Math.floor(val) && !Double.isInfinite(val)) return String.valueOf((int) val);
        return String.valueOf(val);
    }

    @Override
    public void handleClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        FileConfiguration cfg = plugin.getConfigManager().getUpgradesGuiConfig();
        if (slot == slotFromConfig(SEC + ".back", cfg, 49)) { new MenuGUI(plugin).open(player); return; }

        UpgradeType type = resolveUpgradeType(slot, cfg);
        if (type == null) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        if (plugin.getUpgradeManager().purchaseUpgrade(data, type)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            plugin.getMessageManager().sendMessage(player, "messages.upgrade-purchased");
            if (type == UpgradeType.EFFICIENCY) {
                plugin.getShovelManager().refreshShovel(player);
            }
            if (type == UpgradeType.SPEED) {
                applySpeedEffect(player);
            }
            setupInventory(player);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            // Pick the right "cannot afford" message based on currency
            Currency currency = plugin.getUpgradeManager().getUpgradeCurrency(type);
            String msgKey = switch (currency) {
                case SANDBUCKS -> "messages.cannot-afford-upgrade-sandbucks";
                default        -> "messages.cannot-afford-upgrade";
            };
            plugin.getMessageManager().sendMessage(player, msgKey);
        }
    }

    private void applySpeedEffect(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int amplifier = data.getUpgradeLevel(PlayerData.UpgradeType.SPEED) - 1;
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED,
                Integer.MAX_VALUE,
                amplifier,
                false,
                false,
                false
        ));
    }

    private UpgradeType resolveUpgradeType(int slot, FileConfiguration cfg) {
        if (slot == slotFromConfig(SEC + ".sand-multiplier",       cfg, 11)) return UpgradeType.SAND_MULTIPLIER;
        if (slot == slotFromConfig(SEC + ".sand-explosion-chance", cfg, 13)) return UpgradeType.SAND_EXPLOSION_CHANCE;
        if (slot == slotFromConfig(SEC + ".sand-explosion-radius", cfg, 14)) return UpgradeType.SAND_EXPLOSION_RADIUS;
        if (slot == slotFromConfig(SEC + ".sand-cooldown",         cfg, 12)) return UpgradeType.SAND_COOLDOWN;
        if (slot == slotFromConfig(SEC + ".gem-chance",            cfg, 15)) return UpgradeType.GEM_CHANCE;
        if (slot == slotFromConfig(SEC + ".efficiency",            cfg, 10)) return UpgradeType.EFFICIENCY;
        if (slot == slotFromConfig(SEC + ".gem-multiplier",        cfg, 16)) return UpgradeType.GEM_MULTIPLIER;
        if (slot == slotFromConfig(SEC + ".speed",                 cfg, 22)) return UpgradeType.SPEED;
        if (slot == slotFromConfig(SEC + ".sand-jackpot",          cfg, 20)) return UpgradeType.SAND_JACKPOT;
        if (slot == slotFromConfig(SEC + ".gem-jackpot",           cfg, 24)) return UpgradeType.GEM_JACKPOT;
        return null;
    }
}