package net.mysterria.lobby.domain.collectibles;

import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionPersistence {
    
    private final MysterriaLobby plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, PlayerCollectionProgress> progressCache;
    
    public CollectionPersistence(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "collectible_progress.yml");
        this.progressCache = new ConcurrentHashMap<>();
        loadData();
    }
    
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create collectible progress file: " + e.getMessage());
                return;
            }
        }
        
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String playerIdStr : playersSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(playerIdStr);
                    ConfigurationSection playerSection = playersSection.getConfigurationSection(playerIdStr);
                    
                    List<String> collectedHeads = playerSection.getStringList("collected_heads");
                    long lastCollectionTime = playerSection.getLong("last_collection_time", 0);
                    
                    PlayerCollectionProgress progress = new PlayerCollectionProgress(
                        playerId, 
                        new HashSet<>(collectedHeads), 
                        lastCollectionTime
                    );
                    
                    progressCache.put(playerId, progress);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in collectible progress file: " + playerIdStr);
                }
            }
        }
        
        plugin.getLogger().info("Loaded collectible progress for " + progressCache.size() + " players");
    }
    
    public void saveData() {
        for (Map.Entry<UUID, PlayerCollectionProgress> entry : progressCache.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerCollectionProgress progress = entry.getValue();
            
            String path = "players." + playerId.toString();
            dataConfig.set(path + ".collected_heads", new ArrayList<>(progress.getCollectedHeads()));
            dataConfig.set(path + ".last_collection_time", progress.getLastCollectionTime());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save collectible progress file: " + e.getMessage());
        }
    }
    
    public PlayerCollectionProgress getProgress(UUID playerId) {
        return progressCache.computeIfAbsent(playerId, PlayerCollectionProgress::new);
    }
    
    public void saveProgress(PlayerCollectionProgress progress) {
        progressCache.put(progress.getPlayerId(), progress);
    }
    
    public Collection<PlayerCollectionProgress> getAllProgress() {
        return new ArrayList<>(progressCache.values());
    }
    
    public void clearProgress(UUID playerId) {
        progressCache.remove(playerId);
        dataConfig.set("players." + playerId.toString(), null);
    }
    
    public int getTotalPlayersWithProgress() {
        return progressCache.size();
    }
}