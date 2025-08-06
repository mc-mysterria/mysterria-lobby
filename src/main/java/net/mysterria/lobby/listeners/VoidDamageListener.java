package net.mysterria.lobby.listeners;

import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class VoidDamageListener implements Listener {

    private final MysterriaLobby plugin;

    public VoidDamageListener(MysterriaLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            plugin.getSpawnManager().teleportToSpawn(player);

            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        event.setCancelled(true);
        
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getSpawnManager().teleportToSpawn(player);
            
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.setExhaustion(0.0f);
        }, 1L);
    }
}