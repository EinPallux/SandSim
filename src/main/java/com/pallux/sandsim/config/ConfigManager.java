package com.pallux.sandsim.config;

import com.pallux.sandsim.SandSimPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final SandSimPlugin plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;

    public ConfigManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
    }

    public void loadConfigs() {
        saveDefaultConfig("config.yml");
        saveDefaultConfig("messages.yml");
        saveDefaultConfig("gui.yml");
        saveDefaultConfig("items.yml");
        saveDefaultConfig("upgrades.yml");
        saveDefaultConfig("events.yml");

        loadConfig("config.yml");
        loadConfig("messages.yml");
        loadConfig("gui.yml");
        loadConfig("items.yml");
        loadConfig("upgrades.yml");
        loadConfig("events.yml");
    }

    private void saveDefaultConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = plugin.getResource(fileName)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save default config " + fileName + ": " + e.getMessage());
            }
        }
    }

    private void loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            saveDefaultConfig(fileName);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(fileName, config);
        configFiles.put(fileName, file);
    }

    public FileConfiguration getConfig(String fileName) {
        return configs.getOrDefault(fileName, plugin.getConfig());
    }

    public void reloadConfig(String fileName) {
        File file = configFiles.get(fileName);
        if (file != null && file.exists()) {
            configs.put(fileName, YamlConfiguration.loadConfiguration(file));
        }
    }

    public void saveConfig(String fileName) {
        FileConfiguration config = configs.get(fileName);
        File file = configFiles.get(fileName);
        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save config " + fileName + ": " + e.getMessage());
            }
        }
    }

    // Convenience getters
    public FileConfiguration getMainConfig()     { return getConfig("config.yml"); }
    public FileConfiguration getMessagesConfig() { return getConfig("messages.yml"); }
    public FileConfiguration getGuiConfig()      { return getConfig("gui.yml"); }
    public FileConfiguration getItemsConfig()    { return getConfig("items.yml"); }
    public FileConfiguration getUpgradesConfig() { return getConfig("upgrades.yml"); }
    public FileConfiguration getEventsConfig()   { return getConfig("events.yml"); }
}
