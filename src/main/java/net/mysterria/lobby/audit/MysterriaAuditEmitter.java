package net.mysterria.lobby.audit;

import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditOutcome;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditPrivacy;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditProducer;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditRisk;
import net.mysterria.lobby.domain.zones.TeleportZone;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


/** Best-effort bridge to the optional shared Mysterria audit ledger. */
public final class MysterriaAuditEmitter {
    private static final String NAMESPACE = "mysterria-lobby.";

    private static final int MAX_TEXT = 256;
    private static volatile AuditProducer producer;
    private static volatile Logger logger;

    private MysterriaAuditEmitter() {
    }

    /** Creates the producer; any failure logs a warning and leaves auditing a no-op. */
    public static void initialize(JavaPlugin plugin) {
        logger = plugin.getLogger();
        try {
            producer = AuditProducer.create(plugin.getDataFolder().toPath().toAbsolutePath().getParent()
                            .resolve("mysterria-audit-spool"),
                    "mysterria-lobby", plugin.getPluginMeta().getVersion());
        } catch (RuntimeException | LinkageError failure) {
            producer = null;
            warn("Mysterria audit producer unavailable; auditing disabled", failure);
        }
    }

    public static void close() {
        AuditProducer current = producer;
        producer = null;
        if (current == null) return;
        try {
            current.close();
        } catch (RuntimeException | LinkageError failure) {
            warn("Failed to close Mysterria audit producer", failure);
        }
    }

    private static void warn(String message, Throwable failure) {
        Logger current = logger;
        if (current != null) current.log(Level.WARNING, message, failure);
    }

    /**
     * Emits without waiting for persistence. Missing or failing audit infrastructure
     * never change lobby behavior or gate the authoritative mutation.
     */
    private static void emit(String event, AuditOutcome outcome,
                             AuditRisk risk, UUID correlationId, String businessId,
                             UUID actorId, UUID subjectId, UUID targetId, String reason,
                             AuditPrivacy privacy, Map<String, ?> values) {
        if (event == null || event.isBlank() || outcome == null || risk == null
                || correlationId == null || businessId == null || businessId.isBlank()
                || privacy == null) {
            return;
        }

        try {
            AuditProducer current = producer;
            if (current == null) return;
            current.emit(NAMESPACE + event, outcome, risk, privacy, correlationId, businessId,
                    actorId, subjectId, targetId, reason, values);
        } catch (RuntimeException | LinkageError failure) {
            warn("Failed to emit Mysterria audit event " + event, failure);
            recordFailure();
        }
    }

    private static void recordFailure() {
        try {
            AuditProducer current = producer;
            if (current != null) current.recordFailure();
        } catch (RuntimeException | LinkageError failure) {
            warn("Failed to record Mysterria audit failure", failure);
        }
    }

    public static void emitTransfer(String event, AuditOutcome outcome,
                                    UUID correlationId, String businessId, UUID playerId,
                                    String reason, Map<String, ?> values) {
        emit("transfer." + event, outcome, AuditRisk.NORMAL, correlationId,
                businessId, playerId, playerId, null, reason, AuditPrivacy.STAFF_RESTRICTED, values);
    }

    public static void emitPreferenceChanged(UUID correlationId, UUID playerId,
                                             Location location, boolean previous, boolean value) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("preference", "players_visible");
        values.put("previous", previous);
        values.put("value", value);
        putLocation(values, location);
        emit("visibility.preference_changed", AuditOutcome.COMMITTED, AuditRisk.LOW,
                correlationId, "visibility:" + playerId, playerId, playerId, null, null,
                AuditPrivacy.STAFF_RESTRICTED, values);
    }

    public static void emitZoneAdmin(String event, UUID correlationId,
                                     String zoneId, UUID actorId, Map<String, ?> values) {
        emitZoneAdmin(event, AuditOutcome.COMMITTED, correlationId, zoneId, actorId, null, values);
    }

    public static void emitZoneAdmin(String event, AuditOutcome outcome, UUID correlationId,
                                     String zoneId, UUID actorId, String reason, Map<String, ?> values) {
        emit("zone." + event, outcome, AuditRisk.NORMAL,
                correlationId, "zone:" + safe(zoneId), actorId, null, null, reason,
                AuditPrivacy.STAFF_RESTRICTED, values);
    }

    public static void emitBypassToggled(UUID correlationId, UUID actorId, Location location, boolean enabled) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("bypass", "teleport_zone");
        values.put("enabled", enabled);
        putLocation(values, location);
        emit("staff.bypass_toggled", AuditOutcome.COMMITTED, AuditRisk.NORMAL,
                correlationId, "bypass:" + actorId, actorId, actorId, null, null,
                AuditPrivacy.STAFF_RESTRICTED, values);
    }

    /** Stable, bounded description of a zone definition used by zone audit rows. */
    public static Map<String, Object> zoneMetadata(TeleportZone zone) {
        if (zone == null) return Map.of();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("zone_id", safe(zone.getId()));
        metadata.put("server", safe(zone.getServerName()));
        metadata.put("zone_world", zone.getWorld() == null ? "unknown" : safe(zone.getWorld().getName()));
        metadata.put("min_x", zone.getMinX());
        metadata.put("min_y", zone.getMinY());
        metadata.put("min_z", zone.getMinZ());
        metadata.put("max_x", zone.getMaxX());
        metadata.put("max_y", zone.getMaxY());
        metadata.put("max_z", zone.getMaxZ());
        metadata.put("delay_seconds", zone.getDelay());
        metadata.put("permission", safe(zone.getPermission()));
        return metadata;
    }

    /** Adds the shared world/x/y/z location keys (block coordinates) when a location is known. */
    public static void putLocation(Map<String, Object> metadata, Location location) {
        if (metadata == null || location == null) return;
        metadata.put("world", location.getWorld() == null ? "unknown" : safe(location.getWorld().getName()));
        metadata.put("x", location.getBlockX());
        metadata.put("y", location.getBlockY());
        metadata.put("z", location.getBlockZ());
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.length() <= MAX_TEXT ? value : value.substring(0, MAX_TEXT);
    }
}