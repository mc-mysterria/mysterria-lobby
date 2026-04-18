package net.mysterria.lobby.feature.actionbar;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class ActionBarManager {

    private final MysterriaLobby plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private BukkitTask task;
    private final List<String> messagePaths = new ArrayList<>();
    private int currentIndex = 0;

    public ActionBarManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        if (task != null) { task.cancel(); task = null; }
        messagePaths.clear();
        currentIndex = 0;

        if (!plugin.getConfig().getBoolean("action_bar.enabled", false)) return;

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("action_bar.messages");
        if (sec != null) {
            for (String key : sec.getKeys(false)) messagePaths.add("action_bar.messages." + key);
        }
        if (messagePaths.isEmpty()) return;

        int interval = plugin.getConfig().getInt("action_bar.switch_interval", 100);

        task = new BukkitRunnable() {
            @Override public void run() {
                String path = messagePaths.get(currentIndex % messagePaths.size());
                currentIndex++;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String text = plugin.getLangManager().getLocalizedString(player, path);
                    text = applyPlaceholders(text, player);
                    player.sendActionBar(mm.deserialize(text));
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private String applyPlaceholders(String text, Player player) {
        return text
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("{player}", player.getName());
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
    }
}
