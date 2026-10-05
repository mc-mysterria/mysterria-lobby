package net.mysterria.lobby.domain.zones;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditOutcome;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.audit.MysterriaAuditEmitter;
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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private final MysterriaLobby plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, TeleportZone> zones = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> teleportTasks = new ConcurrentHashMap<>();
    private final Map<UUID, TransferContext> transferContexts = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerZones = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> seaEffectTasks = new ConcurrentHashMap<>();
    private final Set<String> zonesWithSeaEffect = new HashSet<>();
    private final Set<UUID> bypassPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
        reload(null);
    }

    public void reload(UUID actorId) {
        reload(actorId, null, null);
    }

    public void reload(UUID actorId, String actorName, UUID correlationId) {
        Map<String, Map<String, Object>> previous = snapshotZoneMetadata();
        zones.clear();
        cancelAllTeleports();
        config = YamlConfiguration.loadConfiguration(configFile);
        loadZones();
        emitReloadedZoneChanges(previous, actorId, actorName, correlationId);
    }

    private Map<String, Map<String, Object>> snapshotZoneMetadata() {
        Map<String, Map<String, Object>> snapshot = new HashMap<>();
        zones.forEach((id, zone) -> snapshot.put(id, MysterriaAuditEmitter.zoneMetadata(zone)));
        return snapshot;
    }

    private void emitReloadedZoneChanges(Map<String, Map<String, Object>> previous, UUID actorId,
                                         String actorName, UUID reloadCorrelationId) {
        Map<String, Map<String, Object>> current = snapshotZoneMetadata();
        Set<String> ids = new TreeSet<>(previous.keySet());
        ids.addAll(current.keySet());
        UUID correlationId = reloadCorrelationId == null ? UUID.randomUUID() : reloadCorrelationId;
        for (String id : ids) {
            Map<String, Object> before = previous.get(id);
            Map<String, Object> after = current.get(id);
            if (Objects.equals(before, after)) continue;
            emitZoneUpdated(correlationId, id, actorId, actorName, before, after);
        }
    }

    private void emitZoneUpdated(UUID correlationId, String zoneId, UUID actorId, String actorName,
                                 Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> values = new LinkedHashMap<>(after == null ? before : after);
        values.put("zone_id", zoneId);
        values.put("source", "reload");
        MysterriaAuditEmitter.putActorName(values, actorId, actorName);
        values.put("change", before == null ? "added" : after == null ? "removed" : "modified");
        if (before != null && after != null) {
            List<String> changed = new ArrayList<>();
            after.forEach((key, value) -> {
                if (!Objects.equals(before.get(key), value)) changed.add(key);
            });
            values.put("changed_fields", String.join(",", changed));
        }
        MysterriaAuditEmitter.emitZoneAdmin("updated", correlationId, zoneId, actorId, values);
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

    public boolean saveZones() {
        // Write beside the zones file and move it into place so a failed write never truncates it
        Path target = configFile.toPath();
        Path temporary = target.resolveSibling(configFile.getName() + ".tmp");
        try {
            Files.writeString(temporary, config.saveToString());
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save teleport zones: " + e.getMessage());
            return false;
        }
    }

    public boolean tryCreateZone(String id, String serverName, Location pos1, Location pos2, int delay, String permission) {
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            throw new IllegalArgumentException("Both positions must be in the same world");
        }

        if (zones.containsKey(id)) {
            return false;
        }

        TeleportZone zone = new TeleportZone(id, serverName, pos1.getWorld(),
                pos1.getX(), pos1.getY(), pos1.getZ(),
                pos2.getX(), pos2.getY(), pos2.getZ(),
                delay, permission);

        Object previous = config.get("zones." + id);
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

        if (!saveZones()) {
            config.set("zones." + id, previous);
            return false;
        }

        zones.put(id, zone);
        return true;
    }

    public boolean deleteZone(String id) {
        if (!zones.containsKey(id)) {
            return false;
        }

        Object previous = config.get("zones." + id);
        config.set("zones." + id, null);
        if (!saveZones()) {
            config.set("zones." + id, previous);
            return false;
        }

        zones.remove(id);
        return true;
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
            if (bypassPlayers.contains(player.getUniqueId())) {
                player.sendMessage(miniMessage.deserialize(
                    "<yellow>[Debug] Entered zone <white>" + newZone.getId() + "</white> → <aqua>" +
                    newZone.getServerName() + "</aqua> <gray>(" + newZone.getDelay() + "s delay)</gray>"));
            } else {
                startTeleportCountdown(player, newZone);
            }
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

        TransferContext context = TransferContext.create(zone.getId(), zone.getServerName());
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
                    teleportTasks.remove(player.getUniqueId()); // Clean up task reference
                    transferContexts.remove(player.getUniqueId(), context);
                    cancel();
                    teleportToServer(player, zone.getServerName(), context);
                    return;
                }

                showCountdownEffects(player, countdown, zone.getServerName());
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        teleportTasks.put(player.getUniqueId(), task);
        transferContexts.put(player.getUniqueId(), context);
        Map<String, Object> requested = transferMetadata(player, "zone", zone.getServerName());
        requested.put("zone_id", zone.getId());
        requested.put("delay_seconds", zone.getDelay());
        MysterriaAuditEmitter.emitTransfer("requested", AuditOutcome.ATTEMPTED,
                context.correlationId(), context.businessId(), player.getUniqueId(), null, requested);
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
        TransferContext context = TransferContext.create(null, serverName);
        MysterriaAuditEmitter.emitTransfer("requested", AuditOutcome.ATTEMPTED,
                context.correlationId(), context.businessId(), player.getUniqueId(), null,
                transferMetadata(player, "manual", serverName));
        teleportToServer(player, serverName, context);
    }

    private void teleportToServer(Player player, String serverName, TransferContext context) {
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 20, 1, 1, 1, 0.1);
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

        try {
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            Map<String, Object> metadata = transferMetadata(player, context.source(), serverName);
            metadata.put("observed_via", "bungeecord_connect_dispatch");
            MysterriaAuditEmitter.emitTransfer("dispatched", AuditOutcome.OBSERVED,
                    context.correlationId(), context.businessId(), player.getUniqueId(), null,
                    metadata);
        } catch (RuntimeException failure) {
            Map<String, Object> metadata = transferMetadata(player, context.source(), serverName);
            metadata.put("failure_type", failure.getClass().getSimpleName());
            MysterriaAuditEmitter.emitTransfer("dispatched", AuditOutcome.FAILED,
                    context.correlationId(), context.businessId(), player.getUniqueId(),
                    "bungeecord_dispatch_failed", metadata);
            throw failure;
        }
    }

    private static Map<String, Object> transferMetadata(Player player, String source, String serverName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", source);
        metadata.put("server", serverName);
        MysterriaAuditEmitter.putLocation(metadata, player.getLocation());
        return metadata;
    }

    public void cancelTeleport(Player player) {
        BukkitTask task = teleportTasks.remove(player.getUniqueId());
        TransferContext context = transferContexts.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        if (context != null) {
            emitCancelled(context, player.getUniqueId(), player.getLocation());
        }
    }

    private static void emitCancelled(TransferContext context, UUID playerId, Location location) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", context.source());
        metadata.put("server", context.serverName() == null ? "unknown" : context.serverName());
        metadata.put("zone_id", context.zoneId() == null ? "" : context.zoneId());
        MysterriaAuditEmitter.putLocation(metadata, location);
        String reason = context.serverName() == null ? "malformed_zone" : "teleport_task_cancelled";
        MysterriaAuditEmitter.emitTransfer("cancelled", AuditOutcome.CANCELLED,
                context.correlationId(), context.businessId(), playerId, reason, metadata);
    }

    public void cancelAllTeleports() {
        teleportTasks.values().forEach(BukkitTask::cancel);
        teleportTasks.clear();
        transferContexts.forEach((playerId, context) -> emitCancelled(context, playerId, null));
        transferContexts.clear();
        playerZones.clear();
        stopAllSeaEffects();
    }

    public void onPlayerQuit(Player player) {
        cancelTeleport(player);
        playerZones.remove(player.getUniqueId());
        bypassPlayers.remove(player.getUniqueId());
    }

    public boolean toggleBypass(UUID playerUuid) {
        return toggleBypass(playerUuid, null);
    }

    public boolean toggleBypass(UUID playerUuid, Location actorLocation) {
        boolean enabled = !bypassPlayers.remove(playerUuid);
        if (enabled) {
            bypassPlayers.add(playerUuid);
        }
        MysterriaAuditEmitter.emitBypassToggled(UUID.randomUUID(), playerUuid, actorLocation, enabled);
        return enabled;
    }

    public boolean isBypassing(UUID playerUuid) {
        return bypassPlayers.contains(playerUuid);
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
        return toggleSeaEffect(zoneId, null, null);
    }

    public boolean toggleSeaEffect(String zoneId, UUID actorId, Location actorLocation) {
        if (!zones.containsKey(zoneId)) {
            return false;
        }
        boolean enabled = applySeaEffectToggle(zoneId);
        Map<String, Object> values = new LinkedHashMap<>(MysterriaAuditEmitter.zoneMetadata(zones.get(zoneId)));
        values.put("source", "togglesea");
        values.put("change", "sea_effect_toggled");
        values.put("changed_fields", "sea_effect");
        values.put("sea_effect", enabled);
        MysterriaAuditEmitter.putLocation(values, actorLocation);
        MysterriaAuditEmitter.emitZoneAdmin("updated", UUID.randomUUID(), zoneId, actorId, values);
        return enabled;
    }

    private boolean applySeaEffectToggle(String zoneId) {
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
        int particlesPerTick = Math.min((int) ((maxX - minX) * (maxZ - minZ) / 6), 200);

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

    private record TransferContext(UUID correlationId, String businessId, String zoneId,
                                   String serverName, String source) {
        private static TransferContext create(String zoneId, String serverName) {
            UUID correlationId = UUID.randomUUID();
            return new TransferContext(correlationId, "transfer:" + correlationId,
                    zoneId, serverName, zoneId == null ? "manual" : "zone");
        }
    }
}