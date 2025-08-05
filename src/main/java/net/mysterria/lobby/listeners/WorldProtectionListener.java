package net.mysterria.lobby.listeners;

import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.domain.protection.WorldProtectionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class WorldProtectionListener implements Listener {

    private final MysterriaLobby plugin;
    private final WorldProtectionManager protection;

    public WorldProtectionListener(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.protection = plugin.getWorldProtectionManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!protection.isPreventDamage()) return;

        if (event.getEntity() instanceof Player player) {
            if (!protection.hasBypassPermission(player, "damage")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!protection.isPreventHunger()) return;

        if (event.getEntity() instanceof Player player) {
            if (!protection.hasBypassPermission(player, "hunger")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        if (event.getItemDrop().getItemStack().hasItemMeta()) {
            if (event.getItemDrop().getItemStack().getItemMeta().getPersistentDataContainer()
                    .has(new org.bukkit.NamespacedKey(plugin, "actions"))) {
                event.setCancelled(true);
                return;
            }
        }
        
        if (!protection.isPreventItemDrop()) return;

        if (!protection.hasBypassPermission(player, "item_drop")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (!protection.isPreventItemPickup()) return;

        Player player = event.getPlayer();
        if (!protection.hasBypassPermission(player, "item_pickup")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!protection.isPreventBlockBreak()) return;

        Player player = event.getPlayer();
        if (!protection.hasBypassPermission(player, "block_break")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!protection.isPreventBlockPlace()) return;

        Player player = event.getPlayer();
        if (!protection.hasBypassPermission(player, "block_place")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!protection.isPreventInteraction()) return;

        Player player = event.getPlayer();
        if (!protection.hasBypassPermission(player, "interaction")) {
            if (event.getItem() != null && event.getItem().hasItemMeta()) {
                if (event.getItem().getItemMeta().getPersistentDataContainer()
                        .has(new org.bukkit.NamespacedKey(plugin, "actions"))) {
                    return;
                }
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!protection.isPreventWeatherChange()) return;

        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!protection.isPreventMobSpawning()) return;

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL ||
            event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!protection.isPreventExplosions()) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!protection.isPreventExplosions()) return;

        if (event.getEntity() instanceof org.bukkit.entity.Enderman) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!protection.isPreventLeavesDecay()) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onStructureGrow(StructureGrowEvent event) {
        if (!protection.isPreventBlockPlace()) return;
        event.setCancelled(true);
    }
}