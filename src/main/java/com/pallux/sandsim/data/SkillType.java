package com.pallux.sandsim.data;

/**
 * Represents the three Skill Tree tracks and their tiers.
 * Each track has 4 tiers that must be unlocked in order.
 */
public enum SkillType {

    // ── Sand Track ────────────────────────────────────────────────────────────
    SAND_SKILL_1("sand", 1),
    SAND_SKILL_2("sand", 2),
    SAND_SKILL_3("sand", 3),
    SAND_SKILL_4("sand", 4),

    // ── Gems Track ────────────────────────────────────────────────────────────
    GEM_SKILL_1("gems", 1),
    GEM_SKILL_2("gems", 2),
    GEM_SKILL_3("gems", 3),
    GEM_SKILL_4("gems", 4),

    // ── Sandbucks Track ───────────────────────────────────────────────────────
    SANDBUCKS_SKILL_1("sandbucks", 1),
    SANDBUCKS_SKILL_2("sandbucks", 2),
    SANDBUCKS_SKILL_3("sandbucks", 3),
    SANDBUCKS_SKILL_4("sandbucks", 4);

    private final String track;
    private final int    tier;

    SkillType(String track, int tier) {
        this.track = track;
        this.tier  = tier;
    }

    public String getTrack() { return track; }
    public int    getTier()  { return tier; }

    /** Returns the previous skill in the same track, or null if this is tier 1. */
    public SkillType getPrevious() {
        if (tier == 1) return null;
        for (SkillType s : values()) {
            if (s.track.equals(track) && s.tier == tier - 1) return s;
        }
        return null;
    }
}