package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.data.PlayerData;
import org.bukkit.Bukkit;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final SandSimPlugin plugin;
    private final Map<LeaderboardType, List<LeaderboardEntry>> leaderboards;

    public LeaderboardManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        this.leaderboards = new ConcurrentHashMap<>();
        for (LeaderboardType type : LeaderboardType.values()) {
            leaderboards.put(type, new ArrayList<>());
        }
    }

    public void updateLeaderboards() {
        Map<UUID, PlayerData> allData = plugin.getDataManager().getAllPlayerData();
        for (LeaderboardType type : LeaderboardType.values()) {
            List<LeaderboardEntry> entries = allData.entrySet().stream()
                    .map(entry -> new LeaderboardEntry(
                            entry.getKey(),
                            Bukkit.getOfflinePlayer(entry.getKey()).getName(),
                            getValue(entry.getValue(), type)))
                    .sorted(Comparator.comparing(LeaderboardEntry::getValue).reversed())
                    .limit(10)
                    .collect(Collectors.toList());
            leaderboards.put(type, entries);
        }
    }

    private BigDecimal getValue(PlayerData data, LeaderboardType type) {
        return switch (type) {
            case SAND      -> data.getSand();
            case GEMS      -> data.getGems();
            case SANDBUCKS -> data.getSandbucks();
            case REBIRTHS  -> BigDecimal.valueOf(data.getRebirths());
        };
    }

    public List<LeaderboardEntry> getLeaderboard(LeaderboardType type) {
        return new ArrayList<>(leaderboards.getOrDefault(type, new ArrayList<>()));
    }

    public int getPlayerRank(UUID uuid, LeaderboardType type) {
        List<LeaderboardEntry> lb = leaderboards.get(type);
        if (lb == null) return -1;
        for (int i = 0; i < lb.size(); i++) {
            if (lb.get(i).getUuid().equals(uuid)) return i + 1;
        }
        return -1;
    }

    public enum LeaderboardType { SAND, GEMS, SANDBUCKS, REBIRTHS }

    public static class LeaderboardEntry {
        private final UUID uuid;
        private final String playerName;
        private final BigDecimal value;

        public LeaderboardEntry(UUID uuid, String playerName, BigDecimal value) {
            this.uuid       = uuid;
            this.playerName = playerName;
            this.value      = value;
        }

        public UUID getUuid()          { return uuid; }
        public String getPlayerName()  { return playerName != null ? playerName : "Unknown"; }
        public BigDecimal getValue()   { return value; }
    }
}