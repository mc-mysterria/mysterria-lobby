package net.mysterria.lobby.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class SkullUtil {
    
    private static final String TEXTURE_URL_PREFIX = "http://textures.minecraft.net/texture/";
    
    /**
     * Creates a player head with a custom texture from a texture URL or ID
     * @param textureUrlOrId Full URL like "http://textures.minecraft.net/texture/28b9f52e..." or just the ID like "28b9f52e..."
     * @return ItemStack with the custom texture
     */
    public static ItemStack createCustomHead(String textureUrlOrId) {
        if (textureUrlOrId == null || textureUrlOrId.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
        
        // If it's just an ID, prepend the full URL
        String textureUrl = textureUrlOrId.startsWith("http") ? textureUrlOrId : TEXTURE_URL_PREFIX + textureUrlOrId;
        
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta == null) {
            return head;
        }
        
        try {
            System.out.println("Creating custom head with URL: " + textureUrl);
            
            // Create the texture value JSON for Minecraft's format
            String textureValue = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
            String encodedTexture = java.util.Base64.getEncoder().encodeToString(textureValue.getBytes());
            
            // Create a PlayerProfile using Paper's API
            PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
            playerProfile.setProperty(new ProfileProperty("textures", encodedTexture));
            meta.setPlayerProfile(playerProfile);
            
            head.setItemMeta(meta);
            System.out.println("Successfully created custom head");
        } catch (Exception e) {
            System.err.println("Failed to create custom head texture: " + e.getMessage());
            e.printStackTrace();
        }
        
        return head;
    }
    
    /**
     * Applies a custom texture to an existing ItemStack if it's a player head
     * @param item The ItemStack to modify
     * @param textureUrlOrId The texture URL or ID
     * @return The modified ItemStack, or the original if it's not a player head
     */
    public static ItemStack applyCustomTexture(ItemStack item, String textureUrlOrId) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return item;
        }
        
        if (textureUrlOrId == null || textureUrlOrId.isEmpty()) {
            return item;
        }
        
        // Create a new head with the texture and copy other properties
        ItemStack customHead = createCustomHead(textureUrlOrId);
        if (item.hasItemMeta() && customHead.hasItemMeta()) {
            SkullMeta originalMeta = (SkullMeta) item.getItemMeta();
            SkullMeta customMeta = (SkullMeta) customHead.getItemMeta();
            
            // Copy display name and lore
            if (originalMeta.hasDisplayName()) {
                customMeta.setDisplayName(originalMeta.getDisplayName());
            }
            if (originalMeta.hasLore()) {
                customMeta.setLore(originalMeta.getLore());
            }
            
            // Copy custom model data
            if (originalMeta.hasCustomModelData()) {
                customMeta.setCustomModelData(originalMeta.getCustomModelData());
            }
            
            customHead.setItemMeta(customMeta);
        }
        
        // Copy amount and other properties
        customHead.setAmount(item.getAmount());
        
        return customHead;
    }
    
    /**
     * Checks if a string looks like a valid texture URL or ID
     * @param textureUrlOrId The string to check
     * @return true if it looks like a valid texture identifier
     */
    public static boolean isValidTextureId(String textureUrlOrId) {
        if (textureUrlOrId == null || textureUrlOrId.isEmpty()) {
            return false;
        }
        
        // Check if it's a full URL
        if (textureUrlOrId.startsWith(TEXTURE_URL_PREFIX)) {
            return textureUrlOrId.length() > TEXTURE_URL_PREFIX.length();
        }
        
        // Check if it's just a texture ID (should be a long hex string)
        return textureUrlOrId.matches("[a-fA-F0-9]{64}") || textureUrlOrId.matches("[a-fA-F0-9]{32}");
    }
}