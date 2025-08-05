package net.mysterria.lobby.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.description.Description;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.domain.zones.TeleportZone;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Command(name = "teleportzone", aliases = {"tpzone", "zone"})
@Permission("mysterria.lobby.teleport")
public class TeleportCommand {
    
    private final MysterriaLobby plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, Location> firstPositions = new HashMap<>();
    
    public TeleportCommand(MysterriaLobby plugin) {
        this.plugin = plugin;
    }
    
    @Execute(name = "pos1")
    @Description("Set the first position for a teleport zone")
    public void pos1(@Context Player player) {
        Location loc = player.getLocation();
        firstPositions.put(player.getUniqueId(), loc);
        
        player.sendMessage(miniMessage.deserialize(
            "<gradient:#4CAF50:#45a049>✅ First position set: <white>" +
            String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()) +
            "</white> in world <yellow>" + loc.getWorld().getName() + "</yellow></gradient>"
        ));
    }
    
    @Execute(name = "pos2")
    @Description("Set the second position for a teleport zone")
    public void pos2(@Context Player player) {
        Location loc = player.getLocation();
        Location pos1 = firstPositions.get(player.getUniqueId());
        
        if (pos1 == null) {
            player.sendMessage(miniMessage.deserialize(
                "<red>❌ Please set the first position with <yellow>/tpzone pos1</yellow> first!</red>"
            ));
            return;
        }
        
        if (!pos1.getWorld().equals(loc.getWorld())) {
            player.sendMessage(miniMessage.deserialize(
                "<red>❌ Both positions must be in the same world!</red>"
            ));
            return;
        }
        
        player.sendMessage(miniMessage.deserialize(
            "<gradient:#4CAF50:#45a049>✅ Second position set: <white>" +
            String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()) +
            "</white></gradient>"
        ));
        
        double volume = Math.abs((pos1.getX() - loc.getX()) * (pos1.getY() - loc.getY()) * (pos1.getZ() - loc.getZ()));
        player.sendMessage(miniMessage.deserialize(
            "<gray>Zone volume: <yellow>" + String.format("%.1f", volume) + "</yellow> blocks³</gray>"
        ));
    }
    
    @Execute(name = "create")
    @Description("Create a new teleport zone")
    public void create(@Context Player player, @Arg String id, @Arg String serverName, 
                      @Arg(value = "delay") int delay,
                      @Arg(value = "permission") String permission) {
        
        Location pos1 = firstPositions.get(player.getUniqueId());
        if (pos1 == null) {
            player.sendMessage(miniMessage.deserialize(
                "<red>❌ Please set both positions first using <yellow>/tpzone pos1</yellow> and <yellow>/tpzone pos2</yellow>!</red>"
            ));
            return;
        }
        
        Location pos2 = player.getLocation();
        
        if (plugin.getTeleportManager().hasZone(id)) {
            player.sendMessage(miniMessage.deserialize(
                "<red>❌ A teleport zone with ID '<yellow>" + id + "</yellow>' already exists!</red>"
            ));
            return;
        }
        
        try {
            plugin.getTeleportManager().createZone(id, serverName, pos1, pos2, delay, permission);
            firstPositions.remove(player.getUniqueId());
            
            player.sendMessage(miniMessage.deserialize(
                "<gradient:#00d4ff:#0099cc>🎉 Teleport zone '<white>" + id + "</white>' created successfully!</gradient>"
            ));
            player.sendMessage(miniMessage.deserialize(
                "<gray>→ Server: <yellow>" + serverName + "</yellow></gray>"
            ));
            player.sendMessage(miniMessage.deserialize(
                "<gray>→ Delay: <yellow>" + delay + "</yellow> seconds</gray>"
            ));
            if (!permission.isEmpty()) {
                player.sendMessage(miniMessage.deserialize(
                    "<gray>→ Permission: <yellow>" + permission + "</yellow></gray>"
                ));
            }
            
        } catch (Exception e) {
            player.sendMessage(miniMessage.deserialize(
                "<red>❌ Failed to create teleport zone: " + e.getMessage() + "</red>"
            ));
        }
    }
    
    @Execute(name = "delete")
    @Description("Delete a teleport zone")
    public void delete(@Context Player player, @Arg String id) {
        if (!plugin.getTeleportManager().hasZone(id)) {
            player.sendMessage(miniMessage.deserialize(
                "<red>❌ Teleport zone '<yellow>" + id + "</yellow>' not found!</red>"
            ));
            return;
        }
        
        if (plugin.getTeleportManager().deleteZone(id)) {
            player.sendMessage(miniMessage.deserialize(
                "<gradient:#ff6b6b:#ee5a52>🗑️ Teleport zone '<white>" + id + "</white>' deleted successfully!</gradient>"
            ));
        } else {
            player.sendMessage(miniMessage.deserialize(
                "<red>❌ Failed to delete teleport zone '<yellow>" + id + "</yellow>'!</red>"
            ));
        }
    }
    
    @Execute(name = "list")
    @Description("List all teleport zones")
    public void list(@Context Player player) {
        var zones = plugin.getTeleportManager().getZones();
        
        if (zones.isEmpty()) {
            player.sendMessage(miniMessage.deserialize(
                "<yellow>📋 No teleport zones configured.</yellow>"
            ));
            return;
        }
        
        player.sendMessage(miniMessage.deserialize(
            "<gradient:#667eea:#764ba2><bold>📋 Teleport Zones (" + zones.size() + ")</bold></gradient>"
        ));
        
        for (TeleportZone zone : zones) {
            player.sendMessage(miniMessage.deserialize(
                "<gray>• <white>" + zone.getId() + "</white> → <yellow>" + zone.getServerName() + "</yellow> " +
                "<gray>(<yellow>" + zone.getDelay() + "</yellow>s delay)</gray>"
            ));
        }
    }
    
    @Execute(name = "info")
    @Description("Get information about a teleport zone")
    public void info(@Context Player player, @Arg String id) {
        TeleportZone zone = plugin.getTeleportManager().getZone(id);
        if (zone == null) {
            player.sendMessage(miniMessage.deserialize(
                "<red>❌ Teleport zone '<yellow>" + id + "</yellow>' not found!</red>"
            ));
            return;
        }
        
        player.sendMessage(miniMessage.deserialize(
            "<gradient:#667eea:#764ba2><bold>📋 Zone Info: " + zone.getId() + "</bold></gradient>"
        ));
        player.sendMessage(miniMessage.deserialize(
            "<gray>→ Server: <yellow>" + zone.getServerName() + "</yellow></gray>"
        ));
        player.sendMessage(miniMessage.deserialize(
            "<gray>→ World: <yellow>" + zone.getWorld().getName() + "</yellow></gray>"
        ));
        player.sendMessage(miniMessage.deserialize(
            "<gray>→ Region: <yellow>" + 
            String.format("%.1f,%.1f,%.1f", zone.getMinX(), zone.getMinY(), zone.getMinZ()) +
            "</yellow> to <yellow>" +
            String.format("%.1f,%.1f,%.1f", zone.getMaxX(), zone.getMaxY(), zone.getMaxZ()) +
            "</yellow></gray>"
        ));
        player.sendMessage(miniMessage.deserialize(
            "<gray>→ Delay: <yellow>" + zone.getDelay() + "</yellow> seconds</gray>"
        ));
        if (!zone.getPermission().isEmpty()) {
            player.sendMessage(miniMessage.deserialize(
                "<gray>→ Permission: <yellow>" + zone.getPermission() + "</yellow></gray>"
            ));
        }
    }
    
    @Execute(name = "teleport")
    @Description("Manually teleport a player to a server")
    public void teleport(@Context Player player, @Arg String serverName) {
        plugin.getTeleportManager().teleportToServer(player, serverName);
    }
}