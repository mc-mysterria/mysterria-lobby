package net.mysterria.lobby.feature.bossbar;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarManager {

    private final MysterriaLobby plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, BossBar> playerBossBars = new ConcurrentHashMap<>();
    private boolean enabled;
    private BossBar.Color color;
    private BossBar.Overlay overlay;
    private float progress;

    public BossBarManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        // Hide from all online players before reconfiguring
        for (Player player : plugin.getServer().getOnlinePlayers()) hide(player);
        playerBossBars.clear();

        enabled = plugin.getConfig().getBoolean("bossbar.enabled", false);
        if (!enabled) return;

        String colorStr = plugin.getConfig().getString("bossbar.color", "BLUE");
        String styleStr = plugin.getConfig().getString("bossbar.style", "PROGRESS");
        progress = (float) plugin.getConfig().getDouble("bossbar.progress", 1.0);

        try { color = BossBar.Color.valueOf(colorStr); } catch (Exception e) { color = BossBar.Color.BLUE; }
        try { overlay = BossBar.Overlay.valueOf(styleStr); } catch (Exception e) { overlay = BossBar.Overlay.PROGRESS; }

        for (Player player : plugin.getServer().getOnlinePlayers()) show(player);
    }

    public void show(Player player) {
        if (!enabled) return;
        BossBar bar = playerBossBars.computeIfAbsent(player.getUniqueId(), k -> BossBar.bossBar(
                mm.deserialize(plugin.getLangManager().getLocalizedString(player, "bossbar.text")),
                progress, color, overlay
        ));
        // Refresh text for player's language
        bar.name(mm.deserialize(plugin.getLangManager().getLocalizedString(player, "bossbar.text")));
        player.showBossBar(bar);
    }

    public void hide(Player player) {
        BossBar bar = playerBossBars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);
    }

    public void stop() {
        for (Player player : plugin.getServer().getOnlinePlayers()) hide(player);
        playerBossBars.clear();
    }
}
