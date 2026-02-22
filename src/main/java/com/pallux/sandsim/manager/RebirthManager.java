package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    /**
     * Returns how many rebirths the player can currently afford.
     * Uses BigDecimal throughout to support arbitrarily large sand amounts.
     */
    public long getMaxRebirths(PlayerData data) {
        if (data.getSand().compareTo(rebirthCost) < 0) return 0L;
        BigDecimal result = data.getSand().divide(rebirthCost, 0, RoundingMode.DOWN);
        // Cap at Long.MAX_VALUE to avoid overflow when converting to long
        if (result.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            return Long.MAX_VALUE;
        }
        return result.longValue();
    }

    /**
     * Performs {@code amount} rebirths for the player.
     *
     * @param data   the player's data
     * @param amount number of rebirths to perform (must be > 0)
     * @return true if successful
     */
    public boolean performRebirth(PlayerData data, long amount) {
        if (amount <= 0) return false;
        BigDecimal totalCost = rebirthCost.multiply(BigDecimal.valueOf(amount));
        if (data.getSand().compareTo(totalCost) < 0) return false;
        data.removeSand(totalCost);
        data.resetUpgrades();
        data.addRebirths(amount);
        return true;
    }

    public double getRebirthMultiplier(long rebirths) {
        return 1.0 + (rebirths * multiplierPerRebirth);
    }

    public double getRebirthMultiplier(PlayerData data) {
        return getRebirthMultiplier(data.getRebirths());
    }

    public BigDecimal getRebirthCost()         { return rebirthCost; }
    public double getMultiplierPerRebirth()    { return multiplierPerRebirth; }
}