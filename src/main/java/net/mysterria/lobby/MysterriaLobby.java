package net.mysterria.lobby;

import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import net.mysterria.lobby.commands.LobbyCommands;
import net.mysterria.lobby.commands.RulesCommand;
import net.mysterria.lobby.commands.ZonesCommand;
import net.mysterria.lobby.config.ConfigManager;
import net.mysterria.lobby.config.LangManager;
import net.mysterria.lobby.domain.items.JoinItemManager;
import net.mysterria.lobby.domain.player.PlayerVisibilityManager;
import net.mysterria.lobby.domain.protection.WorldProtectionManager;
import net.mysterria.lobby.domain.spawn.SpawnManager;
import net.mysterria.lobby.domain.zones.TeleportManager;
import net.mysterria.lobby.feature.actionbar.ActionBarManager;
import net.mysterria.lobby.feature.announcement.AnnouncementManager;
import net.mysterria.lobby.feature.bossbar.BossBarManager;
import net.mysterria.lobby.feature.doublejump.DoubleJumpManager;
import net.mysterria.lobby.gui.GuiManager;
import net.mysterria.lobby.listeners.*;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class MysterriaLobby extends JavaPlugin {

    private static MysterriaLobby instance;

    private ConfigManager configManager;
    private LangManager langManager;
    private GuiManager guiManager;
    private TeleportManager teleportManager;
    private PlayerVisibilityManager playerVisibilityManager;
    private WorldProtectionManager worldProtectionManager;
    private JoinItemManager joinItemManager;
    private SpawnManager spawnManager;
    private RulesCommand rulesCommand;

    private ActionBarManager actionBarManager;
    private AnnouncementManager announcementManager;
    private BossBarManager bossBarManager;
    private DoubleJumpManager doubleJumpManager;

    private LiteCommands<CommandSender> liteCommands;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.langManager = new LangManager(this);
        this.guiManager = new GuiManager(this);
        this.teleportManager = new TeleportManager(this);
        this.playerVisibilityManager = new PlayerVisibilityManager(this);
        this.worldProtectionManager = new WorldProtectionManager(this);
        this.joinItemManager = new JoinItemManager(this);
        this.spawnManager = new SpawnManager(this);
        this.rulesCommand = new RulesCommand(this);

        this.actionBarManager = new ActionBarManager(this);
        this.announcementManager = new AnnouncementManager(this);
        this.bossBarManager = new BossBarManager(this);
        this.doubleJumpManager = new DoubleJumpManager(this);

        setupCommands();
        registerListeners();

        getLogger().info("MysterriaLobby has been enabled!");
    }

    @Override
    public void onDisable() {
        if (liteCommands != null) liteCommands.unregister();
        if (teleportManager != null) teleportManager.cancelAllTeleports();
        if (actionBarManager != null) actionBarManager.stop();
        if (announcementManager != null) announcementManager.stop();
        if (bossBarManager != null) bossBarManager.stop();
        getLogger().info("MysterriaLobby has been disabled!");
    }

    private void setupCommands() {
        this.liteCommands = LiteBukkitFactory.builder("mysterria-lobby", this)
                .commands(new LobbyCommands(this), new ZonesCommand(this), rulesCommand)
                .build();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new VoidDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new JumpPadListener(), this);
        getServer().getPluginManager().registerEvents(new DoubleJumpListener(this), this);
    }

    public void reload() {
        configManager.reload();
        langManager.reload();
        guiManager.reload();
        teleportManager.reload();
        playerVisibilityManager.reload();
        worldProtectionManager.reload();
        spawnManager.reload();
        rulesCommand.reload();
        actionBarManager.reload();
        announcementManager.reload();
        bossBarManager.reload();
        doubleJumpManager.reload();
    }

    public static MysterriaLobby getInstance() { return instance; }

    public ConfigManager getConfigManager() { return configManager; }
    public LangManager getLangManager() { return langManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public TeleportManager getTeleportManager() { return teleportManager; }
    public PlayerVisibilityManager getPlayerVisibilityManager() { return playerVisibilityManager; }
    public WorldProtectionManager getWorldProtectionManager() { return worldProtectionManager; }
    public JoinItemManager getJoinItemManager() { return joinItemManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public RulesCommand getRulesCommand() { return rulesCommand; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
    public AnnouncementManager getAnnouncementManager() { return announcementManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public DoubleJumpManager getDoubleJumpManager() { return doubleJumpManager; }
}
