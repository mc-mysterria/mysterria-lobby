package net.mysterria.lobby.domain.player;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.audit.MysterriaAuditEmitter;
import net.mysterria.lobby.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerVisibilityManager {
    
    private final MysterriaLobby plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey visibilityKey;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    private File configFile;
    private FileConfiguration config;
    private int cooldownTime;
    private int itemSlot;
    
    public PlayerVisibilityManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.visibilityKey = new NamespacedKey(plugin, "players_visible");
        createConfigFile();
        loadConfig();
    }
    
    private void createConfigFile() {
        configFile = new File(plugin.getDataFolder(), "player-visibility.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);
                defaultConfig.set("cooldown", 3);
                defaultConfig.set("item_slot", 8);
                defaultConfig.set("messages.players_shown.en", "<gradient:#4CAF50:#45a049>👥 Players are now <green><bold>VISIBLE</bold></green></gradient>");
                defaultConfig.set("messages.players_shown.ua", "<gradient:#4CAF50:#45a049>👥 Гравці тепер <green><bold>ВИДИМІ</bold></green></gradient>");
                defaultConfig.set("messages.players_hidden.en", "<gradient:#f44336:#d32f2f>👻 Players are now <red><bold>HIDDEN</bold></red></gradient>");
                defaultConfig.set("messages.players_hidden.ua", "<gradient:#f44336:#d32f2f>👻 Гравці тепер <red><bold>ПРИХОВАНІ</bold></red></gradient>");
                defaultConfig.set("messages.cooldown.en", "<red>⏰ Please wait <yellow>{time}</yellow> seconds before using this again!</red>");
                defaultConfig.set("messages.cooldown.ua", "<red>⏰ Зачекайте <yellow>{time}</yellow> секунд перед повторним використанням!</red>");
                defaultConfig.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create player-visibility.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public void reload() {
        cooldowns.clear();
        config = YamlConfiguration.loadConfiguration(configFile);
        loadConfig();
    }
    
    private void loadConfig() {
        cooldownTime = config.getInt("cooldown", 3);
        itemSlot = config.getInt("item_slot", 8);
    }
    
    public boolean arePlayersVisible(Player player) {
        return player.getPersistentDataContainer().getOrDefault(visibilityKey, PersistentDataType.BOOLEAN, true);
    }
    
    public void setPlayersVisible(Player player, boolean visible) {
        boolean previous = arePlayersVisible(player);
        player.getPersistentDataContainer().set(visibilityKey, PersistentDataType.BOOLEAN, visible);
        if (previous != visible) {
            MysterriaAuditEmitter.emitPreferenceChanged(UUID.randomUUID(), player.getUniqueId(),
                    player.getLocation(), previous, visible);
        }
        updatePlayerVisibility(player);
        updateVisibilityItem(player);
        
        String messageKey = visible ? "messages.players_shown" : "messages.players_hidden";
        String message = getLocalizedMessage(player, messageKey);
        player.sendMessage(miniMessage.deserialize(message));
        
        Sound sound = visible ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.BLOCK_GLASS_BREAK;
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }
    
    public boolean togglePlayerVisibility(Player player) {
        if (isOnCooldown(player)) {
            long remainingTime = getRemainingCooldown(player);
            String message = getLocalizedMessage(player, "messages.cooldown")
                    .replace("{time}", String.valueOf(remainingTime));
            player.sendMessage(miniMessage.deserialize(message));
            return false;
        }
        
        boolean currentState = arePlayersVisible(player);
        setPlayersVisible(player, !currentState);
        setCooldown(player);
        return true;
    }
    
    private void updatePlayerVisibility(Player player) {
        boolean visible = arePlayersVisible(player);
        
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (other.equals(player)) continue;
            
            if (visible) {
                player.showPlayer(plugin, other);
            } else {
                player.hidePlayer(plugin, other);
            }
        }
    }
    
    public void updateVisibilityItem(Player player) {
        boolean visible = arePlayersVisible(player);
        ItemStack item;
        
        if (visible) {
            item = new ItemBuilder(Material.LIME_DYE)
                    .localizedDisplayName(player, "player_hider.toggle_item.shown.name")
                    .localizedLore(player, "player_hider.toggle_item.shown.lore")
                    .build();
        } else {
            item = new ItemBuilder(Material.GRAY_DYE)
                    .localizedDisplayName(player, "player_hider.toggle_item.hidden.name")
                    .localizedLore(player, "player_hider.toggle_item.hidden.lore")
                    .build();
        }
        
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "actions"),
                    PersistentDataType.STRING,
                    "[TOGGLE_VISIBILITY]"
            );
        });
        
        player.getInventory().setItem(itemSlot, item);
    }
    
    public void onPlayerJoin(Player player) {
        updatePlayerVisibility(player);
        updateVisibilityItem(player);
        
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (other.equals(player)) continue;
            
            if (arePlayersVisible(other)) {
                other.showPlayer(plugin, player);
            } else {
                other.hidePlayer(plugin, player);
            }
        }
    }
    
    private boolean isOnCooldown(Player player) {
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null) return false;
        
        return (System.currentTimeMillis() - lastUse) < (cooldownTime * 1000L);
    }
    
    private long getRemainingCooldown(Player player) {
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null) return 0;
        
        long elapsed = System.currentTimeMillis() - lastUse;
        long remaining = (cooldownTime * 1000L) - elapsed;
        return Math.max(0, remaining / 1000);
    }
    
    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    private String getLocalizedMessage(Player player, String path) {
        String lang = plugin.getLangManager().getPlayerLang(player);
        String message = config.getString(path + "." + lang);
        if (message == null) {
            message = config.getString(path + ".en", "Missing message: " + path);
        }
        return message;
    }
    
    public void onPlayerQuit(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
}