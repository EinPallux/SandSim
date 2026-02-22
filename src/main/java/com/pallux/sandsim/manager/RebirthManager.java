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

    /**
     * We cap rebirths-per-operation at this value to avoid overflowing
     * {@code int} fields elsewhere (PlayerData stores rebirths as int).
     * Integer.MAX_VALUE is ~2.1 billion which is already an absurd number
     * of rebirths, so this is a safe ceiling.
     */
    private static final long MAX_REBIRTHS_PER_OP = Integer.MAX_VALUE;

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
     * Returns how many rebirths the player can currently afford, capped at
     * {@link Integer#MAX_VALUE} so the result is always safe to cast to int.
     */
    public long getMaxRebirths(PlayerData data) {
        if (data.getSand().compareTo(rebirthCost) < 0) return 0L;
        BigDecimal result = data.getSand().divide(rebirthCost, 0, RoundingMode.DOWN);
        // Cap to avoid overflowing int storage in PlayerData
        long value = result.min(BigDecimal.valueOf(MAX_REBIRTHS_PER_OP)).longValue();
        return Math.max(0L, value);
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
        data.addRebirths((int) Math.min(amount, MAX_REBIRTHS_PER_OP));
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