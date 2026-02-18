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

    public FactoryManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        this.factoryUnlockCost = new BigDecimal(config.getString("factory.unlock-cost", "10000"));
    }

    public boolean canUnlockFactory(PlayerData data) {
        return !data.isFactoryUnlocked() && data.getSand().compareTo(factoryUnlockCost) >= 0;
    }

    public boolean unlockFactory(PlayerData data) {
        if (!canUnlockFactory(data)) return false;
        data.removeSand(factoryUnlockCost);
        data.setFactoryUnlocked(true);
        data.setLastFactoryProduction(System.currentTimeMillis());
        return true;
    }

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

        // Base production speed, then apply event speed bonus
        double productionSpeed = plugin.getUpgradeManager().getFactoryProductionSpeed(data);
        double speedBonus      = plugin.getEventManager().getFactorySpeedBonus(); // e.g. 0.5 = 50% faster
        // Faster = shorter interval. A 50% speed bonus = interval * (1 / 1.5)
        double effectiveSpeed  = productionSpeed / (1.0 + speedBonus);
        if (effectiveSpeed < 0.05) effectiveSpeed = 0.05; // hard minimum 50ms

        long productionInterval = (long) (effectiveSpeed * 1000);
        long timePassed   = currentTime - lastProduction;
        int  cycles       = (int) (timePassed / productionInterval);

        if (cycles > 0) {
            double baseAmount  = plugin.getUpgradeManager().getFactoryProductionAmount(data);
            double productionBonus = plugin.getEventManager().getFactoryProductionBonus(); // e.g. 0.5
            double totalAmount = baseAmount * (1.0 + productionBonus);

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

    public BigDecimal getFactoryUnlockCost() { return factoryUnlockCost; }

    public long getTimeUntilNextProduction(PlayerData data) {
        if (!data.isFactoryUnlocked()) return -1;

        long   currentTime       = System.currentTimeMillis();
        long   lastProduction    = data.getLastFactoryProduction();
        double productionSpeed   = plugin.getUpgradeManager().getFactoryProductionSpeed(data);
        double speedBonus        = plugin.getEventManager().getFactorySpeedBonus();
        double effectiveSpeed    = productionSpeed / (1.0 + speedBonus);
        long   productionInterval = (long) (effectiveSpeed * 1000);

        long timePassed   = currentTime - lastProduction;
        long timeRemaining = productionInterval - (timePassed % productionInterval);
        return timeRemaining;
    }
}
