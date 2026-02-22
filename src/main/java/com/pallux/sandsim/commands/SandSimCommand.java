package com.pallux.sandsim.commands;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import com.pallux.sandsim.data.PlayerData.UpgradeType;
import com.pallux.sandsim.gui.AdminGUI;
import com.pallux.sandsim.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SandSimCommand implements CommandExecutor, TabCompleter {

    private final SandSimPlugin plugin;

    public SandSimCommand(SandSimPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player && player.hasPermission("sandsim.admin")) {
                new AdminGUI(plugin).open(player);
                return true;
            }
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload"              -> { return handleReload(sender); }
            case "give"                -> { return handleGive(sender, args); }
            case "take"                -> { return handleTake(sender, args); }
            case "set"                 -> { return handleSet(sender, args); }
            case "reset"               -> { return handleReset(sender, args); }
            case "resetallcurrencies"  -> { return handleResetAllCurrencies(sender, args); }
            case "restart"             -> { return handleRestart(sender, args); }
            case "help"                -> { return handleHelp(sender); }
            case "admin"               -> { return handleAdmin(sender); }
            case "upgrades"            -> { return handleUpgrades(sender, args); }
            default -> { plugin.getMessageManager().sendMessage(sender, "messages.unknown-subcommand"); return true; }
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("sandsim.admin")) { plugin.getMessageManager().sendMessage(sender, "messages.no-permission"); return true; }
        plugin.reload();
        plugin.getMessageManager().sendMessage(sender, "messages.reload-success");
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sandsim.admin")) { plugin.getMessageManager().sendMessage(sender, "messages.no-permission"); return true; }
        if (args.length < 4) { plugin.getMessageManager().sendMessage(sender, "messages.usage-give"); return true; }

        String currencyType = args[1].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!target.hasPlayedBefore() && !target.isOnline()) { plugin.getMessageManager().sendMessage(sender, "messages.player-never-played"); return true; }

        BigDecimal amount;
        try { amount = new BigDecimal(args[3]); }
        catch (NumberFormatException e) { plugin.getMessageManager().sendMessage(sender, "messages.invalid-amount"); return true; }

        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        switch (currencyType) {
            case "sand"      -> data.addSand(amount);
            case "gems"      -> data.addGems(amount);
            case "sandbucks" -> data.addSandbucks(amount);
            case "rebirths"  -> data.addRebirths(amount.longValue());
            default -> { plugin.getMessageManager().sendMessage(sender, "messages.invalid-currency"); return true; }
        }
        plugin.getDataManager().savePlayerData(target.getUniqueId());
        plugin.getMessageManager().sendMessage(sender, "messages.gave-currency", "%amount%", amount.toPlainString(), "%currency%", currencyType, "%player%", target.getName());
        return true;
    }

    private boolean handleTake(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sandsim.admin")) { plugin.getMessageManager().sendMessage(sender, "messages.no-permission"); return true; }
        if (args.length < 4) { plugin.getMessageManager().sendMessage(sender, "messages.usage-take"); return true; }

        String currencyType = args[1].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!target.hasPlayedBefore() && !target.isOnline()) { plugin.getMessageManager().sendMessage(sender, "messages.player-never-played"); return true; }

        BigDecimal amount;
        try { amount = new BigDecimal(args[3]); }
        catch (NumberFormatException e) { plugin.getMessageManager().sendMessage(sender, "messages.invalid-amount"); return true; }

        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        switch (currencyType) {
            case "sand"      -> data.removeSand(amount);
            case "gems"      -> data.removeGems(amount);
            case "sandbucks" -> data.removeSandbucks(amount);
            case "rebirths"  -> data.setRebirths(Math.max(0L, data.getRebirths() - amount.longValue()));
            default -> { plugin.getMessageManager().sendMessage(sender, "messages.invalid-currency"); return true; }
        }
        plugin.getDataManager().savePlayerData(target.getUniqueId());
        plugin.getMessageManager().sendMessage(sender, "messages.took-currency", "%amount%", amount.toPlainString(), "%currency%", currencyType, "%player%", target.getName());
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sandsim.admin")) { plugin.getMessageManager().sendMessage(sender, "messages.no-permission"); return true; }
        if (args.length < 4) { plugin.getMessageManager().sendMessage(sender, "messages.usage-set"); return true; }

        String currencyType = args[1].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!target.hasPlayedBefore() && !target.isOnline()) { plugin.getMessageManager().sendMessage(sender, "messages.player-never-played"); return true; }

        BigDecimal amount;
        try { amount = new BigDecimal(args[3]); }
        catch (NumberFormatException e) { plugin.getMessageManager().sendMessage(sender, "messages.invalid-amount"); return true; }

        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        switch (currencyType) {
            case "sand"      -> data.setSand(amount);
            case "gems"      -> data.setGems(amount);
            case "sandbucks" -> data.setSandbucks(amount);
            case "rebirths"  -> data.setRebirths(amount.longValue());
            default -> { plugin.getMessageManager().sendMessage(sender, "messages.invalid-currency"); return true; }
        }
        plugin.getDataManager().savePlayerData(target.getUniqueId());
        plugin.getMessageManager().sendMessage(sender, "messages.set-currency", "%currency%", currencyType, "%amount%", amount.toPlainString(), "%player%", target.getName());
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sandsim.admin")) { plugin.getMessageManager().sendMessage(sender, "messages.no-permission"); return true; }
        if (args.length < 3) { plugin.getMessageManager().sendMessage(sender, "messages.usage-reset"); return true; }

        String currencyType = args[1].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!target.hasPlayedBefore() && !target.isOnline()) { plugin.getMessageManager().sendMessage(sender, "messages.player-never-played"); return true; }

        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        switch (currencyType) {
            case "sand"      -> data.setSand(BigDecimal.ZERO);
            case "gems"      -> data.setGems(BigDecimal.ZERO);
            case "sandbucks" -> data.setSandbucks(BigDecimal.ZERO);
            case "rebirths"  -> data.setRebirths(0L);
            default -> { plugin.getMessageManager().sendMessage(sender, "messages.invalid-currency"); return true; }
        }
        plugin.getDataManager().savePlayerData(target.getUniqueId());
        plugin.getMessageManager().sendMessage(sender, "messages.reset-currency", "%currency%", currencyType, "%player%", target.getName());
        return true;
    }

    private boolean handleResetAllCurrencies(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sandsim.admin")) { plugin.getMessageManager().sendMessage(sender, "messages.no-permission"); return true; }
        if (args.length < 2) { plugin.getMessageManager().sendMessage(sender, "messages.usage-reset-all"); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) { plugin.getMessageManager().sendMessage(sender, "messages.player-never-played"); return true; }
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        data.setSand(BigDecimal.ZERO);
        data.setGems(BigDecimal.ZERO);
        data.setSandbucks(BigDecimal.ZERO);
        data.setRebirths(0L);
        plugin.getDataManager().savePlayerData(target.getUniqueId());
        plugin.getMessageManager().sendMessage(sender, "messages.reset-all-currencies", "%player%", target.getName());
        return true;
    }

    private boolean handleRestart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sandsim.admin")) { plugin.getMessageManager().sendMessage(sender, "messages.no-permission"); return true; }
        if (args.length < 2) { plugin.getMessageManager().sendMessage(sender, "messages.usage-restart"); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) { plugin.getMessageManager().sendMessage(sender, "messages.player-never-played"); return true; }
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        data.resetAll();
        // If the target is online, remove the Speed effect
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            onlineTarget.removePotionEffect(PotionEffectType.SPEED);
        }
        plugin.getDataManager().savePlayerData(target.getUniqueId());
        plugin.getMessageManager().sendMessage(sender, "messages.restart-player", "%player%", target.getName());
        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        if (sender instanceof Player player && player.hasPermission("sandsim.admin")) new AdminGUI(plugin).open(player);
        else sendHelpMessage(sender);
        return true;
    }

    private boolean handleAdmin(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only!"); return true; }
        if (!player.hasPermission("sandsim.admin")) { plugin.getMessageManager().sendMessage(player, "messages.no-permission"); return true; }
        new AdminGUI(plugin).open(player);
        return true;
    }

    private boolean handleUpgrades(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sandsim.admin")) { plugin.getMessageManager().sendMessage(sender, "messages.no-permission"); return true; }
        if (args.length < 5 || !args[1].equalsIgnoreCase("set")) { plugin.getMessageManager().sendMessage(sender, "messages.usage-upgrades"); return true; }

        String upgradeTypeName = args[2].toUpperCase().replace("-", "_");
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        if (!target.hasPlayedBefore() && !target.isOnline()) { plugin.getMessageManager().sendMessage(sender, "messages.player-never-played"); return true; }

        int amount;
        try { amount = Integer.parseInt(args[4]); }
        catch (NumberFormatException e) { plugin.getMessageManager().sendMessage(sender, "messages.invalid-amount"); return true; }

        UpgradeType upgradeType;
        try { upgradeType = UpgradeType.valueOf(upgradeTypeName); }
        catch (IllegalArgumentException e) { plugin.getMessageManager().sendMessage(sender, "messages.invalid-upgrade"); return true; }

        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        data.setUpgradeLevel(upgradeType, amount);

        // If admin sets speed upgrade, sync effect for online players
        if (upgradeType == UpgradeType.SPEED) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                if (amount >= 1) {
                    onlineTarget.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false, false));
                } else {
                    onlineTarget.removePotionEffect(PotionEffectType.SPEED);
                }
            }
        }

        plugin.getDataManager().savePlayerData(target.getUniqueId());
        plugin.getMessageManager().sendMessage(sender, "messages.set-upgrade", "%upgrade%", upgradeType.name(), "%level%", String.valueOf(amount), "%player%", target.getName());
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getMessage("messages.admin-help-header")));
        String p = plugin.getMessageManager().getPrefix();
        sender.sendMessage(ColorUtils.colorize(p + "&e/sandsim reload &7- Reload the plugin"));
        sender.sendMessage(ColorUtils.colorize(p + "&e/sandsim give <currency> <player> <amount> &7- Give currency"));
        sender.sendMessage(ColorUtils.colorize(p + "&e/sandsim take <currency> <player> <amount> &7- Take currency"));
        sender.sendMessage(ColorUtils.colorize(p + "&e/sandsim set <currency> <player> <amount> &7- Set currency"));
        sender.sendMessage(ColorUtils.colorize(p + "&e/sandsim reset <currency> <player> &7- Reset currency"));
        sender.sendMessage(ColorUtils.colorize(p + "&e/sandsim resetallcurrencies <player> &7- Reset all currencies"));
        sender.sendMessage(ColorUtils.colorize(p + "&e/sandsim restart <player> &7- Complete player reset"));
        sender.sendMessage(ColorUtils.colorize(p + "&e/sandsim upgrades set <upgrade> <player> <level> &7- Set upgrade level"));
        sender.sendMessage(ColorUtils.colorize(p + "&e/sandsim admin &7- Open admin GUI"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("sandsim.admin")) return completions;

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload","give","take","set","reset","resetallcurrencies","restart","help","admin","upgrades"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")||args[0].equalsIgnoreCase("take")||args[0].equalsIgnoreCase("set")||args[0].equalsIgnoreCase("reset"))
                completions.addAll(Arrays.asList("sand","gems","sandbucks","rebirths"));
            else if (args[0].equalsIgnoreCase("upgrades")) completions.add("set");
            else if (args[0].equalsIgnoreCase("resetallcurrencies")||args[0].equalsIgnoreCase("restart"))
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(s->s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("upgrades")&&args[1].equalsIgnoreCase("set")) {
                for (UpgradeType t : UpgradeType.values()) completions.add(t.name().toLowerCase().replace("_","-"));
            } else {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(s->s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("upgrades")&&args[1].equalsIgnoreCase("set"))
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(s->s.toLowerCase().startsWith(args[3].toLowerCase())).collect(Collectors.toList());
            else completions.addAll(Arrays.asList("1","10","100","1000","10000"));
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("upgrades")&&args[1].equalsIgnoreCase("set"))
                completions.addAll(Arrays.asList("0","1"));
        }

        return completions.stream().filter(s->s.toLowerCase().startsWith(args[args.length-1].toLowerCase())).collect(Collectors.toList());
    }
}