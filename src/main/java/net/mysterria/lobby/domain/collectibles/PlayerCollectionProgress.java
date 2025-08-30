package net.mysterria.lobby.domain.collectibles;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerCollectionProgress {
    
    private final UUID playerId;
    private final Set<String> collectedHeads;
    private long lastCollectionTime;
    
    public PlayerCollectionProgress(UUID playerId) {
        this.playerId = playerId;
        this.collectedHeads = new HashSet<>();
        this.lastCollectionTime = 0;
    }
    
    public PlayerCollectionProgress(UUID playerId, Set<String> collectedHeads, long lastCollectionTime) {
        this.playerId = playerId;
        this.collectedHeads = new HashSet<>(collectedHeads);
        this.lastCollectionTime = lastCollectionTime;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public Set<String> getCollectedHeads() {
        return new HashSet<>(collectedHeads);
    }
    
    public boolean hasCollected(String headId) {
        return collectedHeads.contains(headId);
    }
    
    public boolean collectHead(String headId) {
        boolean wasNew = collectedHeads.add(headId);
        if (wasNew) {
            lastCollectionTime = System.currentTimeMillis();
        }
        return wasNew;
    }
    
    public int getCollectedCount() {
        return collectedHeads.size();
    }
    
    public boolean hasCompletedCollection(int totalHeads) {
        return collectedHeads.size() >= totalHeads;
    }
    
    public long getLastCollectionTime() {
        return lastCollectionTime;
    }
    
    public double getCompletionPercentage(int totalHeads) {
        if (totalHeads == 0) return 100.0;
        return (double) collectedHeads.size() / totalHeads * 100.0;
    }
}