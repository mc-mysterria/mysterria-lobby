# MysterriaLobby audit events

Rows are written best-effort by the shaded audit client to `plugins/mysterria-audit-spool` and ingested by the optional per-server audit engine. Event types are prefixed `mysterria-lobby.`; all rows are `STAFF_RESTRICTED`. Actor locations are block `world`/`x`/`y`/`z` when known.

| Event type | Outcome(s) | Key facts |
| --- | --- | --- |
| `transfer.requested` | `ATTEMPTED` | Zone countdown scheduled or manual transfer; `business_id=transfer:<correlation UUID>`, `source` (`zone`/`manual`), `server`, `zone_id`, `delay_seconds` |
| `transfer.dispatched` | `OBSERVED`, `FAILED` | BungeeCord `Connect` sent (arrival not proven); same correlation; `correlation_forwarded`, `forward_routes` (`bungeecord_forward`, `direct_channel`); `FAILED` with `reason=bungeecord_dispatch_failed`, `failure_type` |
| `transfer.cancelled` | `CANCELLED` | Countdown cancelled (reload, quit, cancel, shutdown); `reason=teleport_task_cancelled` or `malformed_zone`; no location when the player is offline |
| `visibility.preference_changed` | `COMMITTED` | `business_id=visibility:<player UUID>`; `preference=players_visible`, `previous`, `value` |
| `zone.created` | `COMMITTED`, `FAILED` | Saved to `teleport-zones.yml`; `business_id=zone:<zone ID>`, zone metadata (`server`, `zone_world`, bounds, `delay_seconds`, `permission`); `FAILED` with `reason=save_failed` or `exception` + `failure_type` |
| `zone.deleted` | `COMMITTED`, `FAILED` | Same as `zone.created` for removal |
| `zone.updated` | `COMMITTED` | `source=reload` (one row per changed zone, shared correlation, `change`=`added`/`removed`/`modified`, `changed_fields`) or `source=togglesea` (`changed_fields=sea_effect`, `sea_effect`) |
| `staff.bypass_toggled` | `COMMITTED` | `business_id=bypass:<player UUID>`; `bypass=teleport_zone`, `enabled` |

## Transfer correlation forwarding

Before `Connect`, the lobby sends the payload `byte version (1)`, `UTF correlation UUID`, `UTF zone ID` (empty for manual) via BungeeCord `Forward` to the destination server under sub-channel `mysterria:transfer` (short length prefix), and directly on `mysterria:transfer` when the connection registered that channel.
