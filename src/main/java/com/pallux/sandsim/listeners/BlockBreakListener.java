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

    /** Duration of both jackpot effects in milliseconds (5 seconds). */
    private static final long JACKPOT_DURATION_MS = 5_000L;

    /** Sand Jackpot sand multiplier bonus (×5 on top of overall multiplier). */
    private static final double SAND_JACKPOT_MULTIPLIER = 5.0;

    /** Gem Jackpot gem multiplier bonus (×5 on top of normal gem gain). */
    private static final double GEM_JACKPOT_MULTIPLIER = 5.0;

    public BlockBreakListener(SandSimPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

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

        boolean protectionEnabled = plugin.getConfigManager().getMainConfig()
                .getBoolean("protection.block-break-protection", true);
        if (protectionEnabled && !player.hasPermission("sandsim.bypass.blockbreak")) {
            event.setCancelled(true);
        }
    }

    private void processSandMining(Player player, PlayerData data, BlockBreakEvent event) {
        double sandTypeMultiplier = plugin.getSandBlockManager().getSandTypeMultiplier(event.getBlock());

        double sandUpgradeMultiplier = plugin.getUpgradeManager().getSandMultiplier(data);
        double rebirthMultiplier     = plugin.getRebirthManager().getRebirthMultiplier(data);
        double eventSandBonus        = plugin.getEventManager().getSandBonus();
        double augmentSandMultiplier = plugin.getAugmentManager().getSandMultiplier(data);
        double skillSandMultiplier   = plugin.getSkillManager().getSandMultiplier(data);

        // Overall multiplier BEFORE jackpot
        double overallMultiplier = sandUpgradeMultiplier
                * rebirthMultiplier
                * (1.0 + eventSandBonus)
                * augmentSandMultiplier
                * skillSandMultiplier
                * sandTypeMultiplier;

        // ── Sand Jackpot check ─────────────────────────────────────────────
        checkSandJackpot(player, data);

        // Apply ×5 if jackpot is active
        double totalMultiplier = data.isSandJackpotActive()
                ? overallMultiplier * SAND_JACKPOT_MULTIPLIER
                : overallMultiplier;

        BigDecimal sandAmount = BigDecimal.valueOf(totalMultiplier);
        data.addSand(sandAmount);

        // ── Leveling ───────────────────────────────────────────────────────
        long xpGain = 1L + (long) plugin.getEventManager().getXpBonus();
        int levelsGained = data.addXp(xpGain);

        if (levelsGained > 0) {
            int newPoints = plugin.getSkillManager().syncSkillPoints(data);
            plugin.getMessageManager().sendMessage(player, "messages.level-up",
                    "%level%", String.valueOf(data.getLevel()));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            if (newPoints > 0) {
                plugin.getMessageManager().sendMessage(player, "messages.skill-point-earned",
                        "%points%", String.valueOf(newPoints),
                        "%available%", String.valueOf(data.getAvailableSkillPoints()));
            }
        }

        plugin.getSandBlockManager().setCooldown(event.getBlock().getLocation(), data);

        checkSandExplosion(player, data, event, sandTypeMultiplier);
        checkGemDrop(player, data);

        // Action bar — show jackpot tag if active
        String actionBarKey = data.isSandJackpotActive()
                ? "messages.sand-mined-jackpot"
                : "messages.sand-mined";
        plugin.getMessageManager().sendActionBar(player, actionBarKey,
                "%amount%", NumberFormatter.format(sandAmount));

        player.playSound(player.getLocation(), Sound.BLOCK_SAND_BREAK, 1.0f, 1.0f);
    }

    // ── Sand Jackpot activation ───────────────────────────────────────────────

    /**
     * Rolls for Sand Jackpot activation. If triggered (and not already active),
     * activates the effect and notifies the player.
     */
    private void checkSandJackpot(Player player, PlayerData data) {
        if (data.isSandJackpotActive()) return; // already running
        double chance = plugin.getUpgradeManager().getSandJackpotChance(data);
        if (chance <= 0) return;
        if (random.nextDouble() * 100.0 < chance) {
            data.activateSandJackpot(JACKPOT_DURATION_MS);
            plugin.getMessageManager().sendMessage(player, "messages.sand-jackpot-triggered");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
        }
    }

    // ── Sand Explosion ────────────────────────────────────────────────────────

    private void checkSandExplosion(Player player, PlayerData data, BlockBreakEvent event,
                                    double sandTypeMultiplier) {
        double explosionChance = plugin.getUpgradeManager().getSandExplosionChance(data);
        if (explosionChance <= 0) return;

        if (random.nextDouble() * 100 < explosionChance) {
            int radius = plugin.getUpgradeManager().getSandExplosionRadius(data);
            int[] explodedCounts = breakSandInRadius(event.getBlock().getLocation(), radius, data);
            int normalBlocks = explodedCounts[0];
            int redBlocks    = explodedCounts[1];
            int soulBlocks   = explodedCounts[2];

            double sandUpgrade     = plugin.getUpgradeManager().getSandMultiplier(data);
            double rebirthMult     = plugin.getRebirthManager().getRebirthMultiplier(data);
            double eventSandBonus  = plugin.getEventManager().getSandBonus();
            double augmentSandMult = plugin.getAugmentManager().getSandMultiplier(data);
            double skillSandMult   = plugin.getSkillManager().getSandMultiplier(data);
            double baseMultiplier  = sandUpgrade * rebirthMult * (1.0 + eventSandBonus)
                    * augmentSandMult * skillSandMult;

            double redSandMult  = plugin.getSandBlockManager().getRedSandMultiplier();
            double soulSoilMult = plugin.getSandBlockManager().getSoulSoilMultiplier();

            // Apply jackpot multiplier to explosion sand if active
            double jackpotBonus = data.isSandJackpotActive() ? SAND_JACKPOT_MULTIPLIER : 1.0;

            BigDecimal explosionSand = BigDecimal.valueOf(
                    ((baseMultiplier * normalBlocks)
                            + (baseMultiplier * redSandMult  * redBlocks)
                            + (baseMultiplier * soulSoilMult * soulBlocks))
                            * jackpotBonus);
            data.addSand(explosionSand);

            int totalBlocks = normalBlocks + redBlocks + soulBlocks;
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
     * Breaks all sand-type blocks in a sphere. Returns [normalCount, redCount, soulCount].
     */
    private int[] breakSandInRadius(org.bukkit.Location center, int radius, PlayerData data) {
        int normalCount = 0;
        int redCount    = 0;
        int soulCount   = 0;
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
                        if (plugin.getSandBlockManager().isRedSand(block))       redCount++;
                        else if (plugin.getSandBlockManager().isSoulSoil(block)) soulCount++;
                        else                                                      normalCount++;
                    }
                }
            }
        }
        return new int[]{ normalCount, redCount, soulCount };
    }

    // ── Gem Drop ──────────────────────────────────────────────────────────────

    private void checkGemDrop(Player player, PlayerData data) {
        double gemChance      = plugin.getUpgradeManager().getGemChance(data);
        double eventGemBonus  = plugin.getEventManager().getGemChanceBonus() * 100.0;
        double totalGemChance = gemChance + eventGemBonus;

        if (totalGemChance <= 0) return;

        if (random.nextDouble() * 100 < totalGemChance) {
            double gemUpgradeMultiplier  = plugin.getUpgradeManager().getGemMultiplier(data);
            double augmentGemsMultiplier = plugin.getAugmentManager().getGemsMultiplier(data);
            double skillGemsMultiplier   = plugin.getSkillManager().getGemsMultiplier(data);

            double baseGems = gemUpgradeMultiplier * augmentGemsMultiplier * skillGemsMultiplier;

            // ── Gem Jackpot check ──────────────────────────────────────────
            checkGemJackpot(player, data);
            double jackpotBonus = data.isGemJackpotActive() ? GEM_JACKPOT_MULTIPLIER : 1.0;

            BigDecimal gemAmount = BigDecimal.valueOf(baseGems * jackpotBonus);
            data.addGems(gemAmount);

            String msgKey = data.isGemJackpotActive()
                    ? "messages.gem-found-jackpot"
                    : "messages.gem-found";
            plugin.getMessageManager().sendMessage(player, msgKey,
                    "%amount%", gemAmount.toPlainString());
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
        }
    }

    // ── Gem Jackpot activation ────────────────────────────────────────────────

    private void checkGemJackpot(Player player, PlayerData data) {
        if (data.isGemJackpotActive()) return;
        double chance = plugin.getUpgradeManager().getGemJackpotChance(data);
        if (chance <= 0) return;
        if (random.nextDouble() * 100.0 < chance) {
            data.activateGemJackpot(JACKPOT_DURATION_MS);
            plugin.getMessageManager().sendMessage(player, "messages.gem-jackpot-triggered");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 2.0f);
        }
    }
}