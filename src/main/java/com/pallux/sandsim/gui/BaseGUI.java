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

    public BaseGUI(SandSimPlugin plugin, String configSection) {
        this.plugin = plugin;
        FileConfiguration cfg = plugin.getConfigManager().getGuiConfig();
        String title = cfg.getString(configSection + ".title", "GUI");
        int    size  = cfg.getInt(configSection + ".size", 54);
        this.inventory = Bukkit.createInventory(this, size, ColorUtils.toComponent(title));
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open(Player player) {
        setupInventory(player);
        player.openInventory(inventory);
    }

    protected abstract void setupInventory(Player player);
    public    abstract void handleClick(InventoryClickEvent event, Player player);

    protected void applyFiller(String configSection) {
        FileConfiguration cfg = plugin.getConfigManager().getGuiConfig();
        if (!cfg.getBoolean(configSection + ".filler.enabled", false)) return;
        Material mat = parseMaterial(cfg.getString(configSection + ".filler.material", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = createItem(mat, " ");
        for (int slot : cfg.getIntegerList(configSection + ".filler.slots")) {
            if (slot >= 0 && slot < inventory.getSize()) inventory.setItem(slot, filler);
        }
    }

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

    protected ItemStack itemFromConfig(String path, String... placeholders) {
        FileConfiguration cfg = plugin.getConfigManager().getGuiConfig();
        Material mat  = parseMaterial(cfg.getString(path + ".material", "STONE"), Material.STONE);
        String   name = applyPlaceholders(cfg.getString(path + ".name", ""), placeholders);
        List<String> rawLore = cfg.getStringList(path + ".lore");
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) lore.add(applyPlaceholders(line, placeholders));
        return createItem(mat, name, lore);
    }

    protected int slotFromConfig(String path, int fallback) {
        return plugin.getConfigManager().getGuiConfig().getInt(path + ".slot", fallback);
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

    protected void applyPlaceholderItems(String configSection) {
        FileConfiguration cfg = plugin.getConfigManager().getGuiConfig();
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
