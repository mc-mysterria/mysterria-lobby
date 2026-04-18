package net.mysterria.lobby.listeners;

import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class DoubleJumpListener implements Listener {

    private final MysterriaLobby plugin;

    public DoubleJumpListener(MysterriaLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!event.isFlying()) return;
        Player player = event.getPlayer();
        if (!plugin.getDoubleJumpManager().isEnabled()) return;
        if (plugin.getDoubleJumpManager().isOnCooldown(player)) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        plugin.getDoubleJumpManager().performDoubleJump(player);
    }
}
