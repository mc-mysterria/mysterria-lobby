package net.mysterria.lobby.domain.protection;

import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class WorldProtectionManager {

    private final MysterriaLobby plugin;
    private File configFile;
    private FileConfiguration config;

    // Protection settings
    private boolean preventDamage;
    private boolean preventHunger;
    private boolean preventItemDrop;
    private boolean preventItemPickup;
    private boolean preventBlockBreak;
    private boolean preventBlockPlace;
    private boolean preventInteraction;
    private boolean preventWeatherChange;
    private boolean preventMobSpawning;
    private boolean preventExplosions;
    private boolean preventFireSpread;
    private boolean preventLeavesDecay;

    public WorldProtectionManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        createConfigFile();
        loadConfig();
    }

    private void createConfigFile() {
        configFile = new File(plugin.getDataFolder(), "world-protection.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);

                // Default protection settings
                defaultConfig.set("protection.prevent_damage", true);
                defaultConfig.set("protection.prevent_hunger", true);
                defaultConfig.set("protection.prevent_item_drop", true);
                defaultConfig.set("protection.prevent_item_pickup", true);
                defaultConfig.set("protection.prevent_block_break", true);
                defaultConfig.set("protection.prevent_block_place", true);
                defaultConfig.set("protection.prevent_interaction", true);
                defaultConfig.set("protection.prevent_weather_change", true);
                defaultConfig.set("protection.prevent_mob_spawning", true);
                defaultConfig.set("protection.prevent_explosions", true);
                defaultConfig.set("protection.prevent_fire_spread", true);
                defaultConfig.set("protection.prevent_leaves_decay", true);

                // Bypass permissions
                defaultConfig.set("permissions.bypass.damage", "mysterria.lobby.bypass.damage");
                defaultConfig.set("permissions.bypass.hunger", "mysterria.lobby.bypass.hunger");
                defaultConfig.set("permissions.bypass.item_drop", "mysterria.lobby.bypass.itemdrop");
                defaultConfig.set("permissions.bypass.item_pickup", "mysterria.lobby.bypass.itempickup");
                defaultConfig.set("permissions.bypass.block_break", "mysterria.lobby.bypass.blockbreak");
                defaultConfig.set("permissions.bypass.block_place", "mysterria.lobby.bypass.blockplace");
                defaultConfig.set("permissions.bypass.interaction", "mysterria.lobby.bypass.interaction");
                defaultConfig.set("permissions.bypass.all", "mysterria.lobby.bypass.*");

                defaultConfig.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create world-protection.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        loadConfig();
    }

    private void loadConfig() {
        preventDamage = config.getBoolean("protection.prevent_damage", true);
        preventHunger = config.getBoolean("protection.prevent_hunger", true);
        preventItemDrop = config.getBoolean("protection.prevent_item_drop", true);
        preventItemPickup = config.getBoolean("protection.prevent_item_pickup", true);
        preventBlockBreak = config.getBoolean("protection.prevent_block_break", true);
        preventBlockPlace = config.getBoolean("protection.prevent_block_place", true);
        preventInteraction = config.getBoolean("protection.prevent_interaction", true);
        preventWeatherChange = config.getBoolean("protection.prevent_weather_change", true);
        preventMobSpawning = config.getBoolean("protection.prevent_mob_spawning", true);
        preventExplosions = config.getBoolean("protection.prevent_explosions", true);
        preventFireSpread = config.getBoolean("protection.prevent_fire_spread", true);
        preventLeavesDecay = config.getBoolean("protection.prevent_leaves_decay", true);
    }

    // Getters with permission checks
    public boolean isPreventDamage() { return preventDamage; }
    public boolean isPreventHunger() { return preventHunger; }
    public boolean isPreventItemDrop() { return preventItemDrop; }
    public boolean isPreventItemPickup() { return preventItemPickup; }
    public boolean isPreventBlockBreak() { return preventBlockBreak; }
    public boolean isPreventBlockPlace() { return preventBlockPlace; }
    public boolean isPreventInteraction() { return preventInteraction; }
    public boolean isPreventWeatherChange() { return preventWeatherChange; }
    public boolean isPreventMobSpawning() { return preventMobSpawning; }
    public boolean isPreventExplosions() { return preventExplosions; }
    public boolean isPreventFireSpread() { return preventFireSpread; }
    public boolean isPreventLeavesDecay() { return preventLeavesDecay; }

    // Permission checking methods
    public boolean hasBypassPermission(org.bukkit.entity.Player player, String type) {
        if (player.hasPermission("mysterria.lobby.bypass.*")) return true;
        return player.hasPermission(config.getString("permissions.bypass." + type, ""));
    }

    public String getBypassPermission(String type) {
        return config.getString("permissions.bypass." + type, "mysterria.lobby.bypass." + type);
    }
}