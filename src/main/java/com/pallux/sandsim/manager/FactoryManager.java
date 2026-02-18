package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.data.PlayerData.UpgradeType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public class FactoryManager {

    private final SandSimPlugin plugin;
    private BigDecimal factoryUnlockCost;
    private int factoryUnlockLevel;

    public FactoryManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        this.factoryUnlockCost  = new BigDecimal(config.getString("factory.unlock-cost",  "10000"));
        this.factoryUnlockLevel = config.getInt("factory.unlock-level", 50);
    }

    public boolean canUnlockFactory(PlayerData data) {
        if (data.isFactoryUnlocked()) return false;
        if (data.getLevel() < factoryUnlockLevel) return false;
        return data.getSand().compareTo(factoryUnlockCost) >= 0;
    }

    public boolean meetsLevelRequirement(PlayerData data) {
        return data.getLevel() >= factoryUnlockLevel;
    }

    public boolean unlockFactory(PlayerData data) {
        if (!canUnlockFactory(data)) return false;
        data.removeSand(factoryUnlockCost);
        data.setFactoryUnlocked(true);
        data.setLastFactoryProduction(System.currentTimeMillis());
        return true;
    }

    /**
     * Called every second. Only processes online players so offline players
     * never accumulate Sandbucks passively.
     */
    public void processFactoryProduction() {
        long currentTime = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getDataManager().getPlayerData(player);
            if (!data.isFactoryUnlocked()) continue;
            processPlayerFactory(data, currentTime);
        }
    }

    private void processPlayerFactory(PlayerData data, long currentTime) {
        long lastProduction = data.getLastFactoryProduction();

        double productionSpeed = plugin.getUpgradeManager().getFactoryProductionSpeed(data);
        double speedBonus      = plugin.getEventManager().getFactorySpeedBonus();
        double effectiveSpeed  = productionSpeed / (1.0 + speedBonus);
        if (effectiveSpeed < 0.05) effectiveSpeed = 0.05;

        long productionInterval = (long) (effectiveSpeed * 1000);
        long timePassed         = currentTime - lastProduction;
        int  cycles             = (int) (timePassed / productionInterval);

        if (cycles > 0) {
            double baseAmount          = plugin.getUpgradeManager().getFactoryProductionAmount(data);
            double productionBonus     = plugin.getEventManager().getFactoryProductionBonus();
            // Augment sandbucks multiplier (e.g. 1.01 = +1%)
            double augmentSandbucksMult = plugin.getAugmentManager().getSandbucksMultiplier(data);
            double totalAmount          = baseAmount * (1.0 + productionBonus) * augmentSandbucksMult;

            BigDecimal total = BigDecimal.valueOf(totalAmount * cycles);
            data.addSandbucks(total);
            data.setLastFactoryProduction(lastProduction + ((long) cycles * productionInterval));
        }
    }

    public boolean canUpgradeFactory(PlayerData data, UpgradeType type) {
        if (!data.isFactoryUnlocked()) return false;
        int currentLevel = data.getUpgradeLevel(type);
        if (currentLevel >= plugin.getUpgradeManager().getMaxLevel(type)) return false;
        BigDecimal cost = plugin.getUpgradeManager().getUpgradeCost(type, currentLevel);
        return data.getSandbucks().compareTo(cost) >= 0;
    }

    public boolean purchaseFactoryUpgrade(PlayerData data, UpgradeType type) {
        if (!canUpgradeFactory(data, type)) return false;
        BigDecimal cost = plugin.getUpgradeManager().getUpgradeCost(type, data.getUpgradeLevel(type));
        data.removeSandbucks(cost);
        data.upgradeLevel(type, 1);
        return true;
    }

    public BigDecimal getFactoryUnlockCost()  { return factoryUnlockCost; }
    public int        getFactoryUnlockLevel()  { return factoryUnlockLevel; }

    public long getTimeUntilNextProduction(PlayerData data) {
        if (!data.isFactoryUnlocked()) return -1;
        long   currentTime        = System.currentTimeMillis();
        long   lastProduction     = data.getLastFactoryProduction();
        double productionSpeed    = plugin.getUpgradeManager().getFactoryProductionSpeed(data);
        double speedBonus         = plugin.getEventManager().getFactorySpeedBonus();
        double effectiveSpeed     = productionSpeed / (1.0 + speedBonus);
        long   productionInterval = (long) (effectiveSpeed * 1000);
        long   timePassed         = currentTime - lastProduction;
        return productionInterval - (timePassed % productionInterval);
    }
}