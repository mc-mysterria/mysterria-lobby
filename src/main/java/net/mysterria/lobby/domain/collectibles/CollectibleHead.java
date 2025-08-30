package net.mysterria.lobby.domain.collectibles;

import org.bukkit.Location;

public class CollectibleHead {
    
    private final String id;
    private final String name;
    private final Location location;
    private final String textureUrl;
    private final String textureValue;
    
    public CollectibleHead(String id, String name, Location location, String textureUrl, String textureValue) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.textureUrl = textureUrl;
        this.textureValue = textureValue;
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