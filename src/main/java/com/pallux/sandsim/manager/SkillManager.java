package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.data.SkillType;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages the Skill Tree system.
 *
 * Key rules:
 *  - 1 Skill Point earned per 5 levels (level 5 = 1pt, level 10 = 2pts, …).
 *  - Skills must be unlocked in order within each track (tier 1 before tier 2, etc.).
 *  - Skills and skill points are PERMANENT — they never reset on rebirth.
 *  - Each skill costs a configurable number of points (default 1 each).
 *  - Multipliers from skills are separate from upgrade/rebirth multipliers
 *    and are applied additively as a bonus percentage.
 */
public class SkillManager {

    private final SandSimPlugin plugin;

    public SkillManager(SandSimPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Config helpers ────────────────────────────────────────────────────────

    private FileConfiguration cfg() {
        return plugin.getConfigManager().getSkillsConfig();
    }

    /** Cost in skill points to purchase a given skill (default 1). */
    public int getSkillCost(SkillType skill) {
        return cfg().getInt("skills." + skill.getTrack() + ".tier-" + skill.getTier() + ".cost", 1);
    }

    /** Sand bonus percentage granted by a given sand skill (e.g. 100 = +100%). */
    public double getSkillSandBonus(SkillType skill) {
        return cfg().getDouble("skills.sand.tier-" + skill.getTier() + ".bonus", defaultSandBonus(skill));
    }

    /** Gems bonus percentage granted by a given gem skill. */
    public double getSkillGemsBonus(SkillType skill) {
        return cfg().getDouble("skills.gems.tier-" + skill.getTier() + ".bonus", defaultGemsBonus(skill));
    }

    /** Sandbucks bonus percentage granted by a given sandbucks skill. */
    public double getSkillSandbucksBonus(SkillType skill) {
        return cfg().getDouble("skills.sandbucks.tier-" + skill.getTier() + ".bonus", defaultSandbucksBonus(skill));
    }

    // ── Default bonuses (used when config key is absent) ─────────────────────

    private double defaultSandBonus(SkillType skill) {
        return switch (skill.getTier()) {
            case 1 -> 100.0;
            case 2 -> 150.0;
            case 3 -> 200.0;
            case 4 -> 250.0;
            default -> 0.0;
        };
    }

    private double defaultGemsBonus(SkillType skill) {
        return switch (skill.getTier()) {
            case 1 -> 50.0;
            case 2 -> 75.0;
            case 3 -> 100.0;
            case 4 -> 125.0;
            default -> 0.0;
        };
    }

    private double defaultSandbucksBonus(SkillType skill) {
        return switch (skill.getTier()) {
            case 1 -> 25.0;
            case 2 -> 50.0;
            case 3 -> 75.0;
            case 4 -> 100.0;
            default -> 0.0;
        };
    }

    // ── Prerequisite check ────────────────────────────────────────────────────

    /**
     * Returns true if the player has met the prerequisite for this skill
     * (i.e., the previous tier in the same track is already purchased, or this is tier 1).
     */
    public boolean hasPrerequisite(PlayerData data, SkillType skill) {
        SkillType prev = skill.getPrevious();
        return prev == null || data.hasSkill(prev);
    }

    // ── Purchase ──────────────────────────────────────────────────────────────

    /**
     * Attempts to purchase the given skill for the player.
     *
     * @return true if purchase succeeded.
     */
    public boolean purchaseSkill(PlayerData data, SkillType skill) {
        if (data.hasSkill(skill))              return false; // already owned
        if (!hasPrerequisite(data, skill))     return false; // locked
        int cost = getSkillCost(skill);
        if (data.getAvailableSkillPoints() < cost) return false; // can't afford
        data.purchaseSkill(skill, cost);
        return true;
    }

    // ── Multiplier queries ────────────────────────────────────────────────────

    /**
     * Total sand bonus from all purchased sand skills as a decimal multiplier.
     * e.g. if tier 1 (+100%) and tier 2 (+150%) are purchased → returns 1 + (100+150)/100 = 3.5
     * The bonuses are ADDITIVE (each tier adds its own bonus to the base).
     */
    public double getSandMultiplier(PlayerData data) {
        double bonus = 0.0;
        for (SkillType skill : SkillType.values()) {
            if (!skill.getTrack().equals("sand")) continue;
            if (data.hasSkill(skill)) bonus += getSkillSandBonus(skill);
        }
        return 1.0 + (bonus / 100.0);
    }

    /**
     * Total gems bonus from all purchased gem skills as a decimal multiplier.
     */
    public double getGemsMultiplier(PlayerData data) {
        double bonus = 0.0;
        for (SkillType skill : SkillType.values()) {
            if (!skill.getTrack().equals("gems")) continue;
            if (data.hasSkill(skill)) bonus += getSkillGemsBonus(skill);
        }
        return 1.0 + (bonus / 100.0);
    }

    /**
     * Total sandbucks bonus from all purchased sandbucks skills as a decimal multiplier.
     */
    public double getSandbucksMultiplier(PlayerData data) {
        double bonus = 0.0;
        for (SkillType skill : SkillType.values()) {
            if (!skill.getTrack().equals("sandbucks")) continue;
            if (data.hasSkill(skill)) bonus += getSkillSandbucksBonus(skill);
        }
        return 1.0 + (bonus / 100.0);
    }

    // ── Skill point sync ──────────────────────────────────────────────────────

    /**
     * Called whenever a player gains levels, to ensure their earned skill points
     * are up to date. Returns the number of new points awarded.
     */
    public int syncSkillPoints(PlayerData data) {
        int before = data.getSkillPointsEarned();
        data.recalculateSkillPoints();
        return Math.max(0, data.getSkillPointsEarned() - before);
    }

    // ── Lore/display helpers ──────────────────────────────────────────────────

    /** Human-readable bonus string for a sand skill tier. */
    public String getSandBonusDisplay(SkillType skill) {
        double bonus = getSkillSandBonus(skill);
        return formatBonus(bonus);
    }

    /** Human-readable bonus string for a gem skill tier. */
    public String getGemsBonusDisplay(SkillType skill) {
        double bonus = getSkillGemsBonus(skill);
        return formatBonus(bonus);
    }

    /** Human-readable bonus string for a sandbucks skill tier. */
    public String getSandbucksBonusDisplay(SkillType skill) {
        double bonus = getSkillSandbucksBonus(skill);
        return formatBonus(bonus);
    }

    private String formatBonus(double pct) {
        if (pct == Math.floor(pct)) return "+" + (int) pct + "%";
        return "+" + pct + "%";
    }
}