package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SandBlockManager {

    private final SandSimPlugin plugin;
    private final Map<Location, Long> cooldowns;
    private Material sandMaterial;
    private Material redSandMaterial;
    private Material cooldownMaterial;
    private double redSandMultiplier;

    public SandBlockManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        String sandMat     = config.getString("sand-block.material",          "SAND");
        String redSandMat  = config.getString("sand-block.red-sand-material", "RED_SAND");
        String cooldownMat = config.getString("sand-block.cooldown-material", "BEDROCK");
        this.redSandMultiplier = config.getDouble("sand-block.red-sand-multiplier", 1.5);

        try { this.sandMaterial     = Material.valueOf(sandMat);    } catch (IllegalArgumentException e) { this.sandMaterial     = Material.SAND;     }
        try { this.redSandMaterial  = Material.valueOf(redSandMat); } catch (IllegalArgumentException e) { this.redSandMaterial  = Material.RED_SAND; }
        try { this.cooldownMaterial = Material.valueOf(cooldownMat); } catch (IllegalArgumentException e) { this.cooldownMaterial = Material.BEDROCK;  }
    }

    /** Returns true if the block is a mineable sand block (normal or red). */
    public boolean isSandBlock(Block block) {
        return block.getType() == sandMaterial || block.getType() == redSandMaterial;
    }

    /** Returns true if the block is specifically normal Sand. */
    public boolean isNormalSand(Block block) {
        return block.getType() == sandMaterial;
    }

    /** Returns true if the block is specifically Red Sand. */
    public boolean isRedSand(Block block) {
        return block.getType() == redSandMaterial;
    }

    /**
     * Returns the bonus multiplier for this specific block type.
     * Normal sand → 1.0, Red sand → redSandMultiplier (default 1.5).
     */
    public double getSandTypeMultiplier(Block block) {
        if (block.getType() == redSandMaterial) return redSandMultiplier;
        return 1.0;
    }

    public boolean isOnCooldown(Location location) {
        Long cooldownEnd = cooldowns.get(location);
        if (cooldownEnd == null) return false;
        if (System.currentTimeMillis() >= cooldownEnd) { cooldowns.remove(location); return false; }
        return true;
    }

    public void setCooldown(Location location, PlayerData data) {
        double cooldownSeconds = plugin.getUpgradeManager().getSandCooldown(data);
        long cooldownMillis    = (long) (cooldownSeconds * 1000);
        long cooldownEnd       = System.currentTimeMillis() + cooldownMillis;

        cooldowns.put(location.clone(), cooldownEnd);

        Block block = location.getBlock();
        // Remember which material to restore (sand or red sand)
        Material originalMaterial = block.getType();
        Bukkit.getScheduler().runTask(plugin, () -> block.setType(cooldownMaterial));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (cooldowns.remove(location.clone()) != null) block.setType(originalMaterial);
        }, (long) (cooldownSeconds * 20));
    }

    public long getRemainingCooldown(Location location) {
        Long cooldownEnd = cooldowns.get(location);
        if (cooldownEnd == null) return 0;
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }

    public Material getSandMaterial()        { return sandMaterial; }
    public Material getRedSandMaterial()     { return redSandMaterial; }
    public Material getCooldownMaterial()    { return cooldownMaterial; }
    public double   getRedSandMultiplier()   { return redSandMultiplier; }
}