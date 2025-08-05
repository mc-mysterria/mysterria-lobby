package net.mysterria.lobby.domain.zones;

import org.bukkit.Location;
import org.bukkit.World;

public class TeleportZone {
    
    private final String id;
    private final String serverName;
    private final World world;
    private final double minX, minY, minZ;
    private final double maxX, maxY, maxZ;
    private final int delay;
    private final String permission;
    
    public TeleportZone(String id, String serverName, World world, 
                       double minX, double minY, double minZ,
                       double maxX, double maxY, double maxZ,
                       int delay, String permission) {
        this.id = id;
        this.serverName = serverName;
        this.world = world;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.delay = delay;
        this.permission = permission;
    }
    
    public boolean contains(Location location) {
        if (!location.getWorld().equals(world)) return false;
        
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
    
    public String getId() {
        return id;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public World getWorld() {
        return world;
    }
    
    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    public double getMaxZ() { return maxZ; }
    
    public int getDelay() {
        return delay;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public Location getCenter() {
        return new Location(world, 
            (minX + maxX) / 2, 
            (minY + maxY) / 2, 
            (minZ + maxZ) / 2);
    }
    
    @Override
    public String toString() {
        return String.format("TeleportZone{id='%s', server='%s', world='%s', " +
                           "min=[%.1f,%.1f,%.1f], max=[%.1f,%.1f,%.1f], delay=%d}",
                           id, serverName, world.getName(), 
                           minX, minY, minZ, maxX, maxY, maxZ, delay);
    }
}