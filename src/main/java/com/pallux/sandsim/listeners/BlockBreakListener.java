package com.pallux.sandsim.listeners;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.utils.NumberFormatter;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.math.BigDecimal;
import java.util.Random;

public class BlockBreakListener implements Listener {

    private final SandSimPlugin plugin;
    private final Random random;

    public BlockBreakListener(SandSimPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getSandBlockManager().isSandBlock(event.getBlock())) return;

        if (!plugin.getShovelManager().isSandShovel(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "messages.need-shovel");
            return;
        }

        if (plugin.getSandBlockManager().isOnCooldown(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }

        event.setDropItems(false);
        event.setExpToDrop(0);

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        processSandMining(player, data, event);
    }

    private void processSandMining(Player player, PlayerData data, BlockBreakEvent event) {
        // --- Sand calculation ---
        double sandMultiplier   = plugin.getUpgradeManager().getSandMultiplier(data);
        double rebirthMultiplier = plugin.getRebirthManager().getRebirthMultiplier(data);
        // Event bonus is additive on top of all multipliers: total * (1 + eventBonus)
        double eventSandBonus   = plugin.getEventManager().getSandBonus();
        double totalMultiplier  = sandMultiplier * rebirthMultiplier * (1.0 + eventSandBonus);

        BigDecimal sandAmount = BigDecimal.valueOf(totalMultiplier);
        data.addSand(sandAmount);

        // --- Leveling ---
        long xpGain = 1L + (long) plugin.getEventManager().getXpBonus();
        int levelsGained = data.addXp(xpGain);

        // Notify level-up
        if (levelsGained > 0) {
            plugin.getMessageManager().sendMessage(player, "messages.level-up",
                    "%level%", String.valueOf(data.getLevel()));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        // Cooldown
        plugin.getSandBlockManager().setCooldown(event.getBlock().getLocation(), data);

        // Explosion
        checkSandExplosion(player, data, event);

        // Gems
        checkGemDrop(player, data);

        // Action bar feedback
        plugin.getMessageManager().sendActionBar(player, "messages.sand-mined",
                "%amount%", NumberFormatter.format(sandAmount));

        player.playSound(player.getLocation(), Sound.BLOCK_SAND_BREAK, 1.0f, 1.0f);
    }

    private void checkSandExplosion(Player player, PlayerData data, BlockBreakEvent event) {
        double explosionChance = plugin.getUpgradeManager().getSandExplosionChance(data);
        if (explosionChance <= 0) return;

        if (random.nextDouble() * 100 < explosionChance) {
            int radius = plugin.getUpgradeManager().getSandExplosionRadius(data);
            int blocksExploded = breakSandInRadius(event.getBlock().getLocation(), radius, data);

            double sandMultiplier    = plugin.getUpgradeManager().getSandMultiplier(data);
            double rebirthMultiplier = plugin.getRebirthManager().getRebirthMultiplier(data);
            double eventSandBonus    = plugin.getEventManager().getSandBonus();
            double totalMultiplier   = sandMultiplier * rebirthMultiplier * (1.0 + eventSandBonus);

            BigDecimal explosionSand = BigDecimal.valueOf(totalMultiplier * blocksExploded);
            data.addSand(explosionSand);

            // XP for each exploded block
            if (blocksExploded > 0) {
                long xpGain = blocksExploded * (1L + (long) plugin.getEventManager().getXpBonus());
                data.addXp(xpGain);
            }

            plugin.getMessageManager().sendMessage(player, "messages.sand-explosion",
                    "%blocks%", String.valueOf(blocksExploded));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
        }
    }

    private int breakSandInRadius(org.bukkit.Location center, int radius, PlayerData data) {
        int count = 0;
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    if (x * x + y * y + z * z > radius * radius) continue;

                    org.bukkit.Location loc = new org.bukkit.Location(
                            center.getWorld(), cx + x, cy + y, cz + z);
                    org.bukkit.block.Block block = loc.getBlock();

                    if (plugin.getSandBlockManager().isSandBlock(block) &&
                            !plugin.getSandBlockManager().isOnCooldown(loc)) {
                        plugin.getSandBlockManager().setCooldown(loc, data);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void checkGemDrop(Player player, PlayerData data) {
        double gemChance = plugin.getUpgradeManager().getGemChance(data);
        // Event adds a flat bonus chance
        double eventGemBonus = plugin.getEventManager().getGemChanceBonus() * 100.0; // convert to %
        double totalGemChance = gemChance + eventGemBonus;

        if (totalGemChance <= 0) return;

        if (random.nextDouble() * 100 < totalGemChance) {
            double gemMultiplier = plugin.getUpgradeManager().getGemMultiplier(data);
            BigDecimal gemAmount = BigDecimal.valueOf(gemMultiplier);
            data.addGems(gemAmount);

            plugin.getMessageManager().sendMessage(player, "messages.gem-found",
                    "%amount%", gemAmount.toPlainString());
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
        }
    }
}
