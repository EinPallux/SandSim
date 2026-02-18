package com.pallux.sandsim;

import com.pallux.sandsim.commands.*;
import com.pallux.sandsim.config.ConfigManager;
import com.pallux.sandsim.data.DataManager;
import com.pallux.sandsim.expansion.SandSimExpansion;
import com.pallux.sandsim.listeners.*;
import com.pallux.sandsim.manager.*;
import com.pallux.sandsim.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SandSimPlugin extends JavaPlugin {

    private static SandSimPlugin instance;
    private ConfigManager configManager;
    private DataManager dataManager;
    private MessageManager messageManager;
    private ShovelManager shovelManager;
    private MenuItemManager menuItemManager;
    private UpgradeManager upgradeManager;
    private RebirthManager rebirthManager;
    private FactoryManager factoryManager;
    private LeaderboardManager leaderboardManager;
    private SandBlockManager sandBlockManager;
    private EventManager eventManager;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager      = new ConfigManager(this);
        this.dataManager        = new DataManager(this);
        this.messageManager     = new MessageManager(this);
        this.shovelManager      = new ShovelManager(this);
        this.menuItemManager    = new MenuItemManager(this);
        this.upgradeManager     = new UpgradeManager(this);
        this.rebirthManager     = new RebirthManager(this);
        this.factoryManager     = new FactoryManager(this);
        this.leaderboardManager = new LeaderboardManager(this);
        this.sandBlockManager   = new SandBlockManager(this);

        // Load configs first
        configManager.loadConfigs();

        // EventManager must be created AFTER configs are loaded
        this.eventManager = new EventManager(this);

        dataManager.loadAllData();

        registerListeners();
        registerCommands();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SandSimExpansion(this).register();
            getLogger().info("PlaceholderAPI hooked successfully!");
        }

        startAsyncTasks();

        getLogger().info(ColorUtils.colorize("&a[SandSim] Plugin enabled successfully!"));
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        if (dataManager != null) {
            dataManager.saveAllData();
        }
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info(ColorUtils.colorize("&c[SandSim] Plugin disabled successfully!"));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDropItemListener(this), this);
    }

    private void registerCommands() {
        getCommand("menu").setExecutor(new MenuCommand(this));
        getCommand("rebirth").setExecutor(new RebirthCommand(this));
        getCommand("factory").setExecutor(new FactoryCommand(this));
        getCommand("upgrades").setExecutor(new UpgradesCommand(this));
        getCommand("sandbucks").setExecutor(new SandbucksCommand(this));
        getCommand("sand").setExecutor(new SandCommand(this));
        getCommand("gems").setExecutor(new GemsCommand(this));
        getCommand("rebirths").setExecutor(new RebirthsCommand(this));
        getCommand("multiplier").setExecutor(new MultiplierCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));

        SandSimCommand sandSimCommand = new SandSimCommand(this);
        getCommand("sandsim").setExecutor(sandSimCommand);
        getCommand("sandsim").setTabCompleter(sandSimCommand);
    }

    private void startAsyncTasks() {
        // Factory production (every second)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () ->
                factoryManager.processFactoryProduction(), 20L, 20L);

        // Leaderboard update (every 5 minutes)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () ->
                leaderboardManager.updateLeaderboards(), 100L, 6000L);

        // Auto-save (every 10 minutes)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () ->
                dataManager.saveAllData(), 12000L, 12000L);
    }

    public void reload() {
        configManager.loadConfigs();
        eventManager.loadConfig();
        getLogger().info("Plugin reloaded successfully!");
    }

    // Getters
    public static SandSimPlugin getInstance()              { return instance; }
    public ConfigManager getConfigManager()                { return configManager; }
    public DataManager getDataManager()                    { return dataManager; }
    public MessageManager getMessageManager()              { return messageManager; }
    public ShovelManager getShovelManager()                { return shovelManager; }
    public MenuItemManager getMenuItemManager()            { return menuItemManager; }
    public UpgradeManager getUpgradeManager()              { return upgradeManager; }
    public RebirthManager getRebirthManager()              { return rebirthManager; }
    public FactoryManager getFactoryManager()              { return factoryManager; }
    public LeaderboardManager getLeaderboardManager()      { return leaderboardManager; }
    public SandBlockManager getSandBlockManager()          { return sandBlockManager; }
    public EventManager getEventManager()                  { return eventManager; }
}
