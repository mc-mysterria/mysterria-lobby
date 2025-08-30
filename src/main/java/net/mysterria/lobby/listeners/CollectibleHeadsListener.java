package net.mysterria.lobby.listeners;

import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.domain.collectibles.CollectibleHead;
import net.mysterria.lobby.domain.collectibles.CollectibleHeadsManager;
import net.mysterria.lobby.domain.collectibles.PlayerCollectionProgress;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class CollectibleHeadsListener implements Listener {
    
    private final MysterriaLobby plugin;
    private final CollectibleHeadsManager headsManager;
    
    public CollectibleHeadsListener(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.headsManager = plugin.getCollectibleHeadsManager();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (!headsManager.isEnabled()) {
            return;
        }
        
        if (!(event.getRightClicked() instanceof ArmorStand armorStand)) {
            return;
        }
        
        if (!headsManager.isCollectibleHead(armorStand)) {
            return;
        }
        
        event.setCancelled(true);
        
        String headId = headsManager.getHeadIdFromArmorStand(armorStand);
        if (headId == null) {
            return;
        }
        
        Player player = event.getPlayer();
        handleHeadCollection(player, headId, armorStand.getLocation());
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (!headsManager.isEnabled()) {
            return;
        }
        
        ArmorStand armorStand = event.getRightClicked();
        if (!headsManager.isCollectibleHead(armorStand)) {
            return;
        }
        
        event.setCancelled(true);
        
        String headId = headsManager.getHeadIdFromArmorStand(armorStand);
        if (headId == null) {
            return;
        }
        
        Player player = event.getPlayer();
        handleHeadCollection(player, headId, armorStand.getLocation());
    }
    
    private void handleHeadCollection(Player player, String headId, Location headLocation) {
        boolean wasCollected = headsManager.collectHead(player, headId);
        
        if (!wasCollected) {
            // Already collected
            PlayerCollectionProgress progress = headsManager.getPlayerProgress(player);
            sendAlreadyCollectedMessage(player, headId, progress);
            return;
        }
        
        // Successfully collected
        CollectibleHead head = headsManager.getHeadById(headId);
        PlayerCollectionProgress progress = headsManager.getPlayerProgress(player);
        
        playCollectionEffects(player, headLocation);
        sendCollectionMessage(player, head, progress);
        
        if (progress.hasCompletedCollection(headsManager.getTotalHeads())) {
            sendCompletionMessage(player, progress);
        }
    }
    
    private void playCollectionEffects(Player player, Location location) {
        // Play sound
        String soundName = plugin.getConfig().getString("collectible_heads.effects.collection_sound", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) plugin.getConfig().getDouble("collectible_heads.effects.sound_volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("collectible_heads.effects.sound_pitch", 1.5);
        
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, volume, pitch);
        }
        
        // Spawn particles
        String particleName = plugin.getConfig().getString("collectible_heads.effects.collection_particle", "VILLAGER_HAPPY");
        int particleCount = plugin.getConfig().getInt("collectible_heads.effects.particle_count", 10);
        double particleSpread = plugin.getConfig().getDouble("collectible_heads.effects.particle_spread", 1.0);
        
        try {
            Particle particle = Particle.valueOf(particleName);
            
            // Spawn particles in a circle around the head
            new BukkitRunnable() {
                int ticks = 0;
                
                @Override
                public void run() {
                    if (ticks >= 20) { // Run for 1 second
                        cancel();
                        return;
                    }
                    
                    Location particleLocation = location.clone().add(0, 1, 0);
                    player.getWorld().spawnParticle(particle, particleLocation, 
                        particleCount, particleSpread, particleSpread, particleSpread, 0.1);
                    
                    ticks += 2;
                }
            }.runTaskTimer(plugin, 0L, 2L);
            
        } catch (IllegalArgumentException e) {
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                location.clone().add(0, 1, 0), particleCount, particleSpread, particleSpread, particleSpread, 0.1);
        }
    }
    
    private void sendCollectionMessage(Player player, CollectibleHead head, PlayerCollectionProgress progress) {
        String headName = plugin.getLangManager().getLocalizedString(player, 
            "collectible_heads.head_names." + head.getHeadType());
        
        // Send title/subtitle
        String title = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.collection_title")
            .replace("%head_name%", headName);
        String subtitle = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.collection_subtitle")
            .replace("%collected%", String.valueOf(progress.getCollectedCount()))
            .replace("%total%", String.valueOf(headsManager.getTotalHeads()));
        
        player.sendTitle(title, subtitle, 10, 40, 10);
        
        // Send chat message  
        String chatMessage = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.collection_chat")
            .replace("%head_name%", headName)
            .replace("%collected%", String.valueOf(progress.getCollectedCount()))
            .replace("%total%", String.valueOf(headsManager.getTotalHeads()))
            .replace("%percentage%", String.format("%.1f", progress.getCompletionPercentage(headsManager.getTotalHeads())));
        
        player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(chatMessage));
    }
    
    private void sendAlreadyCollectedMessage(Player player, String headId, PlayerCollectionProgress progress) {
        CollectibleHead head = headsManager.getHeadById(headId);
        String headType = head != null ? head.getHeadType() : headId;
        String headName = plugin.getLangManager().getLocalizedString(player, 
            "collectible_heads.head_names." + headType);
        
        String message = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.already_collected")
            .replace("%head_name%", headName)
            .replace("%collected%", String.valueOf(progress.getCollectedCount()))
            .replace("%total%", String.valueOf(headsManager.getTotalHeads()));
        
        player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(message));
    }
    
    private void sendCompletionMessage(Player player, PlayerCollectionProgress progress) {
        // Play special completion sound
        String completionSoundName = plugin.getConfig().getString("collectible_heads.effects.completion_sound", "UI_TOAST_CHALLENGE_COMPLETE");
        float volume = (float) plugin.getConfig().getDouble("collectible_heads.effects.sound_volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("collectible_heads.effects.completion_sound_pitch", 1.0);
        
        try {
            Sound sound = Sound.valueOf(completionSoundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, volume, pitch);
        }
        
        // Send completion title
        String title = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.completion_title");
        String subtitle = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.completion_subtitle")
            .replace("%total%", String.valueOf(headsManager.getTotalHeads()));
        
        player.sendTitle(title, subtitle, 20, 60, 20);
        
        // Send completion chat message
        String chatMessage = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.completion_chat")
            .replace("%total%", String.valueOf(headsManager.getTotalHeads()));
        
        player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(chatMessage));
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!headsManager.isEnabled()) {
            return;
        }
        
        if (!(event.getEntity() instanceof ArmorStand armorStand)) {
            return;
        }
        
        if (headsManager.isCollectibleHead(armorStand)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!headsManager.isEnabled()) {
            return;
        }
        
        if (!(event.getEntity() instanceof ArmorStand armorStand)) {
            return;
        }
        
        if (headsManager.isCollectibleHead(armorStand)) {
            event.setCancelled(true);
            
            // If it's a player damaging the armor stand, treat it as a collection attempt
            if (event.getDamager() instanceof Player player) {
                String headId = headsManager.getHeadIdFromArmorStand(armorStand);
                if (headId != null) {
                    handleHeadCollection(player, headId, armorStand.getLocation());
                }
            }
        }
    }
}