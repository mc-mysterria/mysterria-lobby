# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build          # Build shadowJar (output in build/libs/)
./gradlew clean build    # Clean and rebuild
```

No test suite exists. The `.claude/settings.local.json` pre-allows `./gradlew build` without prompting.

**Stack**: Java 25, Paper API 26.1.2 (Minecraft 1.21), Gradle with Shadow plugin.

**Key dependencies**:
- `dev.triumphteam:triumph-gui` — inventory GUI framework
- `dev.rollczi:litecommands-bukkit` — annotation-based command registration
- Adventure MiniMessage — all user-facing text uses MiniMessage formatting

## Architecture

**Entry point**: `MysterriaLobby.java` — singleton JavaPlugin (`getInstance()`). All managers are instantiated in `onEnable()` and stored as fields. A `/lobby reload` command recreates all managers in-place without restarting the plugin.

**Manager initialization order** (matters for dependencies):
1. `ConfigManager` — wraps config.yml
2. `LangManager` — reads language keys / available language list
3. `GuiManager` — scans `guis/*.yml` files + backward-compat `config.yml#menus`
4. `TeleportManager` — loads `teleport-zones.yml`
5. `PlayerVisibilityManager`, `WorldProtectionManager`, `JoinItemManager`, `SpawnManager`
6. `ActionBarManager`, `AnnouncementManager`, `BossBarManager`, `DoubleJumpManager`

**Package layout**:
- `commands/` — LiteCommand handlers (annotated `@Command`, `@Execute`)
- `config/` — `ConfigManager` (config.yml) and `LangManager` (multi-language)
- `domain/` — core logic split into `actions/`, `items/`, `player/`, `protection/`, `spawn/`, `zones/`
- `feature/` — optional lobby features: `actionbar/`, `announcement/`, `bossbar/`, `doublejump/`
- `gui/` — `GuiManager` (file-per-GUI system)
- `listeners/` — Bukkit event handlers
- `util/` — `ItemBuilder` (fluent ItemStack builder), `SkullUtil` (custom skull textures)

## Key Design Patterns

### Localization
`LangManager` auto-detects player locale (`Player.locale()`) and maps it to a language code (e.g. `uk_UA → "ua"`). Available languages are set via `language.available` list in config.yml (falls back to auto-detection from `join.items` keys for backward compat). Per-player overrides stored in memory.

All text keys have language sub-keys:
```yaml
display_name:
  en: "<gold>Text</gold>"
  ua: "<gold>Текст</gold>"
```

Two overload families exist on `LangManager`:
- `getLocalizedComponent(player, "config.path")` — reads from `plugin.getConfig()` (config.yml)
- `getLocalizedComponent(player, fileConfig, "path")` — reads from a `FileConfiguration` (GUI files)

### File-per-GUI System
`GuiManager` loads one GUI per `.yml` file from `{dataFolder}/guis/`. Default files are copied from `src/main/resources/guis/` on first run. GUI files are hot-reloaded via `/lobby reload` — no server restart needed.

GUI file format:
```yaml
title:
  en: "..."
rows: 3
fill:
  enabled: true
  material: GRAY_STAINED_GLASS_PANE
items:
  my_item:
    slot: 13
    material: DIAMOND
    amount: 1
    display_name:
      en: "..."
    lore:
      en: ["..."]
    actions:
      - "[MENU] other_gui"
    custom_model_data: 0
    texture_url: ""   # for PLAYER_HEAD custom skins
```

`GuiManager` tracks a per-player menu history stack. The `[BACK]` action pops the stack and reopens the previous menu (or closes if empty).

### Item Actions (PDC-based)
Items in hotbar store semicolon-separated action strings in PDC under key `mysterria:actions`. GUI items store actions directly in their YAML. Both use `ActionExecutor.executeActions()`. Supported action prefixes:
- `[COMMAND]`, `[CONSOLE]`, `[MENU]`, `[MESSAGE]`, `[TELEPORT]`, `[TOGGLE_VISIBILITY]`, `[SOUND]`, `[CLOSE]`, `[BACK]`, `[SERVER_SELECTOR]`

When adding new action types, update `ActionExecutor.executeAction()`.

### Feature Managers
All features in `feature/` are independently enabled via config (`bossbar.enabled`, `action_bar.enabled`, etc.) and safe to leave disabled. Each has a `reload()` and `stop()` method.

- **BossBarManager** — per-player `BossBar` instances (localized text per player)
- **ActionBarManager** — rotating action bar messages with `{online}/{max}/{player}` placeholders
- **AnnouncementManager** — rotating chat broadcasts, interval in seconds
- **DoubleJumpManager** + `DoubleJumpListener` — gives `allowFlight(true)` in lobby; `PlayerToggleFlightEvent` triggers the jump

### World Protection
`WorldProtectionManager` listens at `EventPriority.HIGH`. Each protection type is independently toggled in `world-protection.yml`. Bypass via permissions like `mysterria.lobby.bypass.damage`.

### Teleport Zones
`TeleportManager` + `TeleportListener` detect zone entry via `PlayerMoveEvent`. On entry: slow-fall/nausea effects, countdown title, then BungeeCord `Connect` packet via plugin channel. Active teleports tracked in `teleportTasks` (UUID → BukkitRunnable); cancelled on zone exit or quit.

## Configuration Files

Located in the plugin data folder (`plugins/MysterriaLobby/`):
- `config.yml` — join items, language, messages, and all feature settings
- `guis/*.yml` — one file per GUI; auto-created from `src/main/resources/guis/` defaults
- `teleport-zones.yml` — zone bounding boxes, destination servers, delays, permissions
- `player-visibility.yml` — cooldown, messages, item slot
- `world-protection.yml` — per-protection-type toggles and bypass permissions
- `rules.yml` — server rules content

To add a new GUI: create `plugins/MysterriaLobby/guis/my_gui.yml` and open it with `[MENU] my_gui`.

## CI/CD

GitHub Actions (`.github/workflows/deploy.yml`) runs `./gradlew clean build` and deploys via Pelican panel API. Deployment targets are read from an external `minecraft-deployment-config` repo. Discord webhook notifies on success/failure. Commits with `[skip ci]` bypass the pipeline.
