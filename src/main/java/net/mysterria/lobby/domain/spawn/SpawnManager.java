package net.mysterria.lobby.domain.spawn;

import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class SpawnManager {

    private final MysterriaLobby plugin;
    private File spawnFile;
    private FileConfiguration spawnConfig;
    private Location spawnLocation;

    public SpawnManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        createSpawnFile();
        loadSpawnLocation();
    }

    private void createSpawnFile() {
        spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
        if (!spawnFile.exists()) {
            try {
                spawnFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create spawn.yml file: " + e.getMessage());
            }
        }
        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);
    }

    private void loadSpawnLocation() {
        if (spawnConfig.contains("spawn")) {
            String worldName = spawnConfig.getString("spawn.world");
            double x = spawnConfig.getDouble("spawn.x");
            double y = spawnConfig.getDouble("spawn.y");
            double z = spawnConfig.getDouble("spawn.z");
            float yaw = (float) spawnConfig.getDouble("spawn.yaw");
            float pitch = (float) spawnConfig.getDouble("spawn.pitch");

            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                spawnLocation = new Location(world, x, y, z, yaw, pitch);
            } else {
                plugin.getLogger().warning("World '" + worldName + "' not found for spawn location!");
            }
        }
    }

    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
        
        spawnConfig.set("spawn.world", location.getWorld().getName());
        spawnConfig.set("spawn.x", location.getX());
        spawnConfig.set("spawn.y", location.getY());
        spawnConfig.set("spawn.z", location.getZ());
        spawnConfig.set("spawn.yaw", location.getYaw());
        spawnConfig.set("spawn.pitch", location.getPitch());

        try {
            spawnConfig.save(spawnFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save spawn location: " + e.getMessage());
        }
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public boolean hasSpawnLocation() {
        return spawnLocation != null;
    }

    public void teleportToSpawn(Player player) {
        if (hasSpawnLocation()) {
            player.teleport(spawnLocation);
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    public void reload() {
        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);
        loadSpawnLocation();
    }
}