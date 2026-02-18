package com.pallux.sandsim.expansion;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.utils.NumberFormatter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class SandSimExpansion extends PlaceholderExpansion {

    private final SandSimPlugin plugin;

    public SandSimExpansion(SandSimPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "sandsim"; }
    @Override public @NotNull String getAuthor()     { return plugin.getDescription().getAuthors().toString(); }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "sand"      -> NumberFormatter.format(data.getSand());
            case "gems"      -> NumberFormatter.format(data.getGems());
            case "sandbucks" -> NumberFormatter.format(data.getSandbucks());
            case "rebirths"  -> NumberFormatter.format(data.getRebirths());

            // Rebirth-only multiplier (kept for backwards compat)
            case "multiplier" -> {
                double mult = plugin.getRebirthManager().getRebirthMultiplier(data);
                yield NumberFormatter.format(BigDecimal.valueOf(mult)) + "x";
            }

            // ----------------------------------------------------------------
            // %sandsim_overall_multiplier%
            // Combines ALL sand multiplier sources:
            //   - Sand Upgrade multiplier  (e.g. level 50 = 51x)
            //   - Rebirth multiplier       (e.g. 90 rebirths = 1.9x)
            //   - Active Event sand bonus  (e.g. +50% = factor 1.5)
            // Formula: sandUpgrade * rebirthMult * (1 + eventBonus)
            // ----------------------------------------------------------------
            case "overall_multiplier" -> {
                double sandUpgrade    = plugin.getUpgradeManager()
                        .getSandMultiplier(data);               // base 1, +1 per upgrade level
                double rebirthMult    = plugin.getRebirthManager()
                        .getRebirthMultiplier(data);            // e.g. 1.9 for 90 rebirths
                double eventBonus     = plugin.getEventManager()
                        .getSandBonus();                        // 0.0 or 0.5 etc.
                double total          = sandUpgrade * rebirthMult * (1.0 + eventBonus);
                yield NumberFormatter.format(BigDecimal.valueOf(total)) + "x";
            }

            // Level & XP placeholders
            case "level"    -> String.valueOf(data.getLevel());
            case "level_xp" -> data.getXpPercent() + "%";

            // Raw values
            case "sand_raw"       -> data.getSand().toPlainString();
            case "gems_raw"       -> data.getGems().toPlainString();
            case "sandbucks_raw"  -> data.getSandbucks().toPlainString();
            case "rebirths_raw"   -> String.valueOf(data.getRebirths());
            case "multiplier_raw" -> String.format("%.4f",
                    plugin.getRebirthManager().getRebirthMultiplier(data));

            default -> null;
        };
    }
}
