package net.mysterria.lobby.feature.doublejump;

import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DoubleJumpManager {

    private final MysterriaLobby plugin;
    private final Map<UUID, Long> cooldownEnd = new ConcurrentHashMap<>();

    public DoubleJumpManager(MysterriaLobby plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // Re-apply flight state to all online players after reload
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isEnabled()) enableFlight(player);
            else disableFlight(player);
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("double_jump.enabled", false);
    }

    public void onPlayerJoin(Player player) {
        if (isEnabled()) enableFlight(player);
    }

    public void onPlayerQuit(Player player) {
        cooldownEnd.remove(player.getUniqueId());
    }

    public void enableFlight(Player player) {
        if (isBypassed(player)) return;
        player.setAllowFlight(true);
    }

    private void disableFlight(Player player) {
        if (isBypassed(player)) return;
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    public boolean isOnCooldown(Player player) {
        Long end = cooldownEnd.get(player.getUniqueId());
        return end != null && System.currentTimeMillis() < end;
    }

    public void performDoubleJump(Player player) {
        double strength = plugin.getConfig().getDouble("double_jump.strength", 1.0);
        double cooldownSec = plugin.getConfig().getDouble("double_jump.cooldown", 3.0);

        cooldownEnd.put(player.getUniqueId(), System.currentTimeMillis() + (long) (cooldownSec * 1000));

        player.setFlying(false);
        player.setAllowFlight(false);
        player.setVelocity(player.getVelocity().setY(strength));

        String soundStr = plugin.getConfig().getString("double_jump.sound", "ENTITY_FIREWORK_ROCKET_LAUNCH");
        try { player.playSound(player.getLocation(), Sound.valueOf(soundStr), 1f, 1f); }
        catch (Exception ignored) {}

        long cooldownTicks = (long) (cooldownSec * 20);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && isEnabled()) enableFlight(player);
        }, cooldownTicks);
    }

    private boolean isBypassed(Player player) {
        GameMode gm = player.getGameMode();
        return gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR;
    }
}
