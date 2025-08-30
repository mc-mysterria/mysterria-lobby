package net.mysterria.lobby.domain.collectibles;

import org.bukkit.Location;

public class CollectibleHead {
    
    private final String id;
    private final String name;
    private final Location location;
    private final String textureUrl;
    private final String textureValue;
    private final String headType;
    
    public CollectibleHead(String id, String name, Location location, String textureUrl, String textureValue) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.textureUrl = textureUrl;
        this.textureValue = textureValue;
        this.headType = extractHeadTypeFromId(id);
    }
    
    public CollectibleHead(String id, String name, Location location, String textureUrl, String textureValue, String headType) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.textureUrl = textureUrl;
        this.textureValue = textureValue;
        this.headType = headType != null ? headType : extractHeadTypeFromId(id);
    }
    
    private String extractHeadTypeFromId(String headId) {
        if (headId.contains("_")) {
            String[] parts = headId.split("_");
            if (parts.length >= 2) {
                // Remove the timestamp part and keep the type (e.g., "mysterious_orb_1234" -> "mysterious_orb")
                StringBuilder typeBuilder = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) typeBuilder.append("_");
                    typeBuilder.append(parts[i]);
                    // If the last part looks like a timestamp, stop
                    if (i == parts.length - 2 && parts[parts.length - 1].matches("\\d+")) {
                        break;
                    }
                }
                return typeBuilder.toString();
            }
        }
        return headId;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public String getTextureUrl() {
        return textureUrl;
    }
    
    public String getTextureValue() {
        return textureValue;
    }
    
    public String getHeadType() {
        return headType;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CollectibleHead that = (CollectibleHead) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}