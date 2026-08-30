# MysterriaLobby - Usage Guide

## Build

`./gradlew build` resolves the COI API snapshot from the Mysterria Nexus repository.
To compile against an unpublished local API build, pass its absolute path with `coiApiJar`:

```bash
./gradlew build -PcoiApiJar=/absolute/path/to/api-1.4.8-SNAPSHOT.jar
```

The COI API remains a `compileOnly` dependency and is not included in the plugin jar.

## 🎯 **Features Implemented**

### ✅ **Core Features**
- **🔗 Multi-language Localization** - Per-player language preferences stored in PDC
- **🧭 Join Items** - Configurable items with actions, localized names and lore
- **📚 Custom Menus** - Both legacy and modern Triumph GUI menus
- **⚙️ Configuration Management** - Hot-reload support for all configs
- **🎆 Join Actions** - Fireworks, healing, spawn teleportation

### ✅ **Player Management**
- **👻 Player Visibility Toggle** - Hide/show players with cooldown (slot 8)
- **🌍 Language Selection** - Manual language switching with command or menu

### ✅ **Teleportation System**
- **🌐 Teleport Zones** - Cubic regions that teleport players to other servers
- **⏱️ Configurable Delays** - Countdown with visual effects before teleportation
- **🎬 Visual Effects** - Particles, sounds, titles, and screen effects
- **🔒 Permission System** - Optional permissions for teleport zones

### ✅ **World Protection**
- **🛡️ Comprehensive Protection** - Damage, hunger, item drops, block interactions
- **⚙️ Configurable Options** - Enable/disable individual protection features
- **🔑 Bypass Permissions** - Staff can bypass protections with permissions

## 🚀 **Commands**

### **LiteCommands Integration**
All commands use the modern LiteCommands framework with tab completion and validation:

```bash
# Main lobby commands
/lobby reload                    # Reload all configurations
/lobby setlang <language>        # Change language preference
/lobby visibility               # Toggle player visibility
/lobby info                     # Display plugin information

# Teleport zone management
/teleportzone pos1              # Set first position
/teleportzone pos2              # Set second position
/teleportzone create <id> <server> [delay] [permission]
/teleportzone delete <id>       # Delete a teleport zone
/teleportzone list              # List all teleport zones
/teleportzone info <id>         # Get zone information
/teleportzone teleport <server> # Manually teleport to server
```

## 📁 **Configuration Files**

### **Main Config** (`config.yml`)
- Join items with MiniMessage gradients
- Menu configurations with localized content
- General settings (fireworks, healing, spawn teleport)
- Player visibility toggle settings

### **Teleport Zones** (`teleport-zones.yml`)
- Cubic regions defined by min/max coordinates
- Server destinations for each zone
- Configurable delays and permissions
- Example zones for survival, creative, minigames

### **Player Visibility** (`player-visibility.yml`)
- Cooldown settings
- Localized messages for visibility states
- Item slot configuration

### **World Protection** (`world-protection.yml`)
- Individual protection toggles
- Bypass permissions for each protection type
- Comprehensive world safety settings

## 🎨 **Visual Features**

### **MiniMessage Integration**
All text supports rich formatting:
```yaml
display_name:
  en: "<gradient:#00d4ff:#0099cc><bold>📚 Server Info</bold></gradient>"
  ua: "<gradient:#00d4ff:#0099cc><bold>📚 Інформація про сервер</bold></gradient>"
```

### **Triumph GUI Menus**
Modern, interactive menus with:
- Click sound effects
- Decorative glass panes
- Rich gradient text
- Smooth interactions

### **Teleportation Effects**
- Particle effects (portal, enchant, dragon breath)
- Countdown titles with gradients
- Sound progression during countdown
- Final teleportation effects

## 🔧 **Action System**

Supports multiple action types:
```yaml
actions:
  - "[COMMAND] spawn"              # Execute player command
  - "[CONSOLE] give {player} diamond" # Execute console command
  - "[MENU] server_info"           # Open menu
  - "[MESSAGE] messages.welcome"   # Send localized message
  - "[TELEPORT] survival"          # Teleport to server
  - "[TOGGLE_VISIBILITY]"          # Toggle player visibility
  - "[SERVER_SELECTOR]"            # Open enhanced server selector
  - "[CLOSE]"                      # Close current inventory
```

## 🌍 **Multi-Language Support**

### **Language Storage**
- Stored in `PersistentDataContainer` with key `mysterria:lang`
- Automatic fallback to default language
- Hot-swappable during gameplay

### **Supported Languages**
- English (`en`) - Default
- Ukrainian (`ua`) - Full translation
- Easily extensible for more languages

## 🛡️ **Permissions**

```
mysterria.lobby.reload          # Reload configurations
mysterria.lobby.teleport        # Manage teleport zones
mysterria.lobby.bypass.*        # Bypass all protections
mysterria.lobby.bypass.damage   # Bypass damage protection
mysterria.lobby.bypass.hunger   # Bypass hunger protection
mysterria.lobby.bypass.itemdrop # Bypass item drop protection
# ... and more bypass permissions
```

## 📋 **Example Setup**

1. **Install the plugin** in your Paper 1.21.3+ server
2. **Configure Velocity** proxy with server names matching teleport zones
3. **Set up teleport zones**:
   ```bash
   /tpzone pos1
   /tpzone pos2
   /tpzone create survival_portal survival 5
   ```
4. **Customize languages** in config.yml with your server's branding
5. **Adjust protections** in world-protection.yml as needed

## 🎯 **Advanced Features**

### **Server Selector Menu**
Enhanced Triumph GUI menu with:
- Animated hover effects
- Server-specific descriptions
- Beautiful gradient styling
- Sound feedback

### **Zone-Based Teleportation**
- Walk into zones to trigger countdown
- Visual effects during countdown
- Cancellation if player leaves zone
- Permission-based access control

### **Persistent Player Settings**
- Language preferences survive server restarts
- Visibility settings maintained across sessions
- Stored efficiently in Minecraft's PDC system

---

**🎉 Ready to create an amazing lobby experience!** The plugin provides a professional, multilingual lobby system with modern UI components and comprehensive world protection.
