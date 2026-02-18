package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;

public class RebirthManager {

    private final SandSimPlugin plugin;
    private BigDecimal rebirthCost;
    private double multiplierPerRebirth;

    public RebirthManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        this.rebirthCost = new BigDecimal(config.getString("rebirth.cost", "1000"));
        this.multiplierPerRebirth = config.getDouble("rebirth.multiplier-per-rebirth", 0.01);
    }

    public boolean canRebirth(PlayerData data) {
        return data.getSand().compareTo(rebirthCost) >= 0;
    }

    public int getMaxRebirths(PlayerData data) {
        if (data.getSand().compareTo(rebirthCost) < 0) return 0;
        return data.getSand().divide(rebirthCost, 0, java.math.RoundingMode.DOWN).intValue();
    }

    public boolean performRebirth(PlayerData data, int amount) {
        if (amount <= 0) return false;
        BigDecimal totalCost = rebirthCost.multiply(BigDecimal.valueOf(amount));
        if (data.getSand().compareTo(totalCost) < 0) return false;
        data.removeSand(totalCost);
        data.resetUpgrades();
        data.addRebirths(amount);
        return true;
    }

    public double getRebirthMultiplier(int rebirths) {
        return 1.0 + (rebirths * multiplierPerRebirth);
    }

    public double getRebirthMultiplier(PlayerData data) {
        return getRebirthMultiplier(data.getRebirths());
    }

    public BigDecimal getRebirthCost()         { return rebirthCost; }
    public double getMultiplierPerRebirth()    { return multiplierPerRebirth; }
}
