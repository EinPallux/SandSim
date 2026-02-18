package com.pallux.sandsim.gui;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.manager.LeaderboardManager;
import com.pallux.sandsim.manager.LeaderboardManager.LeaderboardEntry;
import com.pallux.sandsim.manager.LeaderboardManager.LeaderboardType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardGUI extends BaseGUI {

    private static final String SEC = "leaderboard";
    private LeaderboardType currentType;

    public LeaderboardGUI(SandSimPlugin plugin) { this(plugin, LeaderboardType.SAND); }

    public LeaderboardGUI(SandSimPlugin plugin, LeaderboardType type) {
        super(plugin, SEC);
        this.currentType = type;
    }

    @Override
    protected void setupInventory(Player player) {
        inventory.clear();
        applyFiller(SEC);
        inventory.setItem(slotFromConfig(SEC + ".back", 49), itemFromConfig(SEC + ".back"));

        FileConfiguration cfg = plugin.getConfigManager().getGuiConfig();
        buildTab(cfg, "type-sand",      10, LeaderboardType.SAND);
        buildTab(cfg, "type-gems",      12, LeaderboardType.GEMS);
        buildTab(cfg, "type-sandbucks", 14, LeaderboardType.SANDBUCKS);
        buildTab(cfg, "type-rebirths",  16, LeaderboardType.REBIRTHS);
        buildEntries(cfg);
        applyPlaceholderItems(SEC);
    }

    private void buildTab(FileConfiguration cfg, String key, int defaultSlot, LeaderboardType type) {
        String path = SEC + "." + key;
        Material mat  = parseMaterial(cfg.getString(path + ".material", "STONE"), Material.STONE);
        String   name = cfg.getString(path + ".name", key);
        boolean selected = currentType == type;
        List<String> lore = cfg.getStringList(selected ? path + ".lore-selected" : path + ".lore-unselected");
        inventory.setItem(slotFromConfig(path, defaultSlot), createItem(mat, name, lore));
    }

    private void buildEntries(FileConfiguration cfg) {
        List<Integer> slots = cfg.getIntegerList(SEC + ".entry-slots");
        String entryName    = cfg.getString(SEC + ".entry.name", "&6#%rank% &7- &f%player%");
        List<String> entryLoreTpl = cfg.getStringList(SEC + ".entry.lore");
        String typeLabel = currentType.name().charAt(0) + currentType.name().substring(1).toLowerCase();
        List<LeaderboardEntry> entries = plugin.getLeaderboardManager().getLeaderboard(currentType);

        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            if (slot < 0 || slot >= inventory.getSize()) continue;

            if (i >= entries.size()) {
                List<String> emptyLore = new ArrayList<>();
                emptyLore.add("&8No player yet");
                inventory.setItem(slot, createItem(Material.GRAY_STAINED_GLASS_PANE,
                        applyPlaceholders(entryName, "%rank%", String.valueOf(i + 1), "%player%", "???"), emptyLore));
                continue;
            }

            LeaderboardEntry entry = entries.get(i);
            int rank = i + 1;
            Material mat = switch (rank) {
                case 1  -> parseMaterial(cfg.getString(SEC + ".entry-material-1", "GOLD_BLOCK"),   Material.GOLD_BLOCK);
                case 2  -> parseMaterial(cfg.getString(SEC + ".entry-material-2", "IRON_BLOCK"),   Material.IRON_BLOCK);
                case 3  -> parseMaterial(cfg.getString(SEC + ".entry-material-3", "COPPER_BLOCK"), Material.COPPER_BLOCK);
                default -> parseMaterial(cfg.getString(SEC + ".entry-material-default", "PLAYER_HEAD"), Material.PLAYER_HEAD);
            };

            String name = applyPlaceholders(entryName, "%rank%", String.valueOf(rank), "%player%", entry.getPlayerName());
            List<String> lore = new ArrayList<>();
            for (String line : entryLoreTpl) {
                lore.add(applyPlaceholders(line, "%type%", typeLabel, "%value%", formatNumber(entry.getValue()), "%rank%", String.valueOf(rank), "%player%", entry.getPlayerName()));
            }
            inventory.setItem(slot, createItem(mat, name, lore));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        if      (slot == slotFromConfig(SEC + ".back",           49)) { new MenuGUI(plugin).open(player); }
        else if (slot == slotFromConfig(SEC + ".type-sand",      10)) { currentType = LeaderboardType.SAND;      setupInventory(player); }
        else if (slot == slotFromConfig(SEC + ".type-gems",      12)) { currentType = LeaderboardType.GEMS;      setupInventory(player); }
        else if (slot == slotFromConfig(SEC + ".type-sandbucks", 14)) { currentType = LeaderboardType.SANDBUCKS; setupInventory(player); }
        else if (slot == slotFromConfig(SEC + ".type-rebirths",  16)) { currentType = LeaderboardType.REBIRTHS;  setupInventory(player); }
    }
}
