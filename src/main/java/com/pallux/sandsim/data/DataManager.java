package com.pallux.sandsim.data;

import com.pallux.sandsim.SandSimPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final SandSimPlugin plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private final File dataFolder;

    public DataManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        this.playerDataMap = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, k -> {
            PlayerData data = loadPlayerData(uuid);
            return data != null ? data : new PlayerData(uuid);
        });
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;

        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        Map<String, Object> serialized = data.serialize();
        for (Map.Entry<String, Object> entry : serialized.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for " + uuid + ": " + e.getMessage());
        }
    }

    public void savePlayerData(Player player) {
        savePlayerData(player.getUniqueId());
    }

    public PlayerData loadPlayerData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        if (!playerFile.exists()) return null;

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        Map<String, Object> data = new ConcurrentHashMap<>();
        for (String key : config.getKeys(false)) {
            data.put(key, config.get(key));
        }
        return PlayerData.deserialize(data);
    }

    public void unloadPlayerData(UUID uuid) {
        savePlayerData(uuid);
        playerDataMap.remove(uuid);
    }

    public void unloadPlayerData(Player player) {
        unloadPlayerData(player.getUniqueId());
    }

    public void saveAllData() {
        for (UUID uuid : playerDataMap.keySet()) savePlayerData(uuid);
    }

    public void loadAllData() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            getPlayerData(player.getUniqueId());
        }
    }

    public Map<UUID, PlayerData> getAllPlayerData() {
        Map<UUID, PlayerData> allData = new ConcurrentHashMap<>();
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName().replace(".yml", "");
                try {
                    UUID uuid = UUID.fromString(fileName);
                    PlayerData data = playerDataMap.get(uuid);
                    if (data == null) data = loadPlayerData(uuid);
                    if (data != null) allData.put(uuid, data);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return allData;
    }
}
