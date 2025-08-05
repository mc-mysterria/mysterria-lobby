package net.mysterria.lobby.config;

import net.mysterria.lobby.MysterriaLobby;

public class ConfigManager {
    
    private final MysterriaLobby plugin;
    
    public ConfigManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        reload();
    }
    
    public void reload() {
        plugin.reloadConfig();
        plugin.saveDefaultConfig();
    }
    
    public boolean isFireworkOnJoin() {
        return plugin.getConfig().getBoolean("general.firework_on_join", true);
    }
    
    public boolean isHealOnJoin() {
        return plugin.getConfig().getBoolean("general.heal_on_join", true);
    }
    
    public boolean isTeleportToSpawn() {
        return plugin.getConfig().getBoolean("general.teleport_to_spawn", true);
    }
    
    public boolean isManualLanguageSelectionAllowed() {
        return plugin.getConfig().getBoolean("language.allow_manual_selection", true);
    }
}