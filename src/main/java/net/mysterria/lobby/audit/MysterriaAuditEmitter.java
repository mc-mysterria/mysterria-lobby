package net.mysterria.lobby.audit;

import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditOutcome;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditPrivacy;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditProducer;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditRisk;
import net.mysterria.lobby.domain.zones.TeleportZone;
import org.bukkit.plugin.java.JavaPlugin;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


/** Best-effort bridge to the optional shared Mysterria audit ledger. */
public final class MysterriaAuditEmitter {
    private static final String NAMESPACE = "mysterria-lobby.";

    private static final int MAX_TEXT = 256;
    private static volatile AuditProducer producer;

    private MysterriaAuditEmitter() {
    }

    public static void initialize(JavaPlugin plugin) {
        producer = AuditProducer.create(plugin.getDataFolder().toPath().toAbsolutePath().getParent()
                        .resolve("mysterria-audit-spool"),
                "mysterria-lobby", plugin.getPluginMeta().getVersion());
    }

    public static void close() {
        AuditProducer current = producer;
        producer = null;
        if (current != null) current.close();
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
            AuditProducer current = producer;
            if (current != null) current.recordFailure();
        }
    }

    public static void emitTransfer(String event, AuditOutcome outcome,
                                    UUID correlationId, String businessId, UUID playerId,
                                    String reason, Map<String, ?> values) {
        emit("transfer." + event, outcome, AuditRisk.NORMAL, correlationId,
                businessId, playerId, playerId, null, reason, AuditPrivacy.STAFF_RESTRICTED, values);
    }

    public static void emitPreferenceChanged(UUID correlationId,
                                             UUID playerId, boolean previous, boolean value) {
        emit("visibility.preference_changed", AuditOutcome.COMMITTED, AuditRisk.LOW,
                correlationId, "visibility:" + playerId, playerId, playerId, null, null,
                AuditPrivacy.STAFF_RESTRICTED,
                Map.of("preference", "players_visible", "previous", previous, "value", value));
    }

    public static void emitZoneAdmin(String event, UUID correlationId,
                                     String zoneId, UUID actorId, Map<String, ?> values) {
        emit("zone." + event, AuditOutcome.COMMITTED, AuditRisk.NORMAL,
                correlationId, "zone:" + safe(zoneId), actorId, null, null, null,
                AuditPrivacy.STAFF_RESTRICTED, values);
    }

    public static void emitBypassToggled(UUID correlationId, UUID actorId, boolean enabled) {
        emit("staff.bypass_toggled", AuditOutcome.COMMITTED, AuditRisk.NORMAL,
                correlationId, "bypass:" + actorId, actorId, actorId, null, null,
                AuditPrivacy.STAFF_RESTRICTED,
                Map.of("bypass", "teleport_zone", "enabled", enabled));
    }

    /** Stable, bounded description of a zone definition used by zone audit rows. */
    public static Map<String, Object> zoneMetadata(TeleportZone zone) {
        if (zone == null) return Map.of();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("zone_id", safe(zone.getId()));
        metadata.put("server", safe(zone.getServerName()));
        metadata.put("world", zone.getWorld() == null ? "unknown" : zone.getWorld().getName());
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

    private static String safe(String value) {
        if (value == null) return "";
        return value.length() <= MAX_TEXT ? value : value.substring(0, MAX_TEXT);
    }
}