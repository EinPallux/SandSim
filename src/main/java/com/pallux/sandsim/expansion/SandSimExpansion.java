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

            case "multiplier" -> {
                double mult = plugin.getRebirthManager().getRebirthMultiplier(data);
                yield NumberFormatter.format(BigDecimal.valueOf(mult)) + "x";
            }

            case "overall_multiplier" -> {
                double sandUpgrade    = plugin.getUpgradeManager().getSandMultiplier(data);
                double rebirthMult    = plugin.getRebirthManager().getRebirthMultiplier(data);
                double eventBonus     = plugin.getEventManager().getSandBonus();
                double augmentMult    = plugin.getAugmentManager().getSandMultiplier(data);
                double skillSandMult  = plugin.getSkillManager().getSandMultiplier(data);
                double total          = sandUpgrade * rebirthMult * (1.0 + eventBonus)
                        * augmentMult * skillSandMult;
                yield NumberFormatter.format(BigDecimal.valueOf(total)) + "x";
            }

            case "overall_gems_multiplier" -> {
                double augmentMult   = plugin.getAugmentManager().getGemsMultiplier(data);
                double skillGemsMult = plugin.getSkillManager().getGemsMultiplier(data);
                double gemUpgrade    = plugin.getUpgradeManager().getGemMultiplier(data);
                double total         = gemUpgrade * augmentMult * skillGemsMult;
                yield NumberFormatter.format(BigDecimal.valueOf(total)) + "x";
            }

            case "overall_sandbucks_multiplier" -> {
                double augmentMult  = plugin.getAugmentManager().getSandbucksMultiplier(data);
                double skillSbMult  = plugin.getSkillManager().getSandbucksMultiplier(data);
                double total        = augmentMult * skillSbMult;
                yield NumberFormatter.format(BigDecimal.valueOf(total)) + "x";
            }

            case "skill_points"          -> String.valueOf(data.getAvailableSkillPoints());
            case "skill_points_earned"   -> String.valueOf(data.getSkillPointsEarned());
            case "skill_points_spent"    -> String.valueOf(data.getSkillPointsSpent());

            case "level"    -> String.valueOf(data.getLevel());
            case "level_xp" -> data.getXpPercent() + "%";

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