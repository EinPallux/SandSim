package com.pallux.sandsim.gui;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.AugmentDefinition;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.manager.AugmentManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated Augments GUI.
 *
 * Layout (54 slots):
 *   Row 0  (slots  0– 8): filler
 *   Row 1  (slots  9–17): augment items at 10-16
 *   Row 2  (slots 18–26): augment items at 19-25
 *   Row 3  (slots 27–35): augment items at 28-34
 *   Row 4  (slots 36–44): filler
 *   Row 5  (slots 45–53): prev(45) | filler | back(49) | filler | next(53)
 *
 * All item materials, names, and lore lines are read from augments-gui.yml.
 */
public class AugmentsGUI extends BaseGUI {

    private static final String SEC = "augments-gui";

    // Inner grid slots (rows 1-3, columns 1-7 within each row)
    private static final int[] AUGMENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 53;

    private int page; // 0-indexed

    public AugmentsGUI(SandSimPlugin plugin) {
        this(plugin, 0);
    }

    public AugmentsGUI(SandSimPlugin plugin, int page) {
        super(plugin, SEC, plugin.getConfigManager().getAugmentsGuiConfig());
        this.page = page;
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @Override
    protected void setupInventory(Player player) {
        inventory.clear();

        AugmentManager mgr  = plugin.getAugmentManager();
        PlayerData     data = plugin.getDataManager().getPlayerData(player);
        FileConfiguration gui = plugin.getConfigManager().getAugmentsGuiConfig();

        // Tick so all state is current before render
        mgr.tickResearch(data);

        // Fill every slot with filler
        Material fillerMat = parseMaterial(gui.getString(SEC + ".filler.material", "GRAY_STAINED_GLASS_PANE"),
                Material.GRAY_STAINED_GLASS_PANE);
        String   fillerName = gui.getString(SEC + ".filler.name", " ");
        ItemStack filler = createItem(fillerMat, fillerName);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        // Augment items
        int totalTiers  = mgr.getTotalTiers();
        int totalPages  = Math.max(1, (int) Math.ceil((double) totalTiers / AUGMENT_SLOTS.length));
        int startTier   = page * AUGMENT_SLOTS.length + 1; // 1-based

        for (int i = 0; i < AUGMENT_SLOTS.length; i++) {
            int tier = startTier + i;
            if (tier > totalTiers) break;
            AugmentDefinition def = mgr.getAugment(tier);
            if (def == null) break;
            inventory.setItem(AUGMENT_SLOTS[i], buildAugmentItem(def, data, mgr, gui));
        }

        // ── Navigation ─────────────────────────────────────────────────────
        if (page > 0) {
            Material prevMat  = parseMaterial(gui.getString(SEC + ".prev-page.material", "ARROW"), Material.ARROW);
            String   prevName = gui.getString(SEC + ".prev-page.name", "&c&l← Previous Page");
            List<String> prevLore = new ArrayList<>();
            for (String line : gui.getStringList(SEC + ".prev-page.lore"))
                prevLore.add(applyPlaceholders(line, "%page%", String.valueOf(page)));
            inventory.setItem(SLOT_PREV, createItem(prevMat, prevName, prevLore.isEmpty() ? null : prevLore));
        }

        Material backMat  = parseMaterial(gui.getString(SEC + ".back.material", "BARRIER"), Material.BARRIER);
        String   backName = gui.getString(SEC + ".back.name", "&c&lBack to Menu");
        List<String> backLore = gui.getStringList(SEC + ".back.lore");
        inventory.setItem(SLOT_BACK, createItem(backMat, backName, backLore.isEmpty() ? null : backLore));

        if (page < totalPages - 1) {
            Material nextMat  = parseMaterial(gui.getString(SEC + ".next-page.material", "ARROW"), Material.ARROW);
            String   nextName = gui.getString(SEC + ".next-page.name", "&a&lNext Page →");
            List<String> nextLore = new ArrayList<>();
            for (String line : gui.getStringList(SEC + ".next-page.lore"))
                nextLore.add(applyPlaceholders(line, "%page%", String.valueOf(page + 2)));
            inventory.setItem(SLOT_NEXT, createItem(nextMat, nextName, nextLore.isEmpty() ? null : nextLore));
        }
    }

    // ── Augment item builder ──────────────────────────────────────────────────

    private ItemStack buildAugmentItem(AugmentDefinition def, PlayerData data,
                                       AugmentManager mgr, FileConfiguration gui) {
        int unlockedTier    = mgr.getUnlockedTier(data);
        int researchingTier = mgr.getResearchingTier(data);
        int tier            = def.getTier();

        AugmentState state = getState(tier, unlockedTier, researchingTier);

        String stateKey = switch (state) {
            case UNLOCKED    -> "unlocked";
            case RESEARCHING -> "researching";
            case AVAILABLE   -> "available";
            case LOCKED      -> "locked";
        };

        Material mat = parseMaterial(
                gui.getString(SEC + ".states." + stateKey + ".material", defaultMaterial(state)),
                Material.valueOf(defaultMaterial(state)));

        String nameTemplate = gui.getString(SEC + ".states." + stateKey + ".name",
                defaultName(state));

        List<String> loreTpl = gui.getStringList(SEC + ".states." + stateKey + ".lore");
        if (loreTpl.isEmpty()) loreTpl = defaultLore(state);

        long remainingSecs = (state == AugmentState.RESEARCHING)
                ? mgr.getResearchSecondsRemaining(data) : 0L;

        String sandStr      = formatPct(def.getSandPercent());
        String gemsStr      = formatPct(def.getGemsPercent());
        String sbStr        = formatPct(def.getSandbucksPercent());
        String costStr      = String.valueOf(def.getGemCost());
        String timeStr      = mgr.formatTime(def.getResearchTimeSeconds());
        String remainingStr = mgr.formatTime(remainingSecs);
        String nameStr      = def.getDisplayName();
        String tierStr      = String.valueOf(def.getTier());
        String uTierStr     = String.valueOf(data.getAugmentUnlockedTier());
        String rTierStr     = String.valueOf(data.getAugmentResearchingTier());

        String name = applyPlaceholders(nameTemplate,
                "%name%", nameStr, "%tier%", tierStr,
                "%sand%", sandStr, "%gems%", gemsStr, "%sandbucks%", sbStr,
                "%cost%", costStr, "%time%", timeStr, "%remaining%", remainingStr,
                "%unlocked_tier%", uTierStr, "%researching%", rTierStr);

        List<String> lore = new ArrayList<>();
        for (String line : loreTpl) {
            lore.add(applyPlaceholders(line,
                    "%name%", nameStr, "%tier%", tierStr,
                    "%sand%", sandStr, "%gems%", gemsStr, "%sandbucks%", sbStr,
                    "%cost%", costStr, "%time%", timeStr, "%remaining%", remainingStr,
                    "%unlocked_tier%", uTierStr, "%researching%", rTierStr));
        }

        return createItem(mat, name, lore.isEmpty() ? null : lore);
    }

    /** Formats a percent value: strips trailing .0 for clean display. */
    private String formatPct(double pct) {
        if (pct == 0.0) return "0";
        if (pct == Math.floor(pct)) return String.valueOf((int) pct);
        return String.valueOf(pct);
    }

    // ── Click handling ────────────────────────────────────────────────────────

    @Override
    public void handleClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();

        if (slot == SLOT_BACK) {
            new MenuGUI(plugin).open(player);
            return;
        }

        if (slot == SLOT_PREV && page > 0) {
            page--;
            setupInventory(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        if (slot == SLOT_NEXT) {
            AugmentManager mgr = plugin.getAugmentManager();
            int totalPages = Math.max(1,
                    (int) Math.ceil((double) mgr.getTotalTiers() / AUGMENT_SLOTS.length));
            if (page < totalPages - 1) {
                page++;
                setupInventory(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
            return;
        }

        int slotIndex = getAugmentSlotIndex(slot);
        if (slotIndex < 0) return;

        int tier = page * AUGMENT_SLOTS.length + slotIndex + 1;
        AugmentManager mgr  = plugin.getAugmentManager();
        PlayerData     data = plugin.getDataManager().getPlayerData(player);

        mgr.tickResearch(data);

        AugmentDefinition def = mgr.getAugment(tier);
        if (def == null) return;

        AugmentState state = getState(tier, mgr.getUnlockedTier(data), mgr.getResearchingTier(data));

        switch (state) {
            case AVAILABLE -> {
                if (mgr.startResearch(data)) {
                    plugin.getMessageManager().sendMessage(player, "messages.augment-research-started",
                            "%augment%", def.getDisplayName(),
                            "%time%", mgr.formatTime(def.getResearchTimeSeconds()));
                    player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                    setupInventory(player);
                } else {
                    plugin.getMessageManager().sendMessage(player, "messages.augment-cannot-afford",
                            "%cost%", String.valueOf(def.getGemCost()));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }
            case RESEARCHING -> {
                long secs = mgr.getResearchSecondsRemaining(data);
                plugin.getMessageManager().sendMessage(player, "messages.augment-already-researching",
                        "%time%", mgr.formatTime(secs));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
            }
            case UNLOCKED -> player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            case LOCKED -> {
                plugin.getMessageManager().sendMessage(player, "messages.augment-locked");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getAugmentSlotIndex(int slot) {
        for (int i = 0; i < AUGMENT_SLOTS.length; i++) {
            if (AUGMENT_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    private AugmentState getState(int tier, int unlockedTier, int researchingTier) {
        if (tier <= unlockedTier)    return AugmentState.UNLOCKED;
        if (tier == researchingTier) return AugmentState.RESEARCHING;
        if (tier == unlockedTier + 1 && researchingTier == 0) return AugmentState.AVAILABLE;
        return AugmentState.LOCKED;
    }

    // ── Hardcoded fallbacks ───────────────────────────────────────────────────

    private String defaultMaterial(AugmentState state) {
        return switch (state) {
            case UNLOCKED    -> "GREEN_STAINED_GLASS_PANE";
            case RESEARCHING -> "YELLOW_STAINED_GLASS_PANE";
            case AVAILABLE   -> "YELLOW_STAINED_GLASS_PANE";
            case LOCKED      -> "RED_STAINED_GLASS_PANE";
        };
    }

    private String defaultName(AugmentState state) {
        return switch (state) {
            case UNLOCKED    -> "&a&l%name%";
            case RESEARCHING -> "&e&l%name%";
            case AVAILABLE   -> "&e&l%name%";
            case LOCKED      -> "&c&l%name%";
        };
    }

    private List<String> defaultLore(AugmentState state) {
        List<String> lore = new ArrayList<>();
        lore.add("&8ᴀᴜɢᴍᴇɴᴛꜱ");
        lore.add("");
        lore.add("&7Bonuses &8(replaces previous)&7:");
        lore.add("  &e+%sand%% &7Sand");
        lore.add("  &b+%gems%% &7Gems");
        lore.add("  &6+%sandbucks%% &7Sandbucks");
        lore.add("");
        switch (state) {
            case UNLOCKED -> lore.add("&a✔ Researched");
            case RESEARCHING -> {
                lore.add("&e⏳ Researching...");
                lore.add("&7Time remaining: &e%remaining%");
            }
            case AVAILABLE -> {
                lore.add("&7Research Cost: &b%cost% Gems");
                lore.add("&7Research Time: &e%time%");
                lore.add("");
                lore.add("&aClick to start research!");
            }
            case LOCKED -> {
                lore.add("&c✖ Locked");
                lore.add("&7Research the previous Augment first.");
            }
        }
        return lore;
    }

    private enum AugmentState {
        UNLOCKED, RESEARCHING, AVAILABLE, LOCKED
    }
}