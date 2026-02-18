package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ShovelManager {

    private final SandSimPlugin plugin;
    private final NamespacedKey shovelKey;

    public ShovelManager(SandSimPlugin plugin) {
        this.plugin    = plugin;
        this.shovelKey = new NamespacedKey(plugin, "sand_shovel");
    }

    public ItemStack createShovel(int efficiencyLevel) {
        FileConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();

        ItemStack shovel = new ItemStack(Material.WOODEN_SHOVEL);
        ItemMeta  meta   = shovel.getItemMeta();

        if (meta != null) {
            String name = itemsConfig.getString("shovel.name", "&e&lSand Shovel");
            meta.displayName(ColorUtils.toComponent(name));

            List<String> loreConfig = itemsConfig.getStringList("shovel.lore");
            if (!loreConfig.isEmpty()) {
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                for (String line : loreConfig) lore.add(ColorUtils.toComponent(line));
                meta.lore(lore);
            }

            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(shovelKey, PersistentDataType.BOOLEAN, true);

            shovel.setItemMeta(meta);

            if (efficiencyLevel > 0) {
                shovel.addUnsafeEnchantment(Enchantment.EFFICIENCY, Math.min(efficiencyLevel, 5));
            }
        }
        return shovel;
    }

    public ItemStack createShovel() { return createShovel(0); }

    public boolean isSandShovel(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_SHOVEL) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(shovelKey, PersistentDataType.BOOLEAN);
    }

    public void giveShovel(Player player) {
        if (hasShovel(player)) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        player.getInventory().addItem(createShovel(data.getUpgradeLevel(PlayerData.UpgradeType.EFFICIENCY)));
    }

    public void refreshShovel(Player player) {
        PlayerData data     = plugin.getDataManager().getPlayerData(player);
        int        effLevel = data.getUpgradeLevel(PlayerData.UpgradeType.EFFICIENCY);
        ItemStack  newShovel = createShovel(effLevel);

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isSandShovel(item)) {
                player.getInventory().setItem(i, newShovel);
                return;
            }
        }
        player.getInventory().addItem(newShovel);
    }

    public boolean hasShovel(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSandShovel(item)) return true;
        }
        return false;
    }

    public void removeDuplicateShovels(Player player) {
        boolean foundFirst = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isSandShovel(item)) {
                if (foundFirst) {
                    player.getInventory().setItem(i, null);
                } else {
                    foundFirst = true;
                }
            }
        }
    }
}
