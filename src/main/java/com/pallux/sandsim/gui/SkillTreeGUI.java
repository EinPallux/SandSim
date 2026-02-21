package com.pallux.sandsim.gui;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.data.SkillType;
import com.pallux.sandsim.manager.SkillManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill Tree GUI — 54-slot inventory.
 *
 * Layout (columns 0-8, rows 0-5):
 *
 * Row 0: filler | filler | SAND_HEADER(col2) | filler | GEM_HEADER(col4) | filler | SB_HEADER(col6) | filler | filler
 * Row 1: filler | filler | SAND_SKILL_1      | filler | GEM_SKILL_1      | filler | SB_SKILL_1      | filler | filler
 * Row 2: filler | filler | SAND_SKILL_2      | filler | GEM_SKILL_2      | filler | SB_SKILL_2      | filler | filler
 * Row 3: filler | filler | SAND_SKILL_3      | filler | GEM_SKILL_3      | filler | SB_SKILL_3      | filler | filler
 * Row 4: filler | filler | SAND_SKILL_4      | filler | GEM_SKILL_4      | filler | SB_SKILL_4      | filler | filler
 * Row 5: BACK(45) | filler … filler | INFO(49) | filler … filler
 *
 * Column mapping:
 *   Col 2 (slots 2,11,20,29,38) → Sand track
 *   Col 4 (slots 4,13,22,31,40) → Gems track
 *   Col 6 (slots 6,15,24,33,42) → Sandbucks track
 */
public class SkillTreeGUI extends BaseGUI {

    private static final String SEC = "skilltree";

    // ── Header slots (row 0) ──────────────────────────────────────────────────
    private static final int SLOT_SAND_HEADER = 2;
    private static final int SLOT_GEM_HEADER  = 4;
    private static final int SLOT_SB_HEADER   = 6;

    // ── Skill slots: sand track (col 2, rows 1-4) ─────────────────────────────
    private static final int[] SAND_SLOTS = { 11, 20, 29, 38 };

    // ── Skill slots: gems track (col 4, rows 1-4) ─────────────────────────────
    private static final int[] GEM_SLOTS  = { 13, 22, 31, 40 };

    // ── Skill slots: sandbucks track (col 6, rows 1-4) ───────────────────────
    private static final int[] SB_SLOTS   = { 15, 24, 33, 42 };

    // ── Navigation slots (row 5) ──────────────────────────────────────────────
    private static final int SLOT_BACK = 45;
    private static final int SLOT_INFO = 49;

    public SkillTreeGUI(SandSimPlugin plugin) {
        super(plugin, SEC);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @Override
    protected void setupInventory(Player player) {
        inventory.clear();
        FileConfiguration cfg = plugin.getConfigManager().getSkillsConfig();
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        SkillManager mgr = plugin.getSkillManager();

        // Sync skill points in case level changed
        mgr.syncSkillPoints(data);

        // ── Filler ────────────────────────────────────────────────────────────
        String fillerMatName = cfg.getString(SEC + ".filler.material", "GRAY_STAINED_GLASS_PANE");
        Material fillerMat = parseMaterial(fillerMatName, Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = createItem(fillerMat, cfg.getString(SEC + ".filler.name", " "));
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        // ── Track headers (row 0) ─────────────────────────────────────────────
        inventory.setItem(SLOT_SAND_HEADER, buildHeader(cfg, "sand-header", player, data, mgr));
        inventory.setItem(SLOT_GEM_HEADER,  buildHeader(cfg, "gem-header",  player, data, mgr));
        inventory.setItem(SLOT_SB_HEADER,   buildHeader(cfg, "sb-header",   player, data, mgr));

        // ── Sand skills ───────────────────────────────────────────────────────
        SkillType[] sandSkills = { SkillType.SAND_SKILL_1, SkillType.SAND_SKILL_2,
                SkillType.SAND_SKILL_3, SkillType.SAND_SKILL_4 };
        for (int i = 0; i < 4; i++) {
            inventory.setItem(SAND_SLOTS[i], buildSkillItem(cfg, sandSkills[i], data, mgr, player));
        }

        // ── Gem skills ────────────────────────────────────────────────────────
        SkillType[] gemSkills = { SkillType.GEM_SKILL_1, SkillType.GEM_SKILL_2,
                SkillType.GEM_SKILL_3, SkillType.GEM_SKILL_4 };
        for (int i = 0; i < 4; i++) {
            inventory.setItem(GEM_SLOTS[i], buildSkillItem(cfg, gemSkills[i], data, mgr, player));
        }

        // ── Sandbucks skills ──────────────────────────────────────────────────
        SkillType[] sbSkills = { SkillType.SANDBUCKS_SKILL_1, SkillType.SANDBUCKS_SKILL_2,
                SkillType.SANDBUCKS_SKILL_3, SkillType.SANDBUCKS_SKILL_4 };
        for (int i = 0; i < 4; i++) {
            inventory.setItem(SB_SLOTS[i], buildSkillItem(cfg, sbSkills[i], data, mgr, player));
        }

        // ── Back button ───────────────────────────────────────────────────────
        Material backMat   = parseMaterial(cfg.getString(SEC + ".back.material", "ARROW"), Material.ARROW);
        String   backName  = cfg.getString(SEC + ".back.name", "&c&lBack to Menu");
        List<String> backLore = cfg.getStringList(SEC + ".back.lore");
        inventory.setItem(SLOT_BACK, createItem(backMat, backName, backLore.isEmpty() ? null : backLore));

        // ── Info / skill points display ───────────────────────────────────────
        inventory.setItem(SLOT_INFO, buildInfoItem(cfg, data, mgr));
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildHeader(FileConfiguration cfg, String key, Player player,
                                  PlayerData data, SkillManager mgr) {
        String path = SEC + "." + key;
        Material mat  = parseMaterial(cfg.getString(path + ".material", "WHITE_STAINED_GLASS_PANE"),
                Material.WHITE_STAINED_GLASS_PANE);
        String   name = cfg.getString(path + ".name", "&f&l" + key);

        // Build lore with track-specific overall multiplier placeholders
        List<String> rawLore = cfg.getStringList(path + ".lore");
        List<String> lore = new ArrayList<>();
        double sandMult  = mgr.getSandMultiplier(data);
        double gemsMult  = mgr.getGemsMultiplier(data);
        double sbMult    = mgr.getSandbucksMultiplier(data);
        int available    = data.getAvailableSkillPoints();
        int earned       = data.getSkillPointsEarned();
        int spent        = data.getSkillPointsSpent();

        for (String line : rawLore) {
            lore.add(applyPlaceholders(line,
                    "%sand_mult%",   formatMultiplier(sandMult),
                    "%gems_mult%",   formatMultiplier(gemsMult),
                    "%sb_mult%",     formatMultiplier(sbMult),
                    "%available%",   String.valueOf(available),
                    "%earned%",      String.valueOf(earned),
                    "%spent%",       String.valueOf(spent)));
        }
        return createItem(mat, name, lore.isEmpty() ? null : lore);
    }

    private ItemStack buildSkillItem(FileConfiguration cfg, SkillType skill,
                                     PlayerData data, SkillManager mgr, Player player) {
        boolean owned     = data.hasSkill(skill);
        boolean prereqMet = mgr.hasPrerequisite(data, skill);
        boolean canAfford = data.getAvailableSkillPoints() >= mgr.getSkillCost(skill);

        // State: owned → purchased; prereqMet && canAfford → available; prereqMet && !canAfford → no-points; else → locked
        String state;
        if (owned)                         state = "purchased";
        else if (!prereqMet)               state = "locked";
        else if (canAfford)                state = "available";
        else                               state = "no-points";

        String track = skill.getTrack();
        int    tier  = skill.getTier();
        String pathBase = SEC + ".skill-states." + state;

        // Material: state-specific, defaulting sensibly
        Material mat = parseMaterial(cfg.getString(pathBase + ".material", defaultMaterial(state)),
                Material.valueOf(defaultMaterial(state)));

        // Name template
        String nameTemplate = cfg.getString(pathBase + ".name", defaultName(state, track, tier));

        // Bonus display
        String bonusStr = getBonusDisplay(mgr, skill);
        String costStr  = String.valueOf(mgr.getSkillCost(skill));
        String tierStr  = String.valueOf(tier);
        String trackDisplay = capitalize(track);

        String name = applyPlaceholders(nameTemplate,
                "%track%", trackDisplay, "%tier%", tierStr,
                "%bonus%", bonusStr, "%cost%", costStr);

        // Lore template
        List<String> loreTpl = cfg.getStringList(pathBase + ".lore");
        if (loreTpl.isEmpty()) loreTpl = defaultLore(state, track, tier, bonusStr, costStr);

        List<String> lore = new ArrayList<>();
        for (String line : loreTpl) {
            lore.add(applyPlaceholders(line,
                    "%track%", trackDisplay, "%tier%", tierStr,
                    "%bonus%", bonusStr, "%cost%", costStr));
        }

        return createItem(mat, name, lore.isEmpty() ? null : lore);
    }

    private ItemStack buildInfoItem(FileConfiguration cfg, PlayerData data, SkillManager mgr) {
        String path    = SEC + ".info";
        Material mat   = parseMaterial(cfg.getString(path + ".material", "NETHER_STAR"), Material.NETHER_STAR);
        String   name  = cfg.getString(path + ".name", "&e&lSkill Points");
        int available  = data.getAvailableSkillPoints();
        int earned     = data.getSkillPointsEarned();
        int spent      = data.getSkillPointsSpent();
        int nextLevel  = ((data.getLevel() / 5) + 1) * 5;

        List<String> rawLore = cfg.getStringList(path + ".lore");
        if (rawLore.isEmpty()) {
            rawLore = List.of(
                    "&8ꜱᴋɪʟʟ ᴛʀᴇᴇ",
                    "",
                    "&7Available Points: &a%available%",
                    "&7Total Earned: &e%earned%",
                    "&7Total Spent: &6%spent%",
                    "",
                    "&7Next point at &eLevel %next_level%",
                    "&7(1 point per 5 levels)");
        }
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            lore.add(applyPlaceholders(line,
                    "%available%",  String.valueOf(available),
                    "%earned%",     String.valueOf(earned),
                    "%spent%",      String.valueOf(spent),
                    "%next_level%", String.valueOf(nextLevel)));
        }
        return createItem(mat, name, lore.isEmpty() ? null : lore);
    }

    // ── Click handling ────────────────────────────────────────────────────────

    @Override
    public void handleClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();

        if (slot == SLOT_BACK) {
            new MenuGUI(plugin).open(player);
            return;
        }

        // Resolve which skill was clicked (if any)
        SkillType skill = resolveSkill(slot);
        if (skill == null) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        SkillManager mgr = plugin.getSkillManager();
        mgr.syncSkillPoints(data);

        if (data.hasSkill(skill)) {
            // Already purchased — play subtle click
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        if (!mgr.hasPrerequisite(data, skill)) {
            plugin.getMessageManager().sendMessage(player, "messages.skill-locked");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (!mgr.purchaseSkill(data, skill)) {
            plugin.getMessageManager().sendMessage(player, "messages.skill-no-points",
                    "%cost%", String.valueOf(mgr.getSkillCost(skill)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Purchased successfully
        plugin.getMessageManager().sendMessage(player, "messages.skill-purchased",
                "%skill%", capitalize(skill.getTrack()) + " Skill " + skill.getTier(),
                "%points%", String.valueOf(data.getAvailableSkillPoints()));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.1f);
        plugin.getDataManager().savePlayerData(player);
        setupInventory(player); // refresh
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SkillType resolveSkill(int slot) {
        SkillType[] sandSkills = { SkillType.SAND_SKILL_1, SkillType.SAND_SKILL_2,
                SkillType.SAND_SKILL_3, SkillType.SAND_SKILL_4 };
        SkillType[] gemSkills  = { SkillType.GEM_SKILL_1,  SkillType.GEM_SKILL_2,
                SkillType.GEM_SKILL_3,  SkillType.GEM_SKILL_4 };
        SkillType[] sbSkills   = { SkillType.SANDBUCKS_SKILL_1, SkillType.SANDBUCKS_SKILL_2,
                SkillType.SANDBUCKS_SKILL_3, SkillType.SANDBUCKS_SKILL_4 };

        for (int i = 0; i < 4; i++) if (SAND_SLOTS[i] == slot) return sandSkills[i];
        for (int i = 0; i < 4; i++) if (GEM_SLOTS[i]  == slot) return gemSkills[i];
        for (int i = 0; i < 4; i++) if (SB_SLOTS[i]   == slot) return sbSkills[i];
        return null;
    }

    private String getBonusDisplay(SkillManager mgr, SkillType skill) {
        return switch (skill.getTrack()) {
            case "sand"      -> mgr.getSandBonusDisplay(skill);
            case "gems"      -> mgr.getGemsBonusDisplay(skill);
            case "sandbucks" -> mgr.getSandbucksBonusDisplay(skill);
            default          -> "+?%";
        };
    }

    private String formatMultiplier(double mult) {
        if (mult == Math.floor(mult)) return (int) mult + "x";
        return String.format("%.2fx", mult);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── Default fallback content ──────────────────────────────────────────────

    private String defaultMaterial(String state) {
        return switch (state) {
            case "purchased" -> "GREEN_STAINED_GLASS_PANE";
            case "available" -> "YELLOW_STAINED_GLASS_PANE";
            case "no-points" -> "ORANGE_STAINED_GLASS_PANE";
            default          -> "RED_STAINED_GLASS_PANE";   // locked
        };
    }

    private String defaultName(String state, String track, int tier) {
        String trackLabel = capitalize(track) + " Skill " + tier;
        return switch (state) {
            case "purchased" -> "&a&l" + trackLabel + " &7(Owned)";
            case "available" -> "&e&l" + trackLabel + " &7(Available)";
            case "no-points" -> "&6&l" + trackLabel + " &7(Not enough points)";
            default          -> "&c&l" + trackLabel + " &7(Locked)";
        };
    }

    private List<String> defaultLore(String state, String track, int tier,
                                     String bonusStr, String costStr) {
        List<String> lore = new ArrayList<>();
        lore.add("&8ꜱᴋɪʟʟ ᴛʀᴇᴇ");
        lore.add("");
        lore.add("&7Bonus: &e" + bonusStr + " " + capitalize(track));
        lore.add("");
        switch (state) {
            case "purchased" -> lore.add("&a✔ Purchased!");
            case "available" -> {
                lore.add("&7Cost: &b" + costStr + " Skill Point(s)");
                lore.add("");
                lore.add("&aClick to unlock!");
            }
            case "no-points" -> {
                lore.add("&7Cost: &b" + costStr + " Skill Point(s)");
                lore.add("");
                lore.add("&cNot enough Skill Points!");
            }
            default -> {
                lore.add("&c✖ Locked");
                lore.add("&7Unlock the previous skill first.");
            }
        }
        return lore;
    }
}