package com.pallux.sandsim.listeners;

import com.pallux.sandsim.SandSimPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles all general world-protection features that are toggled via config.yml
 * under the "protection:" section.
 *
 * Features:
 *   block-break-protection   – handled in BlockBreakListener (non-sand blocks)
 *   block-place-protection   – players cannot place any blocks
 *   drop-protection          – players cannot drop any items
 *   no-fall-damage           – players take no fall damage
 *   hide-server-information  – hides /plugins, /version, /about, /ver etc.
 */
public class ProtectionListener implements Listener {

    private final SandSimPlugin plugin;

    /**
     * Commands (without the leading slash, lower-cased) that reveal server
     * software / plugin information and should be hidden from non-bypass players.
     */
    private static final Set<String> INFO_COMMANDS = new HashSet<>(Arrays.asList(
            "plugins", "pl",
            "version", "ver",
            "about",
            "?",
            "icanhasbukkit",
            "bukkit:plugins", "bukkit:version", "bukkit:about",
            "paper:plugins", "paper:version", "paper:about",
            "spigot:plugins", "spigot:version", "spigot:about",
            "minecraft:version"
    ));

    public ProtectionListener(SandSimPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Block Place ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        boolean enabled = plugin.getConfigManager().getMainConfig()
                .getBoolean("protection.block-place-protection", true);
        if (!enabled) return;
        Player player = event.getPlayer();
        if (player.hasPermission("sandsim.bypass.blockplace")) return;
        event.setCancelled(true);
    }

    // ── Item Drop ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        boolean enabled = plugin.getConfigManager().getMainConfig()
                .getBoolean("protection.drop-protection", true);
        if (!enabled) return;

        Player player = event.getPlayer();
        // Always allow shovel/menu-item drops to be caught by their own listener first;
        // those listeners cancel the event and send a specific message, so if the event
        // somehow reaches here uncancelled it is a different item — still block it.
        if (player.hasPermission("sandsim.bypass.drop")) return;
        event.setCancelled(true);
    }

    // ── Fall Damage ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;

        boolean enabled = plugin.getConfigManager().getMainConfig()
                .getBoolean("protection.no-fall-damage", true);
        if (!enabled) return;
        if (player.hasPermission("sandsim.bypass.falldamage")) return;

        event.setCancelled(true);
    }

    // ── Server Information Hiding ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        boolean enabled = plugin.getConfigManager().getMainConfig()
                .getBoolean("protection.hide-server-information", true);
        if (!enabled) return;

        Player player = event.getPlayer();
        if (player.hasPermission("sandsim.bypass.information")) return;

        // Extract base command (strip leading slash, remove args, lower-case)
        String message = event.getMessage();
        if (!message.startsWith("/")) return;
        String baseCommand = message.substring(1).split("\\s+")[0].toLowerCase();

        if (INFO_COMMANDS.contains(baseCommand)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "messages.no-permission");
        }
    }
}