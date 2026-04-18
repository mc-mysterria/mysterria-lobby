package net.mysterria.lobby.feature.announcement;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementManager {

    private final MysterriaLobby plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private BukkitTask task;
    private final List<String> messagePaths = new ArrayList<>();
    private int currentIndex = 0;

    public AnnouncementManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        if (task != null) { task.cancel(); task = null; }
        messagePaths.clear();
        currentIndex = 0;

        if (!plugin.getConfig().getBoolean("announcements.enabled", false)) return;

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("announcements.messages");
        if (sec != null) {
            for (String key : sec.getKeys(false)) messagePaths.add("announcements.messages." + key);
        }
        if (messagePaths.isEmpty()) return;

        // interval is in seconds, convert to ticks
        long intervalTicks = plugin.getConfig().getLong("announcements.interval", 180) * 20L;

        task = new BukkitRunnable() {
            @Override public void run() {
                String path = messagePaths.get(currentIndex % messagePaths.size());
                currentIndex++;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String text = plugin.getLangManager().getLocalizedString(player, path);
                    player.sendMessage(mm.deserialize(text));
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
    }
}
