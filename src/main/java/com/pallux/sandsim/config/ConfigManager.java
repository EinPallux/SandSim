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
        // Core configs
        saveDefaultConfig("config.yml");
        saveDefaultConfig("messages.yml");
        saveDefaultConfig("upgrades.yml");
        saveDefaultConfig("events.yml");
        saveDefaultConfig("augments.yml");
        saveDefaultConfig("skills.yml");
        saveDefaultConfig("items.yml");

        // GUI configs — one per GUI screen
        saveDefaultConfig("gui.yml");              // Dashboard / main menu only
        saveDefaultConfig("augments-gui.yml");     // Augments GUI
        saveDefaultConfig("upgrades-gui.yml");     // Upgrades GUI
        saveDefaultConfig("factory-gui.yml");      // Factory GUI
        saveDefaultConfig("leaderboard-gui.yml");  // Leaderboard GUI
        saveDefaultConfig("skilltree-gui.yml");    // Skill Tree GUI
        saveDefaultConfig("admin-gui.yml");        // Admin GUI

        // Load all of them
        loadConfig("config.yml");
        loadConfig("messages.yml");
        loadConfig("upgrades.yml");
        loadConfig("events.yml");
        loadConfig("augments.yml");
        loadConfig("skills.yml");
        loadConfig("items.yml");
        loadConfig("gui.yml");
        loadConfig("augments-gui.yml");
        loadConfig("upgrades-gui.yml");
        loadConfig("factory-gui.yml");
        loadConfig("leaderboard-gui.yml");
        loadConfig("skilltree-gui.yml");
        loadConfig("admin-gui.yml");
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

    // ── Convenience getters ───────────────────────────────────────────────────

    public FileConfiguration getMainConfig()        { return getConfig("config.yml"); }
    public FileConfiguration getMessagesConfig()    { return getConfig("messages.yml"); }
    public FileConfiguration getUpgradesConfig()    { return getConfig("upgrades.yml"); }
    public FileConfiguration getEventsConfig()      { return getConfig("events.yml"); }
    public FileConfiguration getAugmentsConfig()    { return getConfig("augments.yml"); }
    public FileConfiguration getSkillsConfig()      { return getConfig("skills.yml"); }
    public FileConfiguration getItemsConfig()       { return getConfig("items.yml"); }

    // GUI-specific configs
    public FileConfiguration getGuiConfig()         { return getConfig("gui.yml"); }
    public FileConfiguration getAugmentsGuiConfig() { return getConfig("augments-gui.yml"); }
    public FileConfiguration getUpgradesGuiConfig() { return getConfig("upgrades-gui.yml"); }
    public FileConfiguration getFactoryGuiConfig()  { return getConfig("factory-gui.yml"); }
    public FileConfiguration getLeaderboardGuiConfig() { return getConfig("leaderboard-gui.yml"); }
    public FileConfiguration getSkillTreeGuiConfig(){ return getConfig("skilltree-gui.yml"); }
    public FileConfiguration getAdminGuiConfig()    { return getConfig("admin-gui.yml"); }
}