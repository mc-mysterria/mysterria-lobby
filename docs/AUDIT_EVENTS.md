# MysterriaLobby audit events

MysterriaLobby emits best-effort, non-blocking events through the optional
`MysterriaAudit` Bukkit service. The lobby continues to run normally when the
service is absent or rejects an emission. Events are emitted only after the
authoritative state change is complete (or after the BungeeCord dispatch is
observable).

## Event catalog

| Event type | Outcome | Commit/observation point | Identifiers |
| --- | --- | --- | --- |
| `mysterria-lobby.transfer.requested` | `ATTEMPTED` | A zone countdown is scheduled or an explicit server transfer is dispatched | `business_id=transfer:<correlation UUID>`, actor/subject are the player |
| `mysterria-lobby.transfer.dispatched` | `OBSERVED` | BungeeCord `Connect` plugin message is sent; remote connection completion is not observable here | Same correlation and business ID as the request |
| `mysterria-lobby.transfer.cancelled` | `CANCELLED` | A scheduled countdown task is cancelled (reload, quit, or explicit cancellation) | Same correlation and business ID as the request |
| `mysterria-lobby.visibility.preference_changed` | `COMMITTED` | Player visibility preference is written to the persistent data container | `business_id=visibility:<player UUID>`, actor/subject are the player |
| `mysterria-lobby.zone.created` | `COMMITTED` | Zone is registered and `teleport-zones.yml` is saved | `business_id=zone:<zone ID>`, actor is the staff player |
| `mysterria-lobby.zone.deleted` | `COMMITTED` | Zone is removed and `teleport-zones.yml` is saved | Same stable zone business ID, actor is the staff player |

Transfer events use one operation correlation UUID across the requested,
dispatched, and cancelled lifecycle. Zone and preference commands receive a new
correlation UUID for each committed operation. Metadata keys are snake_case and
bounded before emission; zone metadata includes server, world, bounds, delay,
and permission. No chat text, player names, movement, routine spawn/void
teleports, GUI previews, particles, debug rendering, action bars, or world
protection checks are audited.

The ledger is an optional observer. Its absence or a queue/write failure must
never block or roll back lobby persistence, transfer dispatch, or player
visibility behavior.
