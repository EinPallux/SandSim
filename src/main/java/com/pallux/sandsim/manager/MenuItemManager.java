package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class MenuItemManager {

    private final SandSimPlugin plugin;
    private final NamespacedKey menuItemKey;

    public MenuItemManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        this.menuItemKey = new NamespacedKey(plugin, "menu_item");
    }

    public ItemStack createMenuItem() {
        FileConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();

        // Read material from items.yml, default to NETHER_STAR
        String matName = itemsConfig.getString("menu-item.material", "NETHER_STAR");
        Material material;
        try {
            material = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.NETHER_STAR;
        }

        ItemStack menuItem = new ItemStack(material);
        ItemMeta meta = menuItem.getItemMeta();

        if (meta != null) {
            // Name from items.yml
            String name = itemsConfig.getString("menu-item.name", "&6&lMenu &7(Right Click)");
            meta.displayName(ColorUtils.toComponent(name));

            // Lore from items.yml
            List<String> loreConfig = itemsConfig.getStringList("menu-item.lore");
            if (!loreConfig.isEmpty()) {
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                for (String line : loreConfig) lore.add(ColorUtils.toComponent(line));
                meta.lore(lore);
            }

            // Mark as menu item
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(menuItemKey, PersistentDataType.BOOLEAN, true);

            menuItem.setItemMeta(meta);
        }

        return menuItem;
    }

    public boolean isMenuItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(menuItemKey, PersistentDataType.BOOLEAN);
    }

    public void giveMenuItem(Player player) {
        if (hasMenuItem(player)) return;
        ItemStack menuItem = createMenuItem();
        if (player.getInventory().getItem(8) == null) {
            player.getInventory().setItem(8, menuItem);
        } else {
            player.getInventory().addItem(menuItem);
        }
    }

    public boolean hasMenuItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMenuItem(item)) return true;
        }
        return false;
    }

    public void removeDuplicateMenuItems(Player player) {
        boolean foundFirst = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isMenuItem(item)) {
                if (foundFirst) {
                    player.getInventory().setItem(i, null);
                } else {
                    foundFirst = true;
                }
            }
        }
    }

    public void ensureMenuItemInSlot8(Player player) {
        ItemStack slot8Item = player.getInventory().getItem(8);
        if (isMenuItem(slot8Item)) return;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isMenuItem(item)) {
                if (slot8Item == null) {
                    player.getInventory().setItem(8, item);
                    player.getInventory().setItem(i, null);
                }
                return;
            }
        }

        giveMenuItem(player);
    }

    /**
     * Refreshes the menu item for a player (re-reads name/lore from items.yml).
     * Call this after a /sandsim reload.
     */
    public void refreshMenuItem(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isMenuItem(item)) {
                player.getInventory().setItem(i, createMenuItem());
                return;
            }
        }
        giveMenuItem(player);
    }
}
