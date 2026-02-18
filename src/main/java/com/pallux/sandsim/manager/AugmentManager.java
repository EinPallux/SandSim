package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.AugmentDefinition;
import com.pallux.sandsim.data.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages the Augments system.
 *
 * All augment definitions are loaded from augments.yml so server admins
 * can freely adjust bonuses, costs, timers and add new tiers without
 * touching any Java code.
 *
 * Key rules (enforced here):
 *  - Augments do NOT reset on rebirth.
 *  - Multipliers REPLACE, not stack – only the highest unlocked tier applies.
 *  - Only one augment can be researched at a time.
 *  - Research has an upfront gem cost and a time delay before it activates.
 */
public class AugmentManager {

    private final SandSimPlugin plugin;

    /** All augment definitions ordered by tier (index 0 = tier 1). */
    private final List<AugmentDefinition> augments = new ArrayList<>();

    /** How many augment items fit on one GUI page (3 rows × 7 columns). */
    public static final int ITEMS_PER_PAGE = 21;

    public AugmentManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // ── Config loading ────────────────────────────────────────────────────────

    /**
     * (Re)loads all augment definitions from augments.yml.
     * Safe to call on /sandsim reload.
     */
    public void loadConfig() {
        augments.clear();
        FileConfiguration cfg = plugin.getConfigManager().getAugmentsConfig();

        List<?> rawList = cfg.getList("augments");
        if (rawList == null || rawList.isEmpty()) {
            plugin.getLogger().warning("[AugmentManager] augments.yml has no augment entries!");
            return;
        }

        for (Object entry : rawList) {
            if (!(entry instanceof Map<?, ?> map)) continue;

            try {
                int    tier         = toInt(map.get("tier"),                    augments.size() + 1);
                double sandPct      = toDouble(map.get("sand-percent"),          0.0);
                double gemsPct      = toDouble(map.get("gems-percent"),          0.0);
                double sbPct        = toDouble(map.get("sandbucks-percent"),     0.0);
                long   researchSecs = toLong(map.get("research-time-seconds"),   300L);
                long   gemCost      = toLong(map.get("gem-cost"),                10L);

                augments.add(new AugmentDefinition(tier, sandPct, gemsPct, sbPct, researchSecs, gemCost));
            } catch (Exception e) {
                plugin.getLogger().warning("[AugmentManager] Skipping malformed augment entry: " + e.getMessage());
            }
        }

        // Sort by tier so out-of-order YAML still works correctly
        augments.sort((a, b) -> Integer.compare(a.getTier(), b.getTier()));

        plugin.getLogger().info("[AugmentManager] Loaded " + augments.size() + " augment(s) from augments.yml.");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Unmodifiable list of all definitions, ordered by tier. */
    public List<AugmentDefinition> getAllAugments() {
        return Collections.unmodifiableList(augments);
    }

    /**
     * Returns the definition for a given 1-based position in the sorted list.
     * (Tier numbers in the YAML may not be contiguous; this accesses by position.)
     */
    public AugmentDefinition getAugment(int tier) {
        if (tier < 1 || tier > augments.size()) return null;
        return augments.get(tier - 1);
    }

    public int getTotalTiers() {
        return augments.size();
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    /** Highest tier the player has FULLY unlocked. 0 = none. */
    public int getUnlockedTier(PlayerData data) {
        return data.getAugmentUnlockedTier();
    }

    /** Tier currently being researched. 0 = not researching. */
    public int getResearchingTier(PlayerData data) {
        return data.getAugmentResearchingTier();
    }

    /** Epoch-millis when the current research will complete. */
    public long getResearchCompleteTime(PlayerData data) {
        return data.getAugmentResearchCompleteTime();
    }

    /**
     * Checks whether in-progress research has finished and, if so, promotes
     * the researching tier to the unlocked tier.
     * Must be called before any state read (GUI render, multiplier query, etc.).
     */
    public void tickResearch(PlayerData data) {
        if (data.getAugmentResearchingTier() <= 0) return;
        if (System.currentTimeMillis() >= data.getAugmentResearchCompleteTime()) {
            data.setAugmentUnlockedTier(data.getAugmentResearchingTier());
            data.setAugmentResearchingTier(0);
            data.setAugmentResearchCompleteTime(0L);
        }
    }

    /**
     * Attempts to start research on the next tier.
     *
     * Requirements:
     *  - Not already researching.
     *  - Next tier exists in the loaded list.
     *  - Player has enough gems.
     *
     * @return true if research was started successfully.
     */
    public boolean startResearch(PlayerData data) {
        tickResearch(data);

        if (data.getAugmentResearchingTier() > 0) return false; // already researching

        int nextTier = data.getAugmentUnlockedTier() + 1;
        AugmentDefinition def = getAugment(nextTier);
        if (def == null) return false; // no more tiers

        BigDecimal cost = BigDecimal.valueOf(def.getGemCost());
        if (data.getGems().compareTo(cost) < 0) return false; // can't afford

        data.removeGems(cost);
        data.setAugmentResearchingTier(nextTier);
        data.setAugmentResearchCompleteTime(
                System.currentTimeMillis() + def.getResearchTimeSeconds() * 1000L);
        return true;
    }

    /**
     * Returns the number of seconds remaining in the current research.
     * Returns 0 if not researching or already done.
     */
    public long getResearchSecondsRemaining(PlayerData data) {
        tickResearch(data);
        if (data.getAugmentResearchingTier() <= 0) return 0;
        long remaining = data.getAugmentResearchCompleteTime() - System.currentTimeMillis();
        return Math.max(0L, remaining / 1000L);
    }

    /** Human-readable duration string, e.g. "2h 15m 30s". */
    public String formatTime(long seconds) {
        if (seconds <= 0) return "0s";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.isEmpty()) sb.append(s).append("s");
        return sb.toString().trim();
    }

    // ── Multiplier queries ────────────────────────────────────────────────────

    /** Sand multiplier from augments (1.0 = no bonus, 1.05 = +5%). */
    public double getSandMultiplier(PlayerData data) {
        tickResearch(data);
        AugmentDefinition def = getAugment(data.getAugmentUnlockedTier());
        if (def == null) return 1.0;
        return 1.0 + (def.getSandPercent() / 100.0);
    }

    /** Gems multiplier from augments. */
    public double getGemsMultiplier(PlayerData data) {
        tickResearch(data);
        AugmentDefinition def = getAugment(data.getAugmentUnlockedTier());
        if (def == null) return 1.0;
        return 1.0 + (def.getGemsPercent() / 100.0);
    }

    /** Sandbucks multiplier from augments. */
    public double getSandbucksMultiplier(PlayerData data) {
        tickResearch(data);
        AugmentDefinition def = getAugment(data.getAugmentUnlockedTier());
        if (def == null) return 1.0;
        return 1.0 + (def.getSandbucksPercent() / 100.0);
    }

    // ── Type-safe YAML value helpers ──────────────────────────────────────────

    private static int toInt(Object val, int fallback) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) { try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {} }
        return fallback;
    }

    private static double toDouble(Object val, double fallback) {
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) { try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {} }
        return fallback;
    }

    private static long toLong(Object val, long fallback) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) { try { return Long.parseLong(s); } catch (NumberFormatException ignored) {} }
        return fallback;
    }
}