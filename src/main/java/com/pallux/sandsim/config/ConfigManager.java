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

    /** Sub-folder (relative to the plugin data folder) for all GUI configs. */
    private static final String GUI_FOLDER = "gui_menus";

    public ConfigManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
    }

    public void loadConfigs() {
        // ── Core (non-GUI) configs ────────────────────────────────────────────
        saveDefaultConfig("config.yml",   null);
        saveDefaultConfig("messages.yml", null);
        saveDefaultConfig("events.yml",   null);
        saveDefaultConfig("items.yml",    null);

        // ── GUI configs (stored in gui_menus/ subfolder) ──────────────────────
        // gui.yml          – Dashboard / main menu
        // augments-gui.yml – Augments GUI + augment tier data (replaces augments.yml)
        // upgrades-gui.yml – Upgrades GUI + upgrade costs   (replaces upgrades.yml)
        // skilltree-gui.yml– Skill Tree GUI + skill data    (replaces skills.yml)
        // factory-gui.yml  – Factory GUI
        // leaderboard-gui.yml – Leaderboard GUI
        // admin-gui.yml    – Admin GUI
        saveDefaultConfig("gui.yml",              GUI_FOLDER);
        saveDefaultConfig("augments-gui.yml",     GUI_FOLDER);
        saveDefaultConfig("upgrades-gui.yml",     GUI_FOLDER);
        saveDefaultConfig("skilltree-gui.yml",    GUI_FOLDER);
        saveDefaultConfig("factory-gui.yml",      GUI_FOLDER);
        saveDefaultConfig("leaderboard-gui.yml",  GUI_FOLDER);
        saveDefaultConfig("admin-gui.yml",         GUI_FOLDER);

        // Load everything
        loadConfig("config.yml",   null);
        loadConfig("messages.yml", null);
        loadConfig("events.yml",   null);
        loadConfig("items.yml",    null);

        loadConfig("gui.yml",             GUI_FOLDER);
        loadConfig("augments-gui.yml",    GUI_FOLDER);
        loadConfig("upgrades-gui.yml",    GUI_FOLDER);
        loadConfig("skilltree-gui.yml",   GUI_FOLDER);
        loadConfig("factory-gui.yml",     GUI_FOLDER);
        loadConfig("leaderboard-gui.yml", GUI_FOLDER);
        loadConfig("admin-gui.yml",       GUI_FOLDER);
    }

    /**
     * Copies the resource file to the plugin data folder (with optional subfolder)
     * if it does not already exist on disk.
     *
     * @param fileName  the file name (e.g. "gui.yml")
     * @param subFolder the subfolder inside the plugin data folder, or {@code null}
     *                  for the root data folder
     */
    private void saveDefaultConfig(String fileName, String subFolder) {
        File targetDir = (subFolder == null)
                ? plugin.getDataFolder()
                : new File(plugin.getDataFolder(), subFolder);
        File file = new File(targetDir, fileName);

        if (!file.exists()) {
            targetDir.mkdirs();
            // Resource path: if it lives in gui_menus/ the jar must mirror that path
            String resourcePath = (subFolder == null) ? fileName : subFolder + "/" + fileName;
            try (InputStream in = plugin.getResource(resourcePath)) {
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

    private void loadConfig(String fileName, String subFolder) {
        File targetDir = (subFolder == null)
                ? plugin.getDataFolder()
                : new File(plugin.getDataFolder(), subFolder);
        File file = new File(targetDir, fileName);

        if (!file.exists()) {
            saveDefaultConfig(fileName, subFolder);
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

    /** Root configs */
    public FileConfiguration getMainConfig()     { return getConfig("config.yml"); }
    public FileConfiguration getMessagesConfig() { return getConfig("messages.yml"); }
    public FileConfiguration getEventsConfig()   { return getConfig("events.yml"); }
    public FileConfiguration getItemsConfig()    { return getConfig("items.yml"); }

    /** GUI configs (all in gui_menus/) */
    public FileConfiguration getGuiConfig()            { return getConfig("gui.yml"); }
    public FileConfiguration getAugmentsGuiConfig()    { return getConfig("augments-gui.yml"); }
    public FileConfiguration getUpgradesGuiConfig()    { return getConfig("upgrades-gui.yml"); }
    public FileConfiguration getSkillTreeGuiConfig()   { return getConfig("skilltree-gui.yml"); }
    public FileConfiguration getFactoryGuiConfig()     { return getConfig("factory-gui.yml"); }
    public FileConfiguration getLeaderboardGuiConfig() { return getConfig("leaderboard-gui.yml"); }
    public FileConfiguration getAdminGuiConfig()       { return getConfig("admin-gui.yml"); }

    /**
     * Backward-compatibility aliases so that managers that previously called
     * getAugmentsConfig() / getUpgradesConfig() / getSkillsConfig() continue to
     * compile and receive the merged file.
     */
    public FileConfiguration getAugmentsConfig() { return getAugmentsGuiConfig(); }
    public FileConfiguration getUpgradesConfig() { return getUpgradesGuiConfig(); }
    public FileConfiguration getSkillsConfig()   { return getSkillTreeGuiConfig(); }
}
