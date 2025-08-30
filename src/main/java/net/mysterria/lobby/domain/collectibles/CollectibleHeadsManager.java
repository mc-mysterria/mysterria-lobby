package net.mysterria.lobby.domain.collectibles;

import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.config.CollectibleHeadsConfig;
import net.mysterria.lobby.util.SkullUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectibleHeadsManager {
    
    private final MysterriaLobby plugin;
    private final Map<String, CollectibleHead> heads;
    private final Map<Location, String> headLocationMap;
    private final Map<String, ArmorStand> spawnedHeads;
    private final CollectionPersistence persistence;
    private final DiscordWebhookService webhookService;
    private final CollectibleHeadsConfig headsConfig;
    private boolean enabled;
    
    public CollectibleHeadsManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.heads = new ConcurrentHashMap<>();
        this.headLocationMap = new ConcurrentHashMap<>();
        this.spawnedHeads = new ConcurrentHashMap<>();
        this.persistence = new CollectionPersistence(plugin);
        this.webhookService = new DiscordWebhookService(plugin);
        this.headsConfig = new CollectibleHeadsConfig(plugin);
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
        
        headsConfig.reload();
        Map<String, CollectibleHead> loadedHeads = headsConfig.loadHeads();
        heads.putAll(loadedHeads);
        
        for (CollectibleHead head : heads.values()) {
            headLocationMap.put(head.getLocation(), head.getId());
        }
        
        spawnAllHeads();
        
        plugin.getLogger().info("Loaded " + heads.size() + " collectible heads");
    }
    
    
    private void spawnAllHeads() {
        for (CollectibleHead head : heads.values()) {
            spawnHead(head);
        }
    }
    
    private void spawnHead(CollectibleHead head) {
        Location location = head.getLocation().clone();
        
        // Place the actual head block instead of using armor stand
        location.getBlock().setType(Material.PLAYER_HEAD);
        
        // Create an invisible marker armor stand slightly above for interaction
        Location markerLocation = location.clone().add(0.5, 1.2, 0.5);
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(markerLocation, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setMarker(true);
        armorStand.setSmall(true);
        armorStand.setBasePlate(false);
        armorStand.setArms(false);
        armorStand.setInvulnerable(true);
        armorStand.setPersistent(true);
        
        armorStand.setCustomName("collectible_head:" + head.getId());
        armorStand.setCustomNameVisible(false);
        
        spawnedHeads.put(head.getId(), armorStand);
        
        // Apply the texture to the placed head block
        if (!head.getTextureUrl().isEmpty() || !head.getTextureValue().isEmpty()) {
            applyTextureToHeadBlock(location, head);
        }
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
    
    public boolean addHead(String headId, String name, Location location, String textureUrl, String textureValue) {
        if (heads.containsKey(headId)) {
            return false; // Head already exists
        }
        
        CollectibleHead head = new CollectibleHead(headId, name, location, textureUrl, textureValue);
        heads.put(headId, head);
        headLocationMap.put(location, headId);
        
        if (enabled) {
            spawnHead(head);
        }
        
        return true;
    }
    
    public boolean removeHead(String headId) {
        CollectibleHead head = heads.get(headId);
        if (head == null) {
            return false;
        }
        
        heads.remove(headId);
        headLocationMap.remove(head.getLocation());
        
        ArmorStand armorStand = spawnedHeads.remove(headId);
        if (armorStand != null && !armorStand.isDead()) {
            armorStand.remove();
        }
        
        return true;
    }
    
    public void saveHeadsToConfig() {
        plugin.getConfig().set("collectible_heads.heads", null);
        
        for (CollectibleHead head : heads.values()) {
            String path = "collectible_heads.heads." + head.getId();
            plugin.getConfig().set(path + ".name", head.getName());
            plugin.getConfig().set(path + ".world", head.getLocation().getWorld().getName());
            plugin.getConfig().set(path + ".x", head.getLocation().getX());
            plugin.getConfig().set(path + ".y", head.getLocation().getY());
            plugin.getConfig().set(path + ".z", head.getLocation().getZ());
            plugin.getConfig().set(path + ".yaw", head.getLocation().getYaw());
            plugin.getConfig().set(path + ".pitch", head.getLocation().getPitch());
            plugin.getConfig().set(path + ".texture_url", head.getTextureUrl());
            plugin.getConfig().set(path + ".texture_value", head.getTextureValue());
        }
        
        plugin.saveConfig();
    }
    
    private void applyTextureToHeadBlock(Location location, CollectibleHead head) {
        Block block = location.getBlock();
        if (!(block.getState() instanceof Skull skull)) {
            return;
        }
        
        try {
            ItemStack headItem = SkullUtil.createCustomHead(head.getTextureUrl());
            SkullMeta meta = (SkullMeta) headItem.getItemMeta();
            if (meta != null && meta.getPlayerProfile() != null) {
                skull.setPlayerProfile(meta.getPlayerProfile());
                skull.update();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply texture to head block: " + e.getMessage());
        }
    }
    
    public ItemStack createCollectibleHeadItem(String headType) {
        Map<String, String> headTypes = headsConfig.getHeadTypes();
        if (!headTypes.containsKey(headType)) {
            plugin.getLogger().warning("Unknown head type: " + headType);
            return null;
        }
        
        String texture = headsConfig.getHeadTypeTexture(headType);
        String typeName = headsConfig.getHeadTypeName(headType);
        
        ItemStack headItem = SkullUtil.createCustomHead(texture);
        ItemMeta meta = headItem.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6§l" + typeName + " Head");
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Place this head to create a");
            lore.add("§7collectible for players to find!");
            lore.add("");
            lore.add("§e§lType: §f" + headType);
            lore.add("§a§lCollectible Head");
            
            meta.setLore(lore);
            
            // Add custom data to identify it as a collectible head item
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "collectible_head_type"), 
                org.bukkit.persistence.PersistentDataType.STRING, 
                headType
            );
            
            headItem.setItemMeta(meta);
        }
        
        return headItem;
    }
    
    public boolean isCollectibleHeadItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(
            new NamespacedKey(plugin, "collectible_head_type"), 
            org.bukkit.persistence.PersistentDataType.STRING
        );
    }
    
    public String getHeadTypeFromItem(ItemStack item) {
        if (!isCollectibleHeadItem(item)) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(
            new NamespacedKey(plugin, "collectible_head_type"), 
            org.bukkit.persistence.PersistentDataType.STRING
        );
    }
    
    public boolean handleHeadPlacement(Player player, Location location, String headType) {
        // Generate unique ID for this head
        String headId = headType + "_" + System.currentTimeMillis();
        String name = headsConfig.getHeadTypeName(headType);
        String texture = headsConfig.getHeadTypeTexture(headType);
        
        CollectibleHead head = new CollectibleHead(headId, name, location, texture, "");
        heads.put(headId, head);
        headLocationMap.put(location, headId);
        
        // Save to config
        headsConfig.saveHead(headId, name, location, texture, "", headType);
        
        // Spawn the head (will place the block and create marker)
        spawnHead(head);
        
        // Send confirmation message to player
        String message = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.head_placed")
            .replace("%head_id%", headId)
            .replace("%head_name%", name)
            .replace("%x%", String.format("%.1f", location.getX()))
            .replace("%y%", String.format("%.1f", location.getY()))
            .replace("%z%", String.format("%.1f", location.getZ()));
        
        player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(message));
        
        return true;
    }
    
    public CollectibleHeadsConfig getHeadsConfig() {
        return headsConfig;
    }
}