package com.pallux.sandsim.manager;

import com.pallux.sandsim.SandSimPlugin;
import com.pallux.sandsim.utils.ColorUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EventManager {

    private final SandSimPlugin plugin;
    private final Random random = new Random();

    // Current active event (null = no event)
    private EventType activeEvent = null;

    // Bossbar shown to all players
    private BossBar bossBar;

    // Scheduler tasks
    private BukkitTask eventStartTask;
    private BukkitTask eventEndTask;

    // Config values
    private long intervalTicks;      // how often to start an event (default 30 min)
    private long durationTicks;      // how long an event lasts (default 5 min)

    // Per-event multipliers from config
    private double sandBonus;
    private double gemChanceBonus;
    private double xpBonus;
    private double factoryProductionBonus;
    private double factorySpeedBonus;

    // Bossbar no-event text
    private String noEventText;

    public EventManager(SandSimPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        createBossBar();
        startEventScheduler();
    }

    public void loadConfig() {
        FileConfiguration cfg = plugin.getConfigManager().getEventsConfig();

        intervalTicks        = cfg.getLong("events.interval-minutes",  30) * 60L * 20L;
        durationTicks        = cfg.getLong("events.duration-minutes",   5) * 60L * 20L;

        sandBonus            = cfg.getDouble("events.bonus-sand.sand-multiplier-bonus",        0.5);
        gemChanceBonus       = cfg.getDouble("events.bonus-gem.gem-chance-bonus",              0.1);
        xpBonus              = cfg.getDouble("events.bonus-xp.xp-bonus",                      1.0);
        factoryProductionBonus = cfg.getDouble("events.bonus-factory-production.production-bonus", 0.5);
        factorySpeedBonus    = cfg.getDouble("events.bonus-factory-speed.speed-bonus",         0.5);

        noEventText          = cfg.getString("events.bossbar.no-event-text", "&7World Event: &cNone");
    }

    private void createBossBar() {
        Component title = ColorUtils.toComponent(noEventText);
        this.bossBar = BossBar.bossBar(title, 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
    }

    private void startEventScheduler() {
        eventStartTask = Bukkit.getScheduler().runTaskTimer(plugin, this::triggerRandomEvent,
                intervalTicks, intervalTicks);
    }

    public void triggerRandomEvent() {
        EventType[] types = EventType.values();
        EventType chosen = types[random.nextInt(types.length)];
        startEvent(chosen);
    }

    public void startEvent(EventType type) {
        this.activeEvent = type;

        FileConfiguration cfg = plugin.getConfigManager().getEventsConfig();
        String eventKey = "events." + type.getConfigKey() + ".bossbar-text";
        String defaultText = type.getDefaultBossbarText();
        String text = cfg.getString(eventKey, defaultText);

        // Update bossbar
        bossBar.name(ColorUtils.toComponent(text));
        bossBar.color(type.getBossbarColor());
        bossBar.progress(1.0f);

        // Show bossbar to all online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bossBar);
        }

        // Announce event
        String announceKey = "events." + type.getConfigKey() + ".announce-message";
        String announceDefault = "&6&l[EVENT] &e" + type.getDisplayName() + " &7has started! Duration: &e" +
                (durationTicks / 20 / 60) + " minutes";
        String announceMsg = cfg.getString(announceKey, announceDefault);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ColorUtils.colorize(
                    plugin.getMessageManager().getPrefix() + announceMsg));
        }

        // Cancel previous end task if any
        if (eventEndTask != null) eventEndTask.cancel();

        // Schedule end
        eventEndTask = Bukkit.getScheduler().runTaskLater(plugin, this::endEvent, durationTicks);
    }

    public void endEvent() {
        this.activeEvent = null;

        String noEvent = plugin.getConfigManager().getEventsConfig()
                .getString("events.bossbar.no-event-text", noEventText);
        bossBar.name(ColorUtils.toComponent(noEvent));
        bossBar.color(BossBar.Color.WHITE);
        bossBar.progress(1.0f);

        // Keep bossbar visible (showing "no event")
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bossBar);
        }

        eventEndTask = null;
    }

    /** Call when a new player joins so they see the bossbar. */
    public void showBossBarToPlayer(Player player) {
        player.showBossBar(bossBar);
    }

    public void shutdown() {
        if (eventStartTask != null) eventStartTask.cancel();
        if (eventEndTask   != null) eventEndTask.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(bossBar);
        }
    }

    // ---- Active event query methods ----

    public boolean hasActiveEvent() {
        return activeEvent != null;
    }

    public EventType getActiveEvent() {
        return activeEvent;
    }

    /** Extra sand multiplier bonus (additive on top, e.g. 0.5 = +50%) */
    public double getSandBonus() {
        if (activeEvent == EventType.BONUS_SAND) return sandBonus;
        return 0.0;
    }

    /** Extra gem chance bonus (additive, e.g. 0.1 = +10%) */
    public double getGemChanceBonus() {
        if (activeEvent == EventType.BONUS_GEM) return gemChanceBonus;
        return 0.0;
    }

    /** Extra XP per block broken (additive, e.g. 1 = +1 XP per block) */
    public double getXpBonus() {
        if (activeEvent == EventType.BONUS_XP) return xpBonus;
        return 0.0;
    }

    /** Extra factory production multiplier bonus (additive, e.g. 0.5 = +50%) */
    public double getFactoryProductionBonus() {
        if (activeEvent == EventType.BONUS_FACTORY_PRODUCTION) return factoryProductionBonus;
        return 0.0;
    }

    /** Extra factory speed bonus — reduces interval. A bonus of 0.5 = 50% faster (halved interval). */
    public double getFactorySpeedBonus() {
        if (activeEvent == EventType.BONUS_FACTORY_SPEED) return factorySpeedBonus;
        return 0.0;
    }

    // ---- Event Type Enum ----

    public enum EventType {
        BONUS_SAND("bonus-sand", "Bonus Sand Event", "&e&l✦ BONUS SAND EVENT &7- &a+50% Sand per Block!", BossBar.Color.YELLOW),
        BONUS_GEM("bonus-gem",   "Bonus Gem Event",  "&b&l✦ BONUS GEM EVENT &7- &a+10% Gem Chance!",     BossBar.Color.BLUE),
        BONUS_XP("bonus-xp",    "Bonus XP Event",   "&a&l✦ BONUS XP EVENT &7- &a+1 XP per Block!",      BossBar.Color.GREEN),
        BONUS_FACTORY_PRODUCTION("bonus-factory-production", "Bonus Factory Production",
                "&6&l✦ BONUS FACTORY PRODUCTION &7- &a+50% Production Amount!", BossBar.Color.YELLOW),
        BONUS_FACTORY_SPEED("bonus-factory-speed", "Bonus Factory Speed",
                "&6&l✦ BONUS FACTORY SPEED &7- &a+50% Factory Speed!", BossBar.Color.PINK);

        private final String configKey;
        private final String displayName;
        private final String defaultBossbarText;
        private final BossBar.Color bossbarColor;

        EventType(String configKey, String displayName, String defaultBossbarText, BossBar.Color color) {
            this.configKey          = configKey;
            this.displayName        = displayName;
            this.defaultBossbarText = defaultBossbarText;
            this.bossbarColor       = color;
        }

        public String getConfigKey()          { return configKey; }
        public String getDisplayName()        { return displayName; }
        public String getDefaultBossbarText() { return defaultBossbarText; }
        public BossBar.Color getBossbarColor(){ return bossbarColor; }
    }

    // Getter for use in expansion
    public double getSandBonusRaw()            { return sandBonus; }
    public double getGemChanceBonusRaw()       { return gemChanceBonus; }
    public double getXpBonusRaw()              { return xpBonus; }
    public double getFactoryProductionBonusRaw(){ return factoryProductionBonus; }
    public double getFactorySpeedBonusRaw()    { return factorySpeedBonus; }
    public long getDurationTicks()             { return durationTicks; }
    public long getIntervalTicks()             { return intervalTicks; }
}
