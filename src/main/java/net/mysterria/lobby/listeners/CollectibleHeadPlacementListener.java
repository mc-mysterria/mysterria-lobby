package net.mysterria.lobby.listeners;

import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.domain.collectibles.CollectibleHeadsManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class CollectibleHeadPlacementListener implements Listener {
    
    private final CollectibleHeadsManager headsManager;
    
    public CollectibleHeadPlacementListener(MysterriaLobby plugin) {
        this.headsManager = plugin.getCollectibleHeadsManager();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!headsManager.isEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        if (!headsManager.isCollectibleHeadItem(item)) {
            return;
        }
        
        if (event.getBlockPlaced().getType() != Material.PLAYER_HEAD) {
            return;
        }
        
        String headType = headsManager.getHeadTypeFromItem(item);
        if (headType == null) {
            return;
        }
        
        event.setCancelled(true);
        
        headsManager.handleHeadPlacement(player, event.getBlockPlaced().getLocation(), headType);
        
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), null);
        }
    }
}