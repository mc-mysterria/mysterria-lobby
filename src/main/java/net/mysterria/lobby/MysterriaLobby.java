package net.mysterria.lobby;

import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import net.mysterria.lobby.commands.LobbyCommands;
import net.mysterria.lobby.commands.RulesCommand;
import net.mysterria.lobby.commands.TeleportCommand;
import net.mysterria.lobby.config.ConfigManager;
import net.mysterria.lobby.domain.items.JoinItemManager;
import net.mysterria.lobby.config.LangManager;
import net.mysterria.lobby.listeners.*;
import net.mysterria.lobby.gui.TriumphMenuManager;
import net.mysterria.lobby.domain.player.PlayerVisibilityManager;
import net.mysterria.lobby.domain.protection.WorldProtectionManager;
import net.mysterria.lobby.domain.zones.TeleportManager;
import net.mysterria.lobby.domain.spawn.SpawnManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class MysterriaLobby extends JavaPlugin {

    private static MysterriaLobby instance;
    private ConfigManager configManager;
    private LangManager langManager;
    private TriumphMenuManager triumphMenuManager;
    private TeleportManager teleportManager;
    private PlayerVisibilityManager playerVisibilityManager;
    private WorldProtectionManager worldProtectionManager;
    private JoinItemManager joinItemManager;
    private SpawnManager spawnManager;
    private RulesCommand rulesCommand;
    private LiteCommands<org.bukkit.command.CommandSender> liteCommands;

    public static final NamespacedKey LANG_KEY = new NamespacedKey("mysterria", "lang");

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.langManager = new LangManager(this);
        this.triumphMenuManager = new TriumphMenuManager(this);
        this.teleportManager = new TeleportManager(this);
        this.playerVisibilityManager = new PlayerVisibilityManager(this);
        this.worldProtectionManager = new WorldProtectionManager(this);
        this.joinItemManager = new JoinItemManager(this);
        this.spawnManager = new SpawnManager(this);
        this.rulesCommand = new RulesCommand(this);

        setupCommands();
        registerListeners();

        getLogger().info("MysterriaLobby has been enabled!");
    }

    @Override
    public void onDisable() {
        if (liteCommands != null) {
            liteCommands.unregister();
        }

        if (teleportManager != null) {
            teleportManager.cancelAllTeleports();
        }

        getLogger().info("MysterriaLobby has been disabled!");
    }

    private void setupCommands() {
        this.liteCommands = LiteBukkitFactory.builder("mysterria-lobby", this)
                .commands(
                        new LobbyCommands(this),
                        new TeleportCommand(this),
                        rulesCommand
                )
                .build();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new VoidDamageListener(this), this);
    }

    public void reload() {
        configManager.reload();
        langManager.reload();
        triumphMenuManager.reload();
        teleportManager.reload();
        playerVisibilityManager.reload();
        worldProtectionManager.reload();
        spawnManager.reload();
        rulesCommand.reload();
    }

    public static MysterriaLobby getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public TriumphMenuManager getTriumphMenuManager() {
        return triumphMenuManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public PlayerVisibilityManager getPlayerVisibilityManager() {
        return playerVisibilityManager;
    }

    public WorldProtectionManager getWorldProtectionManager() {
        return worldProtectionManager;
    }

    public JoinItemManager getJoinItemManager() {
        return joinItemManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public RulesCommand getRulesCommand() {
        return rulesCommand;
    }
}