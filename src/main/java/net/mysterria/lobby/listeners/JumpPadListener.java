package net.mysterria.lobby.listeners;

import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JumpPadListener implements Listener {

    private final MysterriaLobby plugin;
    private final Map<Material, Double> jumpPowerMap;
    private final Set<Material> pressurePlates;

    public JumpPadListener(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.jumpPowerMap = new EnumMap<>(Material.class);
        this.pressurePlates = new HashSet<>();
        
        // Configure jump power based on block type
        jumpPowerMap.put(Material.IRON_BLOCK, 1.5);
        jumpPowerMap.put(Material.GOLD_BLOCK, 2.0);
        jumpPowerMap.put(Material.DIAMOND_BLOCK, 2.5);
        jumpPowerMap.put(Material.EMERALD_BLOCK, 3.0);
        jumpPowerMap.put(Material.NETHERITE_BLOCK, 3.5);
        
        pressurePlates.add(Material.STONE_PRESSURE_PLATE);
        pressurePlates.add(Material.OAK_PRESSURE_PLATE);
        pressurePlates.add(Material.SPRUCE_PRESSURE_PLATE);
        pressurePlates.add(Material.BIRCH_PRESSURE_PLATE);
        pressurePlates.add(Material.JUNGLE_PRESSURE_PLATE);
        pressurePlates.add(Material.ACACIA_PRESSURE_PLATE);
        pressurePlates.add(Material.DARK_OAK_PRESSURE_PLATE);
        pressurePlates.add(Material.CRIMSON_PRESSURE_PLATE);
        pressurePlates.add(Material.WARPED_PRESSURE_PLATE);
        pressurePlates.add(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        pressurePlates.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !pressurePlates.contains(clickedBlock.getType())) {
            return;
        }
        
        Block baseBlock = clickedBlock.getRelative(0, -1, 0);
        Material baseBlockType = baseBlock.getType();
        
        if (!jumpPowerMap.containsKey(baseBlockType)) {
            return;
        }

        Player player = event.getPlayer();
        double jumpPower = jumpPowerMap.get(baseBlockType);
        
        Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        direction.normalize();
        
        double forwardMultiplier = jumpPower * 1.5; // Forward velocity is 80% of jump power
        double upwardMultiplier = jumpPower * 0.4;  // Upward velocity is 60% of jump power
        
        Vector newVelocity = direction.multiply(forwardMultiplier);
        newVelocity.setY(upwardMultiplier);
        
        player.setVelocity(newVelocity);
        
        player.playSound(player.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.0f, 1.0f + (float)(jumpPower * 0.2));
        spawnJumpParticles(player.getLocation(), baseBlockType, jumpPower);
    }
    
    private void spawnJumpParticles(Location location, Material blockType, double jumpPower) {
        World world = location.getWorld();
        if (world == null) return;
        
        Particle particleType = getParticleForBlock(blockType);
        int particleCount = (int)(jumpPower * 10);
        
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = location.getX() + Math.cos(angle) * 0.5;
            double z = location.getZ() + Math.sin(angle) * 0.5;
            double y = location.getY() + 0.1;
            
            world.spawnParticle(particleType, x, y, z, 1, 0, 0.5, 0, 0.1);
        }
        
        world.spawnParticle(Particle.CLOUD, location.clone().add(0, 0.5, 0), 5, 0.3, 0.1, 0.3, 0.05);
    }
    
    private Particle getParticleForBlock(Material blockType) {
        return switch (blockType) {
            case IRON_BLOCK -> Particle.CRIT;
            case GOLD_BLOCK -> Particle.FIREFLY;
            case DIAMOND_BLOCK -> Particle.FIREWORK;
            case EMERALD_BLOCK -> Particle.HAPPY_VILLAGER;
            case NETHERITE_BLOCK -> Particle.SOUL_FIRE_FLAME;
            default -> Particle.CLOUD;
        };
    }
}