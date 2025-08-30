package net.mysterria.lobby.config;

import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.domain.collectibles.CollectibleHead;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CollectibleHeadsConfig {
    
    private final MysterriaLobby plugin;
    private final File configFile;
    private FileConfiguration config;
    
    public CollectibleHeadsConfig(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "collectible-heads.yml");
        loadConfig();
    }
    
    private void loadConfig() {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("collectible-heads.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public void reload() {
        loadConfig();
    }
    
    public Map<String, CollectibleHead> loadHeads() {
        Map<String, CollectibleHead> heads = new HashMap<>();
        
        ConfigurationSection headsSection = config.getConfigurationSection("heads");
        if (headsSection == null) {
            plugin.getLogger().info("No collectible heads configured in collectible-heads.yml");
            return heads;
        }
        
        for (String headId : headsSection.getKeys(false)) {
            try {
                ConfigurationSection headSection = headsSection.getConfigurationSection(headId);
                
                String worldName = headSection.getString("world");
                double x = headSection.getDouble("x");
                double y = headSection.getDouble("y");
                double z = headSection.getDouble("z");
                float yaw = (float) headSection.getDouble("yaw", 0.0);
                float pitch = (float) headSection.getDouble("pitch", 0.0);
                
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("World '" + worldName + "' not found for collectible head: " + headId);
                    continue;
                }
                
                Location location = new Location(world, x, y, z, yaw, pitch);
                String name = headSection.getString("name", headId);
                String textureUrl = headSection.getString("texture_url", "");
                String textureValue = headSection.getString("texture_value", "");
                String headType = headSection.getString("head_type", "default");
                
                CollectibleHead head = new CollectibleHead(headId, name, location, textureUrl, textureValue);
                heads.put(headId, head);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load collectible head '" + headId + "'", e);
            }
        }
        
        plugin.getLogger().info("Loaded " + heads.size() + " collectible heads from collectible-heads.yml");
        return heads;
    }
    
    public void saveHead(String headId, String name, Location location, String textureUrl, String textureValue, String headType) {
        String path = "heads." + headId;
        config.set(path + ".name", name);
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
        config.set(path + ".texture_url", textureUrl);
        config.set(path + ".texture_value", textureValue);
        config.set(path + ".head_type", headType);
        
        saveConfig();
    }
    
    public void removeHead(String headId) {
        config.set("heads." + headId, null);
        saveConfig();
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save collectible-heads.yml", e);
        }
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public Map<String, String> getHeadTypes() {
        Map<String, String> headTypes = new HashMap<>();
        
        ConfigurationSection typesSection = config.getConfigurationSection("head_types");
        if (typesSection != null) {
            for (String typeId : typesSection.getKeys(false)) {
                ConfigurationSection typeSection = typesSection.getConfigurationSection(typeId);
                String name = typeSection.getString("name", typeId);
                headTypes.put(typeId, name);
            }
        }
        
        return headTypes;
    }
    
    public String getHeadTypeTexture(String headType) {
        return config.getString("head_types." + headType + ".texture_url", "");
    }
    
    public String getHeadTypeName(String headType) {
        return config.getString("head_types." + headType + ".name", headType);
    }
}