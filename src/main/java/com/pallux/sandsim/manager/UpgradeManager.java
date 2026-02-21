package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.data.PlayerData.UpgradeType;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class UpgradeManager {

    private final SandSimPlugin plugin;
    private final Map<UpgradeType, UpgradeInfo> upgradeInfoMap;

    public UpgradeManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        this.upgradeInfoMap = new HashMap<>();
        loadUpgradeInfo();
    }

    private void loadUpgradeInfo() {
        // Config now lives in gui_menus/upgrades-gui.yml, accessed via getUpgradesConfig()
        FileConfiguration config = plugin.getConfigManager().getUpgradesConfig();

        upgradeInfoMap.put(UpgradeType.SAND_MULTIPLIER, new UpgradeInfo(
                1, 100, 1,
                config.getDouble("upgrades.sand-multiplier.base-cost", 100),
                config.getDouble("upgrades.sand-multiplier.cost-multiplier", 1.15)));

        upgradeInfoMap.put(UpgradeType.SAND_EXPLOSION_CHANCE, new UpgradeInfo(
                0, 100, 0.01,
                config.getDouble("upgrades.sand-explosion-chance.base-cost", 500),
                config.getDouble("upgrades.sand-explosion-chance.cost-multiplier", 1.2)));

        upgradeInfoMap.put(UpgradeType.SAND_EXPLOSION_RADIUS, new UpgradeInfo(
                1, 10, 1,
                config.getDouble("upgrades.sand-explosion-radius.base-cost", 1000),
                config.getDouble("upgrades.sand-explosion-radius.cost-multiplier", 1.25)));

        upgradeInfoMap.put(UpgradeType.SAND_COOLDOWN, new UpgradeInfo(
                5.0, 0.1, -0.01,
                config.getDouble("upgrades.sand-cooldown.base-cost", 250),
                config.getDouble("upgrades.sand-cooldown.cost-multiplier", 1.18)));

        upgradeInfoMap.put(UpgradeType.GEM_CHANCE, new UpgradeInfo(
                0, 50, 0.01,
                config.getDouble("upgrades.gem-chance.base-cost", 750),
                config.getDouble("upgrades.gem-chance.cost-multiplier", 1.22)));

        upgradeInfoMap.put(UpgradeType.GEM_MULTIPLIER, new UpgradeInfo(
                1, 10, 1,
                config.getDouble("upgrades.gem-multiplier.base-cost", 2000),
                config.getDouble("upgrades.gem-multiplier.cost-multiplier", 1.3)));

        upgradeInfoMap.put(UpgradeType.EFFICIENCY, new UpgradeInfo(
                0, 5, 1,
                config.getDouble("upgrades.efficiency.base-cost", 500),
                config.getDouble("upgrades.efficiency.cost-multiplier", 3.0)));

        // Speed: max 2 levels. Level 1 costs 50,000; Level 2 costs 250,000.
        // cost-multiplier of 5.0 gives: 50000 * 5^0 = 50000, 50000 * 5^1 = 250000
        upgradeInfoMap.put(UpgradeType.SPEED, new UpgradeInfo(
                0, 2, 1,
                config.getDouble("upgrades.speed.base-cost", 50000),
                config.getDouble("upgrades.speed.cost-multiplier", 5.0)));

        upgradeInfoMap.put(UpgradeType.FACTORY_PRODUCTION_SPEED, new UpgradeInfo(
                5.0, 1.0, -0.01,
                config.getDouble("upgrades.factory-production-speed.base-cost", 100),
                config.getDouble("upgrades.factory-production-speed.cost-multiplier", 1.15)));

        upgradeInfoMap.put(UpgradeType.FACTORY_PRODUCTION_AMOUNT, new UpgradeInfo(
                1, 1000, 1,
                config.getDouble("upgrades.factory-production-amount.base-cost", 150),
                config.getDouble("upgrades.factory-production-amount.cost-multiplier", 1.12)));
    }

    public UpgradeInfo getUpgradeInfo(UpgradeType type) { return upgradeInfoMap.get(type); }

    public double getUpgradeValue(UpgradeType type, int level) {
        UpgradeInfo info = upgradeInfoMap.get(type);
        if (info == null) return 0;
        return info.baseValue + (level * info.valuePerLevel);
    }

    public BigDecimal getUpgradeCost(UpgradeType type, int currentLevel) {
        UpgradeInfo info = upgradeInfoMap.get(type);
        if (info == null) return BigDecimal.ZERO;
        double cost = info.baseCost * Math.pow(info.costMultiplier, currentLevel);
        return BigDecimal.valueOf(cost).setScale(0, java.math.RoundingMode.CEILING);
    }

    public boolean canUpgrade(PlayerData data, UpgradeType type) {
        int currentLevel = data.getUpgradeLevel(type);
        if (currentLevel >= getMaxLevel(type)) return false;
        return data.getSand().compareTo(getUpgradeCost(type, currentLevel)) >= 0;
    }

    public boolean purchaseUpgrade(PlayerData data, UpgradeType type) {
        if (!canUpgrade(data, type)) return false;
        BigDecimal cost = getUpgradeCost(type, data.getUpgradeLevel(type));
        data.removeSand(cost);
        data.upgradeLevel(type, 1);
        return true;
    }

    public int getMaxLevel(UpgradeType type) {
        return switch (type) {
            case SAND_MULTIPLIER           -> 100;
            case SAND_EXPLOSION_CHANCE     -> 100;
            case SAND_EXPLOSION_RADIUS     -> 10;
            case SAND_COOLDOWN             -> 490;
            case GEM_CHANCE                -> 50;
            case GEM_MULTIPLIER            -> 10;
            case EFFICIENCY                -> 5;
            case SPEED                     -> 2;
            case FACTORY_PRODUCTION_SPEED  -> 400;
            case FACTORY_PRODUCTION_AMOUNT -> 1000;
        };
    }

    public double getSandMultiplier(PlayerData data) {
        return getUpgradeValue(UpgradeType.SAND_MULTIPLIER, data.getUpgradeLevel(UpgradeType.SAND_MULTIPLIER));
    }
    public double getSandExplosionChance(PlayerData data) {
        return getUpgradeValue(UpgradeType.SAND_EXPLOSION_CHANCE, data.getUpgradeLevel(UpgradeType.SAND_EXPLOSION_CHANCE));
    }
    public int getSandExplosionRadius(PlayerData data) {
        return (int) getUpgradeValue(UpgradeType.SAND_EXPLOSION_RADIUS, data.getUpgradeLevel(UpgradeType.SAND_EXPLOSION_RADIUS));
    }
    public double getSandCooldown(PlayerData data) {
        return getUpgradeValue(UpgradeType.SAND_COOLDOWN, data.getUpgradeLevel(UpgradeType.SAND_COOLDOWN));
    }
    public double getGemChance(PlayerData data) {
        return getUpgradeValue(UpgradeType.GEM_CHANCE, data.getUpgradeLevel(UpgradeType.GEM_CHANCE));
    }
    public double getGemMultiplier(PlayerData data) {
        return getUpgradeValue(UpgradeType.GEM_MULTIPLIER, data.getUpgradeLevel(UpgradeType.GEM_MULTIPLIER));
    }
    public int getEfficiencyLevel(PlayerData data) {
        return data.getUpgradeLevel(UpgradeType.EFFICIENCY);
    }
    /** Returns true if the player has purchased the Speed upgrade. */
    public boolean hasSpeedUpgrade(PlayerData data) {
        return data.getUpgradeLevel(UpgradeType.SPEED) >= 1;
    }
    public double getFactoryProductionSpeed(PlayerData data) {
        return getUpgradeValue(UpgradeType.FACTORY_PRODUCTION_SPEED, data.getUpgradeLevel(UpgradeType.FACTORY_PRODUCTION_SPEED));
    }
    public double getFactoryProductionAmount(PlayerData data) {
        return getUpgradeValue(UpgradeType.FACTORY_PRODUCTION_AMOUNT, data.getUpgradeLevel(UpgradeType.FACTORY_PRODUCTION_AMOUNT));
    }

    public static class UpgradeInfo {
        public final double baseValue;
        public final double maxValue;
        public final double valuePerLevel;
        public final double baseCost;
        public final double costMultiplier;

        public UpgradeInfo(double baseValue, double maxValue, double valuePerLevel,
                           double baseCost, double costMultiplier) {
            this.baseValue      = baseValue;
            this.maxValue       = maxValue;
            this.valuePerLevel  = valuePerLevel;
            this.baseCost       = baseCost;
            this.costMultiplier = costMultiplier;
        }
    }
}
