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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
    private final Map<String, BukkitTask> seaEffectTasks = new ConcurrentHashMap<>();
    private final Set<String> zonesWithSeaEffect = new HashSet<>();

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

                zonesWithSeaEffect.add(zoneId);
                startSeaEffect(zoneId);

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

        zonesWithSeaEffect.add(id);
        startSeaEffect(id);

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
        if (teleportTasks.containsKey(player.getUniqueId())) {
            return;
        }

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
        }
    }

    private void startTeleportCountdown(Player player, TeleportZone zone) {
        if (!zone.getPermission().isEmpty() && !player.hasPermission(zone.getPermission())) {
            player.sendMessage(plugin.getLangManager().getLocalizedComponent(player, "teleport.no_permission"));
            return;
        }

        cancelTeleport(player);

        PotionEffect slowFall = new PotionEffect(PotionEffectType.SLOW_FALLING, (zone.getDelay() + 5) * 20, 0, false, false);
        PotionEffect nausea = new PotionEffect(PotionEffectType.NAUSEA, (zone.getDelay() + 5) * 20, 0, false, false);
        player.addPotionEffect(slowFall);
        player.addPotionEffect(nausea);

        player.sendMessage(plugin.getLangManager().getLocalizedComponent(player, "teleport.detected")
                .replaceText(builder -> builder.match("%server%").replacement(zone.getServerName()))
                .replaceText(builder -> builder.match("%delay%").replacement(String.valueOf(zone.getDelay()))));

        BukkitTask task = new BukkitRunnable() {
            int countdown = zone.getDelay();

            @Override
            public void run() {

                if (countdown <= 0) {
                    teleportToServer(player, zone.getServerName());
                    teleportTasks.remove(player.getUniqueId()); // Clean up task reference
                    cancel();
                    return;
                }

                showCountdownEffects(player, countdown, zone.getServerName());
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        teleportTasks.put(player.getUniqueId(), task);
    }

    private void showCountdownEffects(Player player, int countdown, String serverName) {
        Title title = Title.title(
                miniMessage.deserialize("<gradient:#ff6b6b:#ee5a52><bold>" + countdown + "</bold></gradient>"),
                plugin.getLangManager().getLocalizedComponent(player, "teleport.subtitle")
                        .replaceText(builder -> builder.match("%server%").replacement(serverName)),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
        );
        player.showTitle(title);

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f + (countdown * 0.1f));

        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.PORTAL, loc, 10, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.ENCHANT, loc, 5, 0.3, 0.3, 0.3, 0.5);
    }

    public void teleportToServer(Player player, String serverName) {
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 20, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.END_ROD, loc, 15, 0.5, 1, 0.5, 0.1);
        player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        Title farewell = Title.title(
                plugin.getLangManager().getLocalizedComponent(player, "teleport.title"),
                miniMessage.deserialize("<gradient:#ff6b6b:#ee5a52><bold>\uD83D\uDC4B\uD83D\uDC4B\uD83D\uDC4B</bold></gradient>"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))
        );
        player.showTitle(farewell);

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
        stopAllSeaEffects();
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

    public boolean toggleSeaEffect(String zoneId) {
        if (!zones.containsKey(zoneId)) {
            return false;
        }

        if (zonesWithSeaEffect.contains(zoneId)) {
            zonesWithSeaEffect.remove(zoneId);
            BukkitTask task = seaEffectTasks.remove(zoneId);
            if (task != null) {
                task.cancel();
            }
            return false;
        } else {
            // Enable sea effect
            zonesWithSeaEffect.add(zoneId);
            startSeaEffect(zoneId);
            return true;
        }
    }

    private void startSeaEffect(String zoneId) {
        TeleportZone zone = zones.get(zoneId);
        if (zone == null) return;

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!zonesWithSeaEffect.contains(zoneId)) {
                    cancel();
                    return;
                }

                drawSeaBoundary(zone);
            }
        }.runTaskTimer(plugin, 0L, 10L); // Run every 10 ticks (2 times per second)

        seaEffectTasks.put(zoneId, task);
    }

    private void drawSeaBoundary(TeleportZone zone) {
        double minX = zone.getMinX();
        double minY = zone.getMinY();
        double minZ = zone.getMinZ();
        double maxX = zone.getMaxX();
        double maxY = zone.getMaxY();
        double maxZ = zone.getMaxZ();

        long currentTime = System.currentTimeMillis();

        double baseStepSize = 1.2; // Base distance between particle points
        int particlesPerTick = (int) ((maxX - minX) * (maxZ - minZ) / 6); // Adaptive particle count

        double seaSurfaceY = (minY + maxY) / 2.0;

        for (int i = 0; i < particlesPerTick; i++) {
            double x = minX + Math.random() * (maxX - minX);
            double z = minZ + Math.random() * (maxZ - minZ);

            double wave1 = Math.sin((currentTime / 1000.0) + (x + z) * 0.4) * 1.2; // Increased from 0.4 to 1.2
            double wave2 = Math.sin((currentTime / 1500.0) + (x * 0.8 + z * 0.6)) * 0.8; // Increased from 0.2 to 0.8
            double wave3 = Math.sin((currentTime / 800.0) + (x * 0.3 + z * 0.9)) * 0.6; // Increased from 0.15 to 0.6
            double wave4 = Math.sin((currentTime / 2000.0) + (x * 0.5 + z * 0.3)) * 0.4; // Additional wave layer
            double totalWave = wave1 + wave2 + wave3 + wave4;

            double finalY = seaSurfaceY + totalWave;

            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.AQUA, 1.4f);
            double offsetX = x + (Math.random() - 0.5) * 0.3;
            double offsetZ = z + (Math.random() - 0.5) * 0.3;
            zone.getWorld().spawnParticle(Particle.DUST, offsetX, finalY, offsetZ, 1, 0.15, 0.1, 0.15, 0, dustOptions);

            if (Math.random() < 0.15) {
                zone.getWorld().spawnParticle(Particle.BUBBLE_POP, offsetX, finalY - 0.2, offsetZ, 1, 0.2, 0.1, 0.2, 0);
            }

            if (Math.random() < 0.08) {
                zone.getWorld().spawnParticle(Particle.SPLASH, offsetX, finalY + 0.1, offsetZ, 3, 0.3, 0.2, 0.3, 0.2);
            }

            if (Math.random() < 0.02) {
                zone.getWorld().spawnParticle(Particle.DOLPHIN, offsetX, finalY + 0.3, offsetZ, 1, 0.4, 0.3, 0.4, 0);
            }

            if (Math.random() < 0.05 && totalWave > 0.5) {
                zone.getWorld().spawnParticle(Particle.FALLING_WATER, offsetX, finalY + 0.5, offsetZ, 2, 0.2, 0.1, 0.2, 0);
            }
        }
    }


    public void stopAllSeaEffects() {
        zonesWithSeaEffect.clear();
        seaEffectTasks.values().forEach(BukkitTask::cancel);
        seaEffectTasks.clear();
    }

    public boolean hasSeaEffect(String zoneId) {
        return zonesWithSeaEffect.contains(zoneId);
    }
}