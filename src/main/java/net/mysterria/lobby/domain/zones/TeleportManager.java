package net.mysterria.lobby.domain.zones;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private final MysterriaLobby plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, TeleportZone> zones = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> teleportTasks = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerZones = new ConcurrentHashMap<>();

    private File configFile;
    private FileConfiguration config;

    public TeleportManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        createConfigFile();
        loadZones();

        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
    }

    private void createConfigFile() {
        configFile = new File(plugin.getDataFolder(), "teleport-zones.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
                // Create default configuration
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);
                defaultConfig.set("zones.example_portal.server", "survival");
                defaultConfig.set("zones.example_portal.world", "world");
                defaultConfig.set("zones.example_portal.region.min.x", 10.0);
                defaultConfig.set("zones.example_portal.region.min.y", 64.0);
                defaultConfig.set("zones.example_portal.region.min.z", 10.0);
                defaultConfig.set("zones.example_portal.region.max.x", 15.0);
                defaultConfig.set("zones.example_portal.region.max.y", 69.0);
                defaultConfig.set("zones.example_portal.region.max.z", 15.0);
                defaultConfig.set("zones.example_portal.delay", 5);
                defaultConfig.set("zones.example_portal.permission", "");
                defaultConfig.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create teleport-zones.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        zones.clear();
        cancelAllTeleports();
        config = YamlConfiguration.loadConfiguration(configFile);
        loadZones();
    }

    private void loadZones() {
        ConfigurationSection zonesSection = config.getConfigurationSection("zones");
        if (zonesSection == null) return;

        for (String zoneId : zonesSection.getKeys(false)) {
            ConfigurationSection zone = zonesSection.getConfigurationSection(zoneId);
            if (zone == null) continue;

            try {
                String serverName = zone.getString("server");
                String worldName = zone.getString("world");
                World world = Bukkit.getWorld(worldName);

                if (world == null) {
                    plugin.getLogger().warning("World '" + worldName + "' not found for zone '" + zoneId + "'");
                    continue;
                }

                double minX = zone.getDouble("region.min.x");
                double minY = zone.getDouble("region.min.y");
                double minZ = zone.getDouble("region.min.z");
                double maxX = zone.getDouble("region.max.x");
                double maxY = zone.getDouble("region.max.y");
                double maxZ = zone.getDouble("region.max.z");

                int delay = zone.getInt("delay", 5);
                String permission = zone.getString("permission", "");

                TeleportZone teleportZone = new TeleportZone(zoneId, serverName, world,
                        minX, minY, minZ, maxX, maxY, maxZ, delay, permission);

                zones.put(zoneId, teleportZone);

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load teleport zone '" + zoneId + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + zones.size() + " teleport zones");
    }

    public void saveZones() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save teleport zones: " + e.getMessage());
        }
    }

    public void createZone(String id, String serverName, Location pos1, Location pos2, int delay, String permission) {
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            throw new IllegalArgumentException("Both positions must be in the same world");
        }

        TeleportZone zone = new TeleportZone(id, serverName, pos1.getWorld(),
                pos1.getX(), pos1.getY(), pos1.getZ(),
                pos2.getX(), pos2.getY(), pos2.getZ(),
                delay, permission);

        zones.put(id, zone);

        ConfigurationSection zoneSection = config.createSection("zones." + id);
        zoneSection.set("server", serverName);
        zoneSection.set("world", pos1.getWorld().getName());
        zoneSection.set("region.min.x", zone.getMinX());
        zoneSection.set("region.min.y", zone.getMinY());
        zoneSection.set("region.min.z", zone.getMinZ());
        zoneSection.set("region.max.x", zone.getMaxX());
        zoneSection.set("region.max.y", zone.getMaxY());
        zoneSection.set("region.max.z", zone.getMaxZ());
        zoneSection.set("delay", delay);
        if (!permission.isEmpty()) {
            zoneSection.set("permission", permission);
        }

        saveZones();
    }

    public boolean deleteZone(String id) {
        if (zones.remove(id) != null) {
            config.set("zones." + id, null);
            saveZones();
            return true;
        }
        return false;
    }

    public void checkPlayerZone(Player player) {
        String currentZone = playerZones.get(player.getUniqueId());
        TeleportZone newZone = null;

        for (TeleportZone zone : zones.values()) {
            if (zone.contains(player.getLocation())) {
                newZone = zone;
                break;
            }
        }

        if (newZone != null && !newZone.getId().equals(currentZone)) {
            playerZones.put(player.getUniqueId(), newZone.getId());
            startTeleportCountdown(player, newZone);
        } else if (newZone == null && currentZone != null) {
            playerZones.remove(player.getUniqueId());
            cancelTeleport(player);
        }
    }

    private void startTeleportCountdown(Player player, TeleportZone zone) {
        if (!zone.getPermission().isEmpty() && !player.hasPermission(zone.getPermission())) {
            player.sendMessage(miniMessage.deserialize(
                    "<red>⛔ You don't have permission to access this teleporter!"
            ));
            return;
        }

        cancelTeleport(player);

        player.sendMessage(miniMessage.deserialize(
                "<gradient:#00d4ff:#0099cc>🌟 Teleporter detected! Teleporting to <white>" +
                zone.getServerName() + "</white> in <yellow>" + zone.getDelay() + "</yellow> seconds...</gradient>"
        ));

        BukkitTask task = new BukkitRunnable() {
            int countdown = zone.getDelay();

            @Override
            public void run() {
                if (!zone.contains(player.getLocation())) {
                    player.sendMessage(miniMessage.deserialize(
                            "<red>❌ Teleportation cancelled - you left the zone!"
                    ));
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    teleportToServer(player, zone.getServerName());
                    cancel();
                    return;
                }

                // Visual effects
                showCountdownEffects(player, countdown);
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        teleportTasks.put(player.getUniqueId(), task);
    }

    private void showCountdownEffects(Player player, int countdown) {
        // Title
        Title title = Title.title(
                miniMessage.deserialize("<gradient:#ff6b6b:#ee5a52><bold>" + countdown + "</bold></gradient>"),
                miniMessage.deserialize("<gray>Teleporting..."),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
        );
        player.showTitle(title);

        // Sound
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f + (countdown * 0.1f));

        // Particles
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.PORTAL, loc, 10, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.ENCHANT, loc, 5, 0.3, 0.3, 0.3, 0.5);
    }

    public void teleportToServer(Player player, String serverName) {
        // Final effects
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 20, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.END_ROD, loc, 15, 0.5, 1, 0.5, 0.1);
        player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // Title
        Title farewell = Title.title(
                miniMessage.deserialize("<gradient:#a8e6cf:#7fcdcd><bold>✨ Teleporting!</bold></gradient>"),
                miniMessage.deserialize("<gray>Connecting to <yellow>" + serverName + "</yellow>..."),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))
        );
        player.showTitle(farewell);

        // Send to server via BungeeCord
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void cancelTeleport(Player player) {
        BukkitTask task = teleportTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public void cancelAllTeleports() {
        teleportTasks.values().forEach(BukkitTask::cancel);
        teleportTasks.clear();
        playerZones.clear();
    }

    public void onPlayerQuit(Player player) {
        cancelTeleport(player);
        playerZones.remove(player.getUniqueId());
    }

    public Collection<TeleportZone> getZones() {
        return zones.values();
    }

    public TeleportZone getZone(String id) {
        return zones.get(id);
    }

    public boolean hasZone(String id) {
        return zones.containsKey(id);
    }
}