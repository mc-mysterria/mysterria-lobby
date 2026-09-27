# MysterriaLobby audit events

MysterriaLobby emits best-effort events through its shaded neutral audit client. Events distinguish transfer attempts, local state changes, and observable BungeeCord dispatch; dispatch does not prove arrival on another server.

The optional per-server audit engine owns SQLite and local staff searches. Each producer writes to its own bounded spool directory even when the engine is absent. Existing gameplay dependencies remain separate from audit transport.

## Event catalog

| Event type | Outcome | Commit/observation point | Identifiers |
| --- | --- | --- | --- |
| `mysterria-lobby.transfer.requested` | `ATTEMPTED` | A zone countdown is scheduled or an explicit server transfer is dispatched | `business_id=transfer:<correlation UUID>`, actor/subject are the player |
| `mysterria-lobby.transfer.dispatched` | `OBSERVED` | BungeeCord `Connect` plugin message is sent; remote connection completion is not observable here | Same correlation and business ID as the request; metadata `correlation_forwarded` reports whether the `mysterria:transfer` message was sent |
| `mysterria-lobby.transfer.cancelled` | `CANCELLED` | A scheduled countdown task is cancelled (reload, quit, or explicit cancellation) | Same correlation and business ID as the request |
| `mysterria-lobby.visibility.preference_changed` | `COMMITTED` | Player visibility preference is written to the persistent data container | `business_id=visibility:<player UUID>`, actor/subject are the player |
| `mysterria-lobby.zone.created` | `COMMITTED` | Zone is registered and `teleport-zones.yml` is saved | `business_id=zone:<zone ID>`, actor is the staff player |
| `mysterria-lobby.zone.deleted` | `COMMITTED` | Zone is removed and `teleport-zones.yml` is saved | Same stable zone business ID, actor is the staff player |
| `mysterria-lobby.zone.updated` | `COMMITTED` | `/lobby reload` re-read `teleport-zones.yml` and a zone definition was added, removed, or modified compared to the previous in-memory set (one row per changed zone, none when nothing changed) | Same stable zone business ID; actor is the staff player who ran the reload (absent for console); all rows from one reload share a correlation UUID; metadata adds `source=reload`, `change` (`added`/`removed`/`modified`) and, for `modified`, `changed_fields` |
| `mysterria-lobby.staff.bypass_toggled` | `COMMITTED` | `/tpzone bypass` flipped the in-memory teleport-zone bypass flag (not persisted; cleared on quit without a row) | `business_id=bypass:<player UUID>`, actor/subject are the staff player; metadata `bypass=teleport_zone`, `enabled` (new state) |

Transfer events use one operation correlation UUID across the requested,
dispatched, and cancelled lifecycle. Zone, bypass, and preference commands receive a new
correlation UUID for each committed operation (a reload shares one UUID across
its `zone.updated` rows). Metadata keys are snake_case and
bounded before emission; zone metadata includes server, world, bounds, delay,
and permission. No chat text, player names, movement, routine spawn/void
teleports, GUI previews, particles, debug rendering, action bars, or world
protection checks are audited.

## Transfer correlation forwarding

Immediately before the BungeeCord `Connect` message, the lobby sends one plugin
message on channel `mysterria:transfer` through the same player connection, so
the destination server can link its arrival audit row to the lobby's transfer
lifecycle. Payload (Java `DataOutput` encoding, as written by Guava
`ByteArrayDataOutput`):

| Order | Type | Field | Notes |
| --- | --- | --- | --- |
| 1 | `byte` | payload version | Currently `1`; reject or ignore unknown versions |
| 2 | `UTF` (modified UTF-8, 2-byte length prefix) | correlation UUID | Canonical `UUID.toString()` form; equals the `correlation_id` of the lobby `transfer.*` rows, and `business_id` is `transfer:<correlation UUID>` |
| 3 | `UTF` | zone ID | Source teleport zone ID; empty string for manual transfers (`/tpzone teleport`, `[TELEPORT]` actions) |

Delivery notes for the receiving side: plugin messages on this channel go to the
proxy first. The proxy must forward (or re-emit) the payload to the destination
backend for it to be observed there, and it may arrive before the player
finishes joining the destination, so receivers should buffer it briefly keyed
by player UUID. A missing message means only that correlation is unavailable;
the transfer is unaffected. A forwarding failure is logged at `FINE` and never
blocks the `Connect` dispatch. A receiving server can read it with:

```java
DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
int version = in.readUnsignedByte();          // 1
UUID correlationId = UUID.fromString(in.readUTF());
String zoneId = in.readUTF();                 // "" for manual transfers
```

The ledger is an optional observer. Its absence or a queue/write failure must
never block or roll back lobby persistence, transfer dispatch, or player
visibility behavior.
