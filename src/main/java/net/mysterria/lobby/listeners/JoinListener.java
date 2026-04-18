package net.mysterria.lobby.listeners;

import net.kyori.adventure.title.Title;
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

import java.time.Duration;

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

        // Welcome title
        if (plugin.getConfig().getBoolean("join.title.enabled", false)) {
            showWelcomeTitle(player);
        }

        // Welcome message
        if (plugin.getConfig().getBoolean("join.message.enabled", false)) {
            String msg = plugin.getLangManager().getLocalizedString(player, "join.message");
            msg = msg.replace("{player}", player.getName());
            player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(msg));
        }

        plugin.getBossBarManager().show(player);
        plugin.getDoubleJumpManager().onPlayerJoin(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerVisibilityManager().onPlayerQuit(player);
        plugin.getTeleportManager().onPlayerQuit(player);
        plugin.getLangManager().onPlayerQuit(player);
        plugin.getBossBarManager().hide(player);
        plugin.getDoubleJumpManager().onPlayerQuit(player);
        plugin.getGuiManager().clearHistory(player);
    }

    private void showWelcomeTitle(Player player) {
        var lang = plugin.getLangManager();
        var title = lang.getLocalizedComponent(player, "join.title.title");
        var subtitle = lang.getLocalizedComponent(player, "join.title.subtitle");
        int fadeIn = plugin.getConfig().getInt("join.title.fade_in", 10);
        int stay = plugin.getConfig().getInt("join.title.stay", 70);
        int fadeOut = plugin.getConfig().getInt("join.title.fade_out", 20);

        player.showTitle(Title.title(title, subtitle, Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        )));
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
