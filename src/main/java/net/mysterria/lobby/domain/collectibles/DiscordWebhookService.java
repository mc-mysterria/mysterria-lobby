package net.mysterria.lobby.domain.collectibles;

import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.entity.Player;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhookService {
    
    private final MysterriaLobby plugin;
    private final HttpClient httpClient;
    
    public DiscordWebhookService(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    public void sendCompletionNotification(Player player, int totalHeads, long completionTime) {
        String webhookUrl = plugin.getConfig().getString("collectible_heads.discord.webhook_url");
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            plugin.getLogger().info("Discord webhook not configured for collectibles");
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject embed = createCompletionEmbed(player, totalHeads, completionTime);
                JsonObject payload = new JsonObject();
                payload.add("embeds", new com.google.gson.JsonArray());
                payload.getAsJsonArray("embeds").add(embed);
                
                String webhookUsername = plugin.getConfig().getString("collectible_heads.discord.username", "Mysterria Lobby");
                String webhookAvatarUrl = plugin.getConfig().getString("collectible_heads.discord.avatar_url", "");
                
                payload.addProperty("username", webhookUsername);
                if (!webhookAvatarUrl.isEmpty()) {
                    payload.addProperty("avatar_url", webhookAvatarUrl);
                }
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    plugin.getLogger().info("Successfully sent Discord notification for " + player.getName() + "'s collection completion");
                } else {
                    plugin.getLogger().warning("Discord webhook returned status " + response.statusCode() + ": " + response.body());
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord notification: " + e.getMessage());
            }
        });
    }
    
    private JsonObject createCompletionEmbed(Player player, int totalHeads, long completionTime) {
        JsonObject embed = new JsonObject();
        
        String title = plugin.getConfig().getString("collectible_heads.discord.completion_title", 
            "🎉 Collection Complete!");
        String description = plugin.getConfig().getString("collectible_heads.discord.completion_description",
            "**%player%** has found all %total% collectible heads in the lobby!\n\n" +
            "⏰ Completion time: %time%\n" +
            "🏆 Amazing exploration skills!")
            .replace("%player%", player.getName())
            .replace("%total%", String.valueOf(totalHeads))
            .replace("%time%", formatCompletionTime(completionTime));
        
        embed.addProperty("title", title);
        embed.addProperty("description", description);
        embed.addProperty("color", 0x00ff00); // Green color
        embed.addProperty("timestamp", java.time.Instant.now().toString());
        
        JsonObject thumbnail = new JsonObject();
        thumbnail.addProperty("url", "https://crafatar.com/avatars/" + player.getUniqueId() + "?overlay=true&size=64");
        embed.add("thumbnail", thumbnail);
        
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Mysterria Network");
        String footerIconUrl = plugin.getConfig().getString("collectible_heads.discord.footer_icon_url", "");
        if (!footerIconUrl.isEmpty()) {
            footer.addProperty("icon_url", footerIconUrl);
        }
        embed.add("footer", footer);
        
        return embed;
    }
    
    private String formatCompletionTime(long completionTime) {
        if (completionTime <= 0) {
            return "Unknown";
        }
        
        long now = System.currentTimeMillis();
        long diffMillis = now - completionTime;
        long diffSeconds = diffMillis / 1000;
        long diffMinutes = diffSeconds / 60;
        long diffHours = diffMinutes / 60;
        
        if (diffHours > 0) {
            return diffHours + "h " + (diffMinutes % 60) + "m ago";
        } else if (diffMinutes > 0) {
            return diffMinutes + "m " + (diffSeconds % 60) + "s ago";
        } else {
            return diffSeconds + "s ago";
        }
    }
}