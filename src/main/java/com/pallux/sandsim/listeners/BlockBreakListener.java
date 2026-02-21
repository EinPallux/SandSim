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

        // ── Sand blocks (normal + red) ─────────────────────────────────────
        if (plugin.getSandBlockManager().isSandBlock(event.getBlock())) {

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
            return;
        }

        // ── Non-sand block break protection ───────────────────────────────
        boolean protectionEnabled = plugin.getConfigManager().getMainConfig()
                .getBoolean("protection.block-break-protection", true);
        if (protectionEnabled && !player.hasPermission("sandsim.bypass.blockbreak")) {
            event.setCancelled(true);
        }
    }

    private void processSandMining(Player player, PlayerData data, BlockBreakEvent event) {
        // ── Red Sand bonus multiplier ──────────────────────────────────────
        double sandTypeMultiplier = plugin.getSandBlockManager().getSandTypeMultiplier(event.getBlock());

        // ── Sand calculation ───────────────────────────────────────────────
        double sandUpgradeMultiplier = plugin.getUpgradeManager().getSandMultiplier(data);
        double rebirthMultiplier     = plugin.getRebirthManager().getRebirthMultiplier(data);
        double eventSandBonus        = plugin.getEventManager().getSandBonus();
        double augmentSandMultiplier = plugin.getAugmentManager().getSandMultiplier(data);

        double totalMultiplier = sandUpgradeMultiplier
                * rebirthMultiplier
                * (1.0 + eventSandBonus)
                * augmentSandMultiplier
                * sandTypeMultiplier;

        BigDecimal sandAmount = BigDecimal.valueOf(totalMultiplier);
        data.addSand(sandAmount);

        // ── Leveling ───────────────────────────────────────────────────────
        long xpGain = 1L + (long) plugin.getEventManager().getXpBonus();
        int levelsGained = data.addXp(xpGain);

        if (levelsGained > 0) {
            plugin.getMessageManager().sendMessage(player, "messages.level-up",
                    "%level%", String.valueOf(data.getLevel()));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        // ── Cooldown ───────────────────────────────────────────────────────
        plugin.getSandBlockManager().setCooldown(event.getBlock().getLocation(), data);

        // ── Explosion ──────────────────────────────────────────────────────
        checkSandExplosion(player, data, event, sandTypeMultiplier);

        // ── Gems ───────────────────────────────────────────────────────────
        checkGemDrop(player, data);

        // ── Action bar feedback ────────────────────────────────────────────
        plugin.getMessageManager().sendActionBar(player, "messages.sand-mined",
                "%amount%", NumberFormatter.format(sandAmount));

        player.playSound(player.getLocation(), Sound.BLOCK_SAND_BREAK, 1.0f, 1.0f);
    }

    private void checkSandExplosion(Player player, PlayerData data, BlockBreakEvent event,
                                    double sandTypeMultiplier) {
        double explosionChance = plugin.getUpgradeManager().getSandExplosionChance(data);
        if (explosionChance <= 0) return;

        if (random.nextDouble() * 100 < explosionChance) {
            int radius = plugin.getUpgradeManager().getSandExplosionRadius(data);
            int[] explodedCounts = breakSandInRadius(event.getBlock().getLocation(), radius, data);
            int normalBlocks = explodedCounts[0];
            int redBlocks    = explodedCounts[1];

            double sandUpgrade       = plugin.getUpgradeManager().getSandMultiplier(data);
            double rebirthMult       = plugin.getRebirthManager().getRebirthMultiplier(data);
            double eventSandBonus    = plugin.getEventManager().getSandBonus();
            double augmentSandMult   = plugin.getAugmentManager().getSandMultiplier(data);
            double baseMultiplier    = sandUpgrade * rebirthMult * (1.0 + eventSandBonus) * augmentSandMult;
            double redSandMult       = plugin.getSandBlockManager().getRedSandMultiplier();

            // Normal blocks use base multiplier; red blocks use base * redSandMultiplier
            BigDecimal explosionSand = BigDecimal.valueOf(
                    (baseMultiplier * normalBlocks) + (baseMultiplier * redSandMult * redBlocks));
            data.addSand(explosionSand);

            int totalBlocks = normalBlocks + redBlocks;
            if (totalBlocks > 0) {
                long xpGain = totalBlocks * (1L + (long) plugin.getEventManager().getXpBonus());
                data.addXp(xpGain);
            }

            plugin.getMessageManager().sendMessage(player, "messages.sand-explosion",
                    "%blocks%", String.valueOf(totalBlocks));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
        }
    }

    /**
     * Breaks sand blocks in radius and returns [normalCount, redSandCount].
     */
    private int[] breakSandInRadius(org.bukkit.Location center, int radius, PlayerData data) {
        int normalCount = 0;
        int redCount    = 0;
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
                        boolean isRed = plugin.getSandBlockManager().isRedSand(block);
                        plugin.getSandBlockManager().setCooldown(loc, data);
                        if (isRed) redCount++;
                        else normalCount++;
                    }
                }
            }
        }
        return new int[]{ normalCount, redCount };
    }

    private void checkGemDrop(Player player, PlayerData data) {
        double gemChance      = plugin.getUpgradeManager().getGemChance(data);
        double eventGemBonus  = plugin.getEventManager().getGemChanceBonus() * 100.0;
        double totalGemChance = gemChance + eventGemBonus;

        if (totalGemChance <= 0) return;

        if (random.nextDouble() * 100 < totalGemChance) {
            double gemUpgradeMultiplier  = plugin.getUpgradeManager().getGemMultiplier(data);
            double augmentGemsMultiplier = plugin.getAugmentManager().getGemsMultiplier(data);

            BigDecimal gemAmount = BigDecimal.valueOf(gemUpgradeMultiplier * augmentGemsMultiplier);
            data.addGems(gemAmount);

            plugin.getMessageManager().sendMessage(player, "messages.gem-found",
                    "%amount%", gemAmount.toPlainString());
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
        }
    }
}