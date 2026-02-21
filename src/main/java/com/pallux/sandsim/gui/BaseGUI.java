package com.pallux.sandsim.gui;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.utils.ColorUtils;
import com.pallux.sandsim.utils.NumberFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseGUI implements InventoryHolder {

    protected final SandSimPlugin plugin;
    protected final Inventory inventory;

    /**
     * Primary constructor — GUI reads its title and size from the supplied
     * {@code config} file under the given {@code configSection} key.
     *
     * Each GUI class should pass its own dedicated config file here, e.g.
     * {@code plugin.getConfigManager().getUpgradesGuiConfig()}.
     */
    public BaseGUI(SandSimPlugin plugin, String configSection, FileConfiguration config) {
        this.plugin = plugin;
        String title = config.getString(configSection + ".title", "GUI");
        int    size  = config.getInt(configSection + ".size", 54);
        this.inventory = Bukkit.createInventory(this, size, ColorUtils.toComponent(title));
    }

    /**
     * Legacy / convenience constructor that reads from the main gui.yml.
     * Used only by {@link MenuGUI} (the Dashboard).
     */
    public BaseGUI(SandSimPlugin plugin, String configSection) {
        this(plugin, configSection, plugin.getConfigManager().getGuiConfig());
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open(Player player) {
        setupInventory(player);
        player.openInventory(inventory);
    }

    protected abstract void setupInventory(Player player);
    public    abstract void handleClick(InventoryClickEvent event, Player player);

    // ── Filler helper ─────────────────────────────────────────────────────────

    /**
     * Reads filler configuration from whichever config file the subclass uses.
     * Subclasses should call {@link #applyFiller(String, FileConfiguration)}
     * with their own config; this overload falls back to gui.yml for backwards
     * compatibility with MenuGUI.
     */
    protected void applyFiller(String configSection) {
        applyFiller(configSection, plugin.getConfigManager().getGuiConfig());
    }

    protected void applyFiller(String configSection, FileConfiguration cfg) {
        if (!cfg.getBoolean(configSection + ".filler.enabled", false)) return;
        Material mat = parseMaterial(
                cfg.getString(configSection + ".filler.material", "GRAY_STAINED_GLASS_PANE"),
                Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = createItem(mat, " ");
        for (int slot : cfg.getIntegerList(configSection + ".filler.slots")) {
            if (slot >= 0 && slot < inventory.getSize()) inventory.setItem(slot, filler);
        }
    }

    // ── Item creation helpers ─────────────────────────────────────────────────

    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.toComponent(name));
            if (lore != null && !lore.isEmpty()) {
                List<Component> cl = new ArrayList<>();
                for (String line : lore) cl.add(ColorUtils.toComponent(line));
                meta.lore(cl);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack createItem(Material material, String name) {
        return createItem(material, name, null);
    }

    /**
     * Builds an ItemStack from a config path using the main gui.yml.
     * Used only by MenuGUI; other GUIs read their own config directly.
     */
    protected ItemStack itemFromConfig(String path, String... placeholders) {
        return itemFromConfig(path, plugin.getConfigManager().getGuiConfig(), placeholders);
    }

    protected ItemStack itemFromConfig(String path, FileConfiguration cfg, String... placeholders) {
        Material mat  = parseMaterial(cfg.getString(path + ".material", "STONE"), Material.STONE);
        String   name = applyPlaceholders(cfg.getString(path + ".name", ""), placeholders);
        List<String> rawLore = cfg.getStringList(path + ".lore");
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) lore.add(applyPlaceholders(line, placeholders));
        return createItem(mat, name, lore);
    }

    // ── Config reading helpers ────────────────────────────────────────────────

    /** Reads a slot number from the main gui.yml (used by MenuGUI). */
    protected int slotFromConfig(String path, int fallback) {
        return slotFromConfig(path, plugin.getConfigManager().getGuiConfig(), fallback);
    }

    protected int slotFromConfig(String path, FileConfiguration cfg, int fallback) {
        return cfg.getInt(path + ".slot", fallback);
    }

    protected Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    protected String applyPlaceholders(String text, String... pairs) {
        if (text == null) return "";
        for (int i = 0; i + 1 < pairs.length; i += 2) text = text.replace(pairs[i], pairs[i + 1]);
        return text;
    }

    protected String replacePlaceholders(String text, Player player) {
        if (text == null) return "";
        var data = plugin.getDataManager().getPlayerData(player);
        return text
                .replace("%player%",     player.getName())
                .replace("%sand%",       NumberFormatter.format(data.getSand()))
                .replace("%gems%",       NumberFormatter.format(data.getGems()))
                .replace("%sandbucks%",  NumberFormatter.format(data.getSandbucks()))
                .replace("%rebirths%",   NumberFormatter.format(data.getRebirths()))
                .replace("%multiplier%", String.format("%.2fx", plugin.getRebirthManager().getRebirthMultiplier(data)))
                .replace("%level%",      String.valueOf(data.getLevel()))
                .replace("%level_xp%",   data.getXpPercent() + "%");
    }

    protected String formatNumber(java.math.BigDecimal number) {
        return NumberFormatter.format(number);
    }

    /** Reads placeholder item config from the main gui.yml (used by MenuGUI). */
    protected void applyPlaceholderItems(String configSection) {
        applyPlaceholderItems(configSection, plugin.getConfigManager().getGuiConfig());
    }

    protected void applyPlaceholderItems(String configSection, FileConfiguration cfg) {
        String path = configSection + ".placeholder";
        if (!cfg.getBoolean(path + ".enabled", false)) return;
        Material mat  = parseMaterial(cfg.getString(path + ".material", "GRAY_DYE"), Material.GRAY_DYE);
        String   name = cfg.getString(path + ".name", "&7&o???");
        List<String> lore = cfg.getStringList(path + ".lore");
        ItemStack item = createItem(mat, name, lore.isEmpty() ? null : lore);
        for (int slot : cfg.getIntegerList(path + ".slots")) {
            if (slot >= 0 && slot < inventory.getSize()) inventory.setItem(slot, item);
        }
    }
}