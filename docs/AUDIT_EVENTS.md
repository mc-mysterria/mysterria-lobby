# MysterriaLobby audit events

Rows are written best-effort by the shaded audit client to `plugins/mysterria-audit-spool` and ingested by the optional per-server audit engine. Event types are prefixed `mysterria-lobby.`; all rows are `STAFF_RESTRICTED`. Actor locations are block `world`/`x`/`y`/`z` when known. Staff rows without a player actor (console, RCON) have no actor id and carry `actor_name` (`CONSOLE` or the sender name); only the `/lobby reload` command can run without a player, all other staff commands are player-only.

| Event type | Outcome(s) | Key facts |
| --- | --- | --- |
| `transfer.requested` | `ATTEMPTED` | Zone countdown scheduled or manual transfer; `business_id=transfer:<correlation UUID>`, `source` (`zone`/`manual`), `server`, `zone_id`, `delay_seconds` |
| `transfer.dispatched` | `OBSERVED`, `FAILED` | BungeeCord `Connect` handed to the proxy (arrival not proven), `observed_via=bungeecord_connect_dispatch`; same correlation, with `zone_id` only on requested and cancelled rows; `FAILED` with `reason=bungeecord_dispatch_failed`, `failure_type` |
| `transfer.cancelled` | `CANCELLED` | Countdown cancelled (reload, quit, cancel, shutdown); `reason=teleport_task_cancelled` or `malformed_zone`; player location only on quit and cancel, none on reload or shutdown |
| `visibility.preference_changed` | `COMMITTED` | `business_id=visibility:<player UUID>`; `preference=players_visible`, `value` (written on every toggle) |
| `zone.created` | `COMMITTED`, `FAILED` | Saved to `teleport-zones.yml`; `business_id=zone:<zone ID>`, zone metadata (`server`, `zone_world`, bounds, `delay_seconds`, `permission`); `FAILED` with `reason=save_failed` or `exception` + `failure_type` |
| `zone.deleted` | `COMMITTED`, `FAILED` | Same as `zone.created` for removal |
| `zone.updated` | `COMMITTED` | `source=reload` (one row per changed zone, `change`=`added`/`removed`/`modified`, `changed_fields`, `actor_name`, no actor location; shares its correlation with the `staff.reloaded` row of the same `/lobby reload`, which has it) or `source=togglesea` (`change=sea_effect_toggled`, `changed_fields=sea_effect`, `sea_effect`) |
| `staff.reloaded` | `COMMITTED`, `FAILED` | `/lobby reload`, written even when no zone changed; `business_id=reload:mysterria-lobby`, `actor_name`, `scope` (everything reloaded), `duration_ms`, actor block location for players; `FAILED` with `reason=exception` + `failure_type` (the reload may be partly applied) |
| `staff.spawn_set` | `COMMITTED`, `FAILED` | `/lobby spawn`; `business_id=spawn:lobby`, `actor_name`, `setting=lobby_spawn`, `previous_set`, `previous_*` and `new_*` (`world`, `x`, `y`, `z`, `yaw`, `pitch`, exact values); emitted after the synchronous `spawn.yml` save; `FAILED` with `reason=save_failed` + `failure_type` (the in-memory spawn is already changed, as before) |
| `staff.bypass_toggled` | `COMMITTED` | `business_id=bypass:<player UUID>`; `bypass=teleport_zone`, `enabled` |

## Main thread

Rows are built from values already in hand and emitted directly; the logging adds no lookups on the main thread. Dropped for that reason: `previous` on `visibility.preference_changed` (a second PDC read), the actor location on reload `zone.updated` rows (a player lookup; the `staff.reloaded` row of the same reload has it) and the player location on reload and shutdown `transfer.cancelled` rows (a player lookup per countdown).

## Transfer correlation

The correlation stays on the lobby rows only. The lobby no longer forwards it to the destination server: the `mysterria:transfer` message (a BungeeCord `Forward` plus a direct send) had no listener in any Mysterria plugin, so it was an extra plugin message per transfer for nothing. With it went the `mysterria:transfer` channel registration and the `correlation_forwarded` and `forward_routes` fields on `transfer.dispatched`.
