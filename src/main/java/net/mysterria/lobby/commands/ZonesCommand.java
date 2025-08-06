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
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Command(name = "teleportzone", aliases = {"tpzone", "zone"})
@Permission("mysterria.lobby.teleport")
public class ZonesCommand {

    private final MysterriaLobby plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, Location> firstPositions = new HashMap<>();

    public ZonesCommand(MysterriaLobby plugin) {
        this.plugin = plugin;
    }

    @Execute(name = "pos1")
    @Description("Set the first position for a teleport zone")
    public void pos1(@Context Player player) {
        Location loc = player.getLocation();
        firstPositions.put(player.getUniqueId(), loc);

        player.sendMessage(miniMessage.deserialize("<gradient:#4CAF50:#45a049>✅ First position set: <white>" + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()) + "</white> in world <yellow>" + loc.getWorld().getName() + "</yellow></gradient>"));
    }

    @Execute(name = "pos2")
    @Description("Set the second position for a teleport zone")
    public void pos2(@Context Player player) {
        Location loc = player.getLocation();
        Location pos1 = firstPositions.get(player.getUniqueId());

        if (pos1 == null) {
            player.sendMessage(miniMessage.deserialize("<red>❌ Please set the first position with <yellow>/tpzone pos1</yellow> first!</red>"));
            return;
        }

        if (!pos1.getWorld().equals(loc.getWorld())) {
            player.sendMessage(miniMessage.deserialize("<red>❌ Both positions must be in the same world!</red>"));
            return;
        }

        player.sendMessage(miniMessage.deserialize("<gradient:#4CAF50:#45a049>✅ Second position set: <white>" + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()) + "</white></gradient>"));

        double volume = Math.abs((pos1.getX() - loc.getX()) * (pos1.getY() - loc.getY()) * (pos1.getZ() - loc.getZ()));
        player.sendMessage(miniMessage.deserialize("<gray>Zone volume: <yellow>" + String.format("%.1f", volume) + "</yellow> blocks³</gray>"));
    }

    @Execute(name = "create")
    @Description("Create a new teleport zone")
    public void create(@Context Player player, @Arg String id, @Arg String serverName, @Arg(value = "delay") int delay, @Arg(value = "permission") String permission) {

        Location pos1 = firstPositions.get(player.getUniqueId());
        if (pos1 == null) {
            player.sendMessage(miniMessage.deserialize("<red>❌ Please set both positions first using <yellow>/tpzone pos1</yellow> and <yellow>/tpzone pos2</yellow>!</red>"));
            return;
        }

        Location pos2 = player.getLocation();

        if (plugin.getTeleportManager().hasZone(id)) {
            player.sendMessage(miniMessage.deserialize("<red>❌ A teleport zone with ID '<yellow>" + id + "</yellow>' already exists!</red>"));
            return;
        }

        try {
            plugin.getTeleportManager().createZone(id, serverName, pos1, pos2, delay, permission);
            firstPositions.remove(player.getUniqueId());

            player.sendMessage(miniMessage.deserialize("<gradient:#00d4ff:#0099cc>🎉 Teleport zone '<white>" + id + "</white>' created successfully!</gradient>"));
            player.sendMessage(miniMessage.deserialize("<gray>→ Server: <yellow>" + serverName + "</yellow></gray>"));
            player.sendMessage(miniMessage.deserialize("<gray>→ Delay: <yellow>" + delay + "</yellow> seconds</gray>"));
            if (!permission.isEmpty()) {
                player.sendMessage(miniMessage.deserialize("<gray>→ Permission: <yellow>" + permission + "</yellow></gray>"));
            }

        } catch (Exception e) {
            player.sendMessage(miniMessage.deserialize("<red>❌ Failed to create teleport zone: " + e.getMessage() + "</red>"));
        }
    }

    @Execute(name = "delete")
    @Description("Delete a teleport zone")
    public void delete(@Context Player player, @Arg String id) {
        if (!plugin.getTeleportManager().hasZone(id)) {
            player.sendMessage(miniMessage.deserialize("<red>❌ Teleport zone '<yellow>" + id + "</yellow>' not found!</red>"));
            return;
        }

        if (plugin.getTeleportManager().deleteZone(id)) {
            player.sendMessage(miniMessage.deserialize("<gradient:#ff6b6b:#ee5a52>🗑️ Teleport zone '<white>" + id + "</white>' deleted successfully!</gradient>"));
        } else {
            player.sendMessage(miniMessage.deserialize("<red>❌ Failed to delete teleport zone '<yellow>" + id + "</yellow>'!</red>"));
        }
    }

    @Execute(name = "list")
    @Description("List all teleport zones")
    public void list(@Context Player player) {
        var zones = plugin.getTeleportManager().getZones();

        if (zones.isEmpty()) {
            player.sendMessage(miniMessage.deserialize("<yellow>📋 No teleport zones configured.</yellow>"));
            return;
        }

        player.sendMessage(miniMessage.deserialize("<gradient:#667eea:#764ba2><bold>📋 Teleport Zones (" + zones.size() + ")</bold></gradient>"));

        for (TeleportZone zone : zones) {
            player.sendMessage(miniMessage.deserialize("<gray>• <white>" + zone.getId() + "</white> → <yellow>" + zone.getServerName() + "</yellow> " + "<gray>(<yellow>" + zone.getDelay() + "</yellow>s delay)</gray>"));
        }
    }

    @Execute(name = "info")
    @Description("Get information about a teleport zone")
    public void info(@Context Player player, @Arg String id) {
        TeleportZone zone = plugin.getTeleportManager().getZone(id);
        if (zone == null) {
            player.sendMessage(miniMessage.deserialize("<red>❌ Teleport zone '<yellow>" + id + "</yellow>' not found!</red>"));
            return;
        }

        player.sendMessage(miniMessage.deserialize("<gradient:#667eea:#764ba2><bold>📋 Zone Info: " + zone.getId() + "</bold></gradient>"));
        player.sendMessage(miniMessage.deserialize("<gray>→ Server: <yellow>" + zone.getServerName() + "</yellow></gray>"));
        player.sendMessage(miniMessage.deserialize("<gray>→ World: <yellow>" + zone.getWorld().getName() + "</yellow></gray>"));
        player.sendMessage(miniMessage.deserialize("<gray>→ Region: <yellow>" + String.format("%.1f,%.1f,%.1f", zone.getMinX(), zone.getMinY(), zone.getMinZ()) + "</yellow> to <yellow>" + String.format("%.1f,%.1f,%.1f", zone.getMaxX(), zone.getMaxY(), zone.getMaxZ()) + "</yellow></gray>"));
        player.sendMessage(miniMessage.deserialize("<gray>→ Delay: <yellow>" + zone.getDelay() + "</yellow> seconds</gray>"));
        if (!zone.getPermission().isEmpty()) {
            player.sendMessage(miniMessage.deserialize("<gray>→ Permission: <yellow>" + zone.getPermission() + "</yellow></gray>"));
        }
    }

    @Execute(name = "teleport")
    @Description("Manually teleport a player to a server")
    public void teleport(@Context Player player, @Arg String serverName) {
        plugin.getTeleportManager().teleportToServer(player, serverName);
    }

    @Execute(name = "showbounds")
    @Description("Show boundaries of a teleport zone")
    public void showBounds(@Context Player player, @Arg String id) {
        TeleportZone zone = plugin.getTeleportManager().getZone(id);
        if (zone == null) {
            player.sendMessage(miniMessage.deserialize("<red>❌ Teleport zone '<yellow>" + id + "</yellow>' not found!</red>"));
            return;
        }

        if (!zone.getWorld().equals(player.getWorld())) {
            player.sendMessage(miniMessage.deserialize("<red>❌ Zone is in world '<yellow>" + zone.getWorld().getName() + "</yellow>' but you are in '<yellow>" + player.getWorld().getName() + "</yellow>'!</red>"));
            return;
        }

        showZoneBoundaries(player, zone, 10);

        player.sendMessage(plugin.getLangManager().getLocalizedComponent(player, "teleport.bounds_shown").replaceText(builder -> builder.match("%zone%").replacement(zone.getId())));
    }

    @Execute(name = "togglesea")
    @Description("Toggle permanent sea-like boundary display for a zone")
    public void toggleSea(@Context Player player, @Arg String id) {
        TeleportZone zone = plugin.getTeleportManager().getZone(id);
        if (zone == null) {
            player.sendMessage(miniMessage.deserialize("<red>❌ Teleport zone '<yellow>" + id + "</yellow>' not found!</red>"));
            return;
        }

        boolean isEnabled = plugin.getTeleportManager().toggleSeaEffect(zone.getId());

        if (isEnabled) {
            player.sendMessage(plugin.getLangManager().getLocalizedComponent(player, "teleport.sea_enabled").replaceText(builder -> builder.match("%zone%").replacement(zone.getId())));
        } else {
            player.sendMessage(plugin.getLangManager().getLocalizedComponent(player, "teleport.sea_disabled").replaceText(builder -> builder.match("%zone%").replacement(zone.getId())));
        }
    }

    private void showZoneBoundaries(Player player, TeleportZone zone, int duration) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = duration * 20;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                drawZoneBoundary(zone, false);
                ticks += 4; // Run every 4 ticks (5 times per second)
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void drawZoneBoundary(TeleportZone zone, boolean waveEffect) {
        double minX = zone.getMinX();
        double minY = zone.getMinY();
        double minZ = zone.getMinZ();
        double maxX = zone.getMaxX();
        double maxY = zone.getMaxY();
        double maxZ = zone.getMaxZ();

        double step = 0.5;
        long currentTime = System.currentTimeMillis();

        drawLine(zone.getWorld(), minX, minY, minZ, maxX, minY, minZ, step, waveEffect, currentTime);
        drawLine(zone.getWorld(), minX, minY, minZ, minX, minY, maxZ, step, waveEffect, currentTime);
        drawLine(zone.getWorld(), maxX, minY, minZ, maxX, minY, maxZ, step, waveEffect, currentTime);
        drawLine(zone.getWorld(), minX, minY, maxZ, maxX, minY, maxZ, step, waveEffect, currentTime);

        drawLine(zone.getWorld(), minX, maxY, minZ, maxX, maxY, minZ, step, waveEffect, currentTime);
        drawLine(zone.getWorld(), minX, maxY, minZ, minX, maxY, maxZ, step, waveEffect, currentTime);
        drawLine(zone.getWorld(), maxX, maxY, minZ, maxX, maxY, maxZ, step, waveEffect, currentTime);
        drawLine(zone.getWorld(), minX, maxY, maxZ, maxX, maxY, maxZ, step, waveEffect, currentTime);

        drawLine(zone.getWorld(), minX, minY, minZ, minX, maxY, minZ, step, waveEffect, currentTime);
        drawLine(zone.getWorld(), maxX, minY, minZ, maxX, maxY, minZ, step, waveEffect, currentTime);
        drawLine(zone.getWorld(), minX, minY, maxZ, minX, maxY, maxZ, step, waveEffect, currentTime);
        drawLine(zone.getWorld(), maxX, minY, maxZ, maxX, maxY, maxZ, step, waveEffect, currentTime);
    }

    private void drawLine(org.bukkit.World world, double x1, double y1, double z1, double x2, double y2, double z2, double step, boolean waveEffect, long currentTime) {
        double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
        int points = (int) (distance / step);

        for (int i = 0; i <= points; i++) {
            double ratio = (double) i / points;
            double x = x1 + (x2 - x1) * ratio;
            double y = y1 + (y2 - y1) * ratio;
            double z = z1 + (z2 - z1) * ratio;

            if (waveEffect) {
                double wave = Math.sin((currentTime / 500.0) + (x + z) * 0.5) * 0.3;
                y += wave;

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.AQUA, 1.2f);
                world.spawnParticle(Particle.DUST, x, y, z, 1, 0.1, 0.1, 0.1, 0, dustOptions);
                world.spawnParticle(Particle.BUBBLE_POP, x, y - 0.2, z, 1, 0.1, 0.1, 0.1, 0);
                if (Math.random() < 0.1) {
                    world.spawnParticle(Particle.DOLPHIN, x, y, z, 1, 0.2, 0.2, 0.2, 0);
                }
            } else {
                world.spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.ELECTRIC_SPARK, x, y, z, 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }
}