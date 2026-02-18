package com.pallux.sandsim.data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private BigDecimal sand;
    private BigDecimal gems;
    private BigDecimal sandbucks;
    private int rebirths;

    // Leveling
    private int level;
    private long xp;

    // Upgrades
    private int sandMultiplier;
    private int sandExplosionChance;
    private int sandExplosionRadius;
    private int sandCooldown;
    private int gemChance;
    private int gemMultiplier;
    private int efficiency;

    // Factory upgrades
    private boolean factoryUnlocked;
    private int factoryProductionSpeed;
    private int factoryProductionAmount;
    private long lastFactoryProduction;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.sand = BigDecimal.ZERO;
        this.gems = BigDecimal.ZERO;
        this.sandbucks = BigDecimal.ZERO;
        this.rebirths = 0;

        this.level = 1;
        this.xp = 0;

        this.sandMultiplier = 0;
        this.sandExplosionChance = 0;
        this.sandExplosionRadius = 0;
        this.sandCooldown = 0;
        this.gemChance = 0;
        this.gemMultiplier = 0;
        this.efficiency = 0;

        this.factoryUnlocked = false;
        this.factoryProductionSpeed = 0;
        this.factoryProductionAmount = 0;
        this.lastFactoryProduction = System.currentTimeMillis();
    }

    // ---- Currency methods ----

    public void addSand(BigDecimal amount) {
        this.sand = this.sand.add(amount);
    }

    public void removeSand(BigDecimal amount) {
        this.sand = this.sand.subtract(amount);
        if (this.sand.compareTo(BigDecimal.ZERO) < 0) this.sand = BigDecimal.ZERO;
    }

    public void addGems(BigDecimal amount) {
        this.gems = this.gems.add(amount);
    }

    public void removeGems(BigDecimal amount) {
        this.gems = this.gems.subtract(amount);
        if (this.gems.compareTo(BigDecimal.ZERO) < 0) this.gems = BigDecimal.ZERO;
    }

    public void addSandbucks(BigDecimal amount) {
        this.sandbucks = this.sandbucks.add(amount);
    }

    public void removeSandbucks(BigDecimal amount) {
        this.sandbucks = this.sandbucks.subtract(amount);
        if (this.sandbucks.compareTo(BigDecimal.ZERO) < 0) this.sandbucks = BigDecimal.ZERO;
    }

    public void addRebirths(int amount) {
        this.rebirths += amount;
    }

    // ---- Leveling methods ----

    /**
     * Returns how much XP is needed to reach the next level from the given level.
     * Formula: level * 10
     */
    public long getXpForNextLevel() {
        return (long) level * 10L;
    }

    /**
     * Add XP and handle level-ups. Returns how many levels were gained.
     */
    public int addXp(long amount) {
        this.xp += amount;
        int levelsGained = 0;
        while (this.xp >= getXpForNextLevel()) {
            this.xp -= getXpForNextLevel();
            this.level++;
            levelsGained++;
        }
        return levelsGained;
    }

    /**
     * Returns the percentage of XP progress to the next level (0-100).
     */
    public int getXpPercent() {
        long needed = getXpForNextLevel();
        if (needed <= 0) return 100;
        return (int) ((xp * 100L) / needed);
    }

    // ---- Upgrade methods ----

    public void upgradeLevel(UpgradeType type, int levels) {
        switch (type) {
            case SAND_MULTIPLIER        -> this.sandMultiplier        += levels;
            case SAND_EXPLOSION_CHANCE  -> this.sandExplosionChance   += levels;
            case SAND_EXPLOSION_RADIUS  -> this.sandExplosionRadius   += levels;
            case SAND_COOLDOWN          -> this.sandCooldown          += levels;
            case GEM_CHANCE             -> this.gemChance             += levels;
            case GEM_MULTIPLIER         -> this.gemMultiplier         += levels;
            case EFFICIENCY             -> this.efficiency            += levels;
            case FACTORY_PRODUCTION_SPEED  -> this.factoryProductionSpeed  += levels;
            case FACTORY_PRODUCTION_AMOUNT -> this.factoryProductionAmount += levels;
        }
    }

    public int getUpgradeLevel(UpgradeType type) {
        return switch (type) {
            case SAND_MULTIPLIER           -> sandMultiplier;
            case SAND_EXPLOSION_CHANCE     -> sandExplosionChance;
            case SAND_EXPLOSION_RADIUS     -> sandExplosionRadius;
            case SAND_COOLDOWN             -> sandCooldown;
            case GEM_CHANCE                -> gemChance;
            case GEM_MULTIPLIER            -> gemMultiplier;
            case EFFICIENCY                -> efficiency;
            case FACTORY_PRODUCTION_SPEED  -> factoryProductionSpeed;
            case FACTORY_PRODUCTION_AMOUNT -> factoryProductionAmount;
        };
    }

    public void setUpgradeLevel(UpgradeType type, int level) {
        switch (type) {
            case SAND_MULTIPLIER        -> this.sandMultiplier        = level;
            case SAND_EXPLOSION_CHANCE  -> this.sandExplosionChance   = level;
            case SAND_EXPLOSION_RADIUS  -> this.sandExplosionRadius   = level;
            case SAND_COOLDOWN          -> this.sandCooldown          = level;
            case GEM_CHANCE             -> this.gemChance             = level;
            case GEM_MULTIPLIER         -> this.gemMultiplier         = level;
            case EFFICIENCY             -> this.efficiency            = level;
            case FACTORY_PRODUCTION_SPEED  -> this.factoryProductionSpeed  = level;
            case FACTORY_PRODUCTION_AMOUNT -> this.factoryProductionAmount = level;
        }
    }

    public void resetUpgrades() {
        this.sandMultiplier       = 0;
        this.sandExplosionChance  = 0;
        this.sandExplosionRadius  = 0;
        this.sandCooldown         = 0;
        this.gemChance            = 0;
        this.gemMultiplier        = 0;
        this.efficiency           = 0;
    }

    public void resetAll() {
        this.sand       = BigDecimal.ZERO;
        this.gems       = BigDecimal.ZERO;
        this.sandbucks  = BigDecimal.ZERO;
        this.rebirths   = 0;
        this.level      = 1;
        this.xp         = 0;
        resetUpgrades();
        this.factoryUnlocked          = false;
        this.factoryProductionSpeed   = 0;
        this.factoryProductionAmount  = 0;
    }

    // ---- Serialization ----

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("uuid",                    uuid.toString());
        data.put("sand",                    sand.toString());
        data.put("gems",                    gems.toString());
        data.put("sandbucks",               sandbucks.toString());
        data.put("rebirths",                rebirths);
        data.put("level",                   level);
        data.put("xp",                      xp);
        data.put("sandMultiplier",          sandMultiplier);
        data.put("sandExplosionChance",     sandExplosionChance);
        data.put("sandExplosionRadius",     sandExplosionRadius);
        data.put("sandCooldown",            sandCooldown);
        data.put("gemChance",               gemChance);
        data.put("gemMultiplier",           gemMultiplier);
        data.put("efficiency",              efficiency);
        data.put("factoryUnlocked",         factoryUnlocked);
        data.put("factoryProductionSpeed",  factoryProductionSpeed);
        data.put("factoryProductionAmount", factoryProductionAmount);
        data.put("lastFactoryProduction",   lastFactoryProduction);
        return data;
    }

    public static PlayerData deserialize(Map<String, Object> data) {
        UUID uuid = UUID.fromString((String) data.get("uuid"));
        PlayerData pd = new PlayerData(uuid);

        pd.sand                   = new BigDecimal((String) data.getOrDefault("sand",       "0"));
        pd.gems                   = new BigDecimal((String) data.getOrDefault("gems",       "0"));
        pd.sandbucks              = new BigDecimal((String) data.getOrDefault("sandbucks",  "0"));
        pd.rebirths               = (int)  data.getOrDefault("rebirths",               0);
        pd.level                  = (int)  data.getOrDefault("level",                  1);

        // xp may be stored as Integer or Long depending on YAML loading
        Object xpObj = data.getOrDefault("xp", 0L);
        if (xpObj instanceof Integer i) {
            pd.xp = i.longValue();
        } else if (xpObj instanceof Long l) {
            pd.xp = l;
        } else {
            pd.xp = 0L;
        }

        pd.sandMultiplier         = (int)  data.getOrDefault("sandMultiplier",         0);
        pd.sandExplosionChance    = (int)  data.getOrDefault("sandExplosionChance",    0);
        pd.sandExplosionRadius    = (int)  data.getOrDefault("sandExplosionRadius",    0);
        pd.sandCooldown           = (int)  data.getOrDefault("sandCooldown",           0);
        pd.gemChance              = (int)  data.getOrDefault("gemChance",              0);
        pd.gemMultiplier          = (int)  data.getOrDefault("gemMultiplier",          0);
        pd.efficiency             = (int)  data.getOrDefault("efficiency",             0);
        pd.factoryUnlocked        = (boolean) data.getOrDefault("factoryUnlocked",    false);
        pd.factoryProductionSpeed = (int)  data.getOrDefault("factoryProductionSpeed",  0);
        pd.factoryProductionAmount= (int)  data.getOrDefault("factoryProductionAmount", 0);
        pd.lastFactoryProduction  = (long) data.getOrDefault("lastFactoryProduction",   System.currentTimeMillis());

        return pd;
    }

    // ---- Getters / Setters ----

    public UUID getUuid()                         { return uuid; }
    public BigDecimal getSand()                   { return sand; }
    public void setSand(BigDecimal sand)          { this.sand = sand; }
    public BigDecimal getGems()                   { return gems; }
    public void setGems(BigDecimal gems)          { this.gems = gems; }
    public BigDecimal getSandbucks()              { return sandbucks; }
    public void setSandbucks(BigDecimal sb)       { this.sandbucks = sb; }
    public int getRebirths()                      { return rebirths; }
    public void setRebirths(int rebirths)         { this.rebirths = rebirths; }
    public boolean isFactoryUnlocked()            { return factoryUnlocked; }
    public void setFactoryUnlocked(boolean v)     { this.factoryUnlocked = v; }
    public long getLastFactoryProduction()        { return lastFactoryProduction; }
    public void setLastFactoryProduction(long v)  { this.lastFactoryProduction = v; }
    public int getLevel()                         { return level; }
    public void setLevel(int level)               { this.level = level; }
    public long getXp()                           { return xp; }
    public void setXp(long xp)                    { this.xp = xp; }

    // ---- Upgrade type enum ----

    public enum UpgradeType {
        SAND_MULTIPLIER,
        SAND_EXPLOSION_CHANCE,
        SAND_EXPLOSION_RADIUS,
        SAND_COOLDOWN,
        GEM_CHANCE,
        GEM_MULTIPLIER,
        EFFICIENCY,
        FACTORY_PRODUCTION_SPEED,
        FACTORY_PRODUCTION_AMOUNT
    }
}
