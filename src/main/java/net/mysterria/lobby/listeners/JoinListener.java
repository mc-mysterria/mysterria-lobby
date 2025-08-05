package net.mysterria.lobby.listeners;

import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;

public class JoinListener implements Listener {

    private final MysterriaLobby plugin;

    public JoinListener(MysterriaLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfigManager().isHealOnJoin()) {
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }

        if (plugin.getConfigManager().isTeleportToSpawn()) {
            plugin.getSpawnManager().teleportToSpawn(player);
        }

        player.getInventory().clear();
        plugin.getJoinItemManager().giveJoinItems(player);
        plugin.getPlayerVisibilityManager().onPlayerJoin(player);

        if (plugin.getConfigManager().isFireworkOnJoin()) {
            spawnFirework(player.getLocation());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerVisibilityManager().onPlayerQuit(event.getPlayer());
        plugin.getTeleportManager().onPlayerQuit(event.getPlayer());
        plugin.getLangManager().onPlayerQuit(event.getPlayer());
    }

    private void spawnFirework(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL)
                .withColor(Color.BLUE, Color.WHITE)
                .withFade(Color.GRAY)
                .trail(true)
                .build());

        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }
}