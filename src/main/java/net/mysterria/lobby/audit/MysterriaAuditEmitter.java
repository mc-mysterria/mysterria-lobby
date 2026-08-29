package net.mysterria.lobby.audit;

import dev.ua.ikeepcalm.coi.api.audit.AuditEmission;
import dev.ua.ikeepcalm.coi.api.audit.AuditOutcome;
import dev.ua.ikeepcalm.coi.api.audit.AuditPrivacy;
import dev.ua.ikeepcalm.coi.api.audit.AuditRisk;
import dev.ua.ikeepcalm.coi.api.audit.MysterriaAudit;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Best-effort bridge to the optional shared Mysterria audit ledger. */
public final class MysterriaAuditEmitter {
    private static final String NAMESPACE = "mysterria-lobby.";
    private static final int MAX_METADATA_ENTRIES = 32;
    private static final int MAX_TEXT = 256;

    private MysterriaAuditEmitter() {
    }

    /**
     * Emits without waiting for persistence. Missing or failing audit providers
     * never change lobby behavior or gate the authoritative mutation.
     */
    public static void emit(JavaPlugin plugin, String event, AuditOutcome outcome,
                            AuditRisk risk, UUID correlationId, String businessId,
                            UUID actorId, UUID subjectId, UUID targetId, String reason,
                            AuditPrivacy privacy, Map<String, ?> values) {
        if (event == null || event.isBlank() || outcome == null || risk == null
                || correlationId == null || businessId == null || businessId.isBlank()
                || privacy == null) {
            return;
        }

        try {
            RegisteredServiceProvider<MysterriaAudit> registration =
                    Bukkit.getServicesManager().getRegistration(MysterriaAudit.class);
            MysterriaAudit audit = registration == null ? null : registration.getProvider();
            if (audit == null) return;

            Map<String, Object> metadata = boundedMetadata(values);
            audit.emit(new AuditEmission(
                    NAMESPACE + event,
                    outcome,
                    risk,
                    privacy,
                    correlationId,
                    businessId,
                    actorId,
                    subjectId,
                    targetId,
                    reason,
                    metadata));
        } catch (RuntimeException | LinkageError failure) {
            if (plugin != null) {
                plugin.getLogger().log(Level.FINE, "Mysterria audit emission was unavailable", failure);
            }
        }
    }

    public static void emitTransfer(JavaPlugin plugin, String event, AuditOutcome outcome,
                                    UUID correlationId, String businessId, UUID playerId,
                                    String reason, Map<String, ?> values) {
        emit(plugin, "transfer." + event, outcome, AuditRisk.NORMAL, correlationId,
                businessId, playerId, playerId, null, reason, AuditPrivacy.STAFF_RESTRICTED, values);
    }

    public static void emitPreferenceChanged(JavaPlugin plugin, UUID correlationId,
                                             UUID playerId, boolean previous, boolean value) {
        emit(plugin, "visibility.preference_changed", AuditOutcome.COMMITTED, AuditRisk.LOW,
                correlationId, "visibility:" + playerId, playerId, playerId, null, null,
                AuditPrivacy.STAFF_RESTRICTED,
                Map.of("preference", "players_visible", "previous", previous, "value", value));
    }

    public static void emitZoneAdmin(JavaPlugin plugin, String event, UUID correlationId,
                                     String zoneId, UUID actorId, Map<String, ?> values) {
        emit(plugin, "zone." + event, AuditOutcome.COMMITTED, AuditRisk.NORMAL,
                correlationId, "zone:" + safe(zoneId), actorId, null, null, null,
                AuditPrivacy.STAFF_RESTRICTED, values);
    }

    private static Map<String, Object> boundedMetadata(Map<String, ?> values) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (values == null) return metadata;
        values.forEach((key, value) -> {
            if (metadata.size() >= MAX_METADATA_ENTRIES || key == null
                    || !key.matches("[a-z][a-z0-9_]*") || value == null) {
                return;
            }
            metadata.put(key, boundedValue(value));
        });
        return Map.copyOf(metadata);
    }

    private static Object boundedValue(Object value) {
        if (value instanceof String text) return safe(text);
        if (value instanceof Number || value instanceof Boolean) return value;
        return safe(String.valueOf(value));
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.length() <= MAX_TEXT ? value : value.substring(0, MAX_TEXT);
    }
}
