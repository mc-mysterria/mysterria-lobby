package net.mysterria.lobby.domain.collectibles;

import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.util.SkullUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectibleHeadsManager {
    
    private final MysterriaLobby plugin;
    private final Map<String, CollectibleHead> heads;
    private final Map<Location, String> headLocationMap;
    private final Map<String, ArmorStand> spawnedHeads;
    private final CollectionPersistence persistence;
    private final DiscordWebhookService webhookService;
    private boolean enabled;
    
    public CollectibleHeadsManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.heads = new ConcurrentHashMap<>();
        this.headLocationMap = new ConcurrentHashMap<>();
        this.spawnedHeads = new ConcurrentHashMap<>();
        this.persistence = new CollectionPersistence(plugin);
        this.webhookService = new DiscordWebhookService(plugin);
        reload();
    }
    
    public void reload() {
        clearSpawnedHeads();
        heads.clear();
        headLocationMap.clear();
        
        this.enabled = plugin.getConfig().getBoolean("collectible_heads.enabled", false);
        
        if (!enabled) {
            plugin.getLogger().info("Collectible heads system is disabled");
            return;
        }
        
        loadHeadsFromConfig();
        spawnAllHeads();
        
        plugin.getLogger().info("Loaded " + heads.size() + " collectible heads");
    }
    
    private void loadHeadsFromConfig() {
        ConfigurationSection headsSection = plugin.getConfig().getConfigurationSection("collectible_heads.heads");
        if (headsSection == null) {
            plugin.getLogger().warning("No collectible heads configured");
            return;
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
                
                CollectibleHead head = new CollectibleHead(headId, name, location, textureUrl, textureValue);
                heads.put(headId, head);
                headLocationMap.put(location, headId);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load collectible head '" + headId + "': " + e.getMessage());
            }
        }
    }
    
    private void spawnAllHeads() {
        for (CollectibleHead head : heads.values()) {
            spawnHead(head);
        }
    }
    
    private void spawnHead(CollectibleHead head) {
        Location location = head.getLocation();
        
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setMarker(true);
        armorStand.setSmall(false);
        armorStand.setBasePlate(false);
        armorStand.setArms(false);
        armorStand.setInvulnerable(true);
        armorStand.setPersistent(true);
        
        ItemStack headItem = createHeadItem(head);
        armorStand.getEquipment().setHelmet(headItem);
        
        armorStand.setCustomName("collectible_head:" + head.getId());
        armorStand.setCustomNameVisible(false);
        
        spawnedHeads.put(head.getId(), armorStand);
    }
    
    private ItemStack createHeadItem(CollectibleHead head) {
        ItemStack headItem;
        
        if (!head.getTextureUrl().isEmpty()) {
            headItem = SkullUtil.createCustomHead(head.getTextureUrl());
        } else if (!head.getTextureValue().isEmpty()) {
            headItem = SkullUtil.createCustomHead(head.getTextureValue());
        } else {
            headItem = new ItemStack(Material.PLAYER_HEAD);
        }
        
        return headItem;
    }
    
    public boolean collectHead(Player player, String headId) {
        if (!enabled || !heads.containsKey(headId)) {
            return false;
        }
        
        PlayerCollectionProgress progress = persistence.getProgress(player.getUniqueId());
        
        if (progress.hasCollected(headId)) {
            return false; // Already collected
        }
        
        boolean wasNew = progress.collectHead(headId);
        if (wasNew) {
            persistence.saveProgress(progress);
            
            // Check if collection is complete
            if (progress.hasCompletedCollection(heads.size())) {
                handleCollectionComplete(player, progress);
            }
            
            return true;
        }
        
        return false;
    }
    
    private void handleCollectionComplete(Player player, PlayerCollectionProgress progress) {
        // Send Discord notification
        webhookService.sendCompletionNotification(player, heads.size(), progress.getLastCollectionTime());
        
        plugin.getLogger().info(player.getName() + " has completed the collectible heads collection!");
    }
    
    public CollectibleHead getHeadByLocation(Location location) {
        String headId = headLocationMap.get(location);
        return headId != null ? heads.get(headId) : null;
    }
    
    public CollectibleHead getHeadById(String headId) {
        return heads.get(headId);
    }
    
    public PlayerCollectionProgress getPlayerProgress(Player player) {
        return persistence.getProgress(player.getUniqueId());
    }
    
    public Collection<CollectibleHead> getAllHeads() {
        return new ArrayList<>(heads.values());
    }
    
    public int getTotalHeads() {
        return heads.size();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void saveData() {
        persistence.saveData();
    }
    
    private void clearSpawnedHeads() {
        for (ArmorStand armorStand : spawnedHeads.values()) {
            if (armorStand != null && !armorStand.isDead()) {
                armorStand.remove();
            }
        }
        spawnedHeads.clear();
    }
    
    public void shutdown() {
        saveData();
        clearSpawnedHeads();
    }
    
    public ArmorStand getSpawnedHead(String headId) {
        return spawnedHeads.get(headId);
    }
    
    public boolean isCollectibleHead(ArmorStand armorStand) {
        if (armorStand == null || armorStand.getCustomName() == null) {
            return false;
        }
        return armorStand.getCustomName().startsWith("collectible_head:");
    }
    
    public String getHeadIdFromArmorStand(ArmorStand armorStand) {
        if (!isCollectibleHead(armorStand)) {
            return null;
        }
        return armorStand.getCustomName().substring("collectible_head:".length());
    }
    
    public CollectionPersistence getCollectionPersistence() {
        return persistence;
    }
}