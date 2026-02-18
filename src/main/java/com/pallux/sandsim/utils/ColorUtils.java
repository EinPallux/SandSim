package com.pallux.sandsim.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return message;
        message = translateHexColorCodes(message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String translateHexColorCodes(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + group).toString());
        }
        return matcher.appendTail(buffer).toString();
    }

    public static Component toComponent(String message) {
        return LEGACY_SERIALIZER.deserialize(colorize(message));
    }

    public static String stripColors(String message) {
        if (message == null || message.isEmpty()) return message;
        return ChatColor.stripColor(colorize(message));
    }
}
