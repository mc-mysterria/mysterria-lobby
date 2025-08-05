package net.mysterria.lobby.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.description.Description;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "lobby")
public class LobbyCommands {
    
    private final MysterriaLobby plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    public LobbyCommands(MysterriaLobby plugin) {
        this.plugin = plugin;
    }
    
    @Execute(name = "reload")
    @Permission("mysterria.lobby.reload")
    @Description("Reload all lobby configurations")
    public void reload(@Context CommandSender sender) {
        long startTime = System.currentTimeMillis();
        
        try {
            plugin.reload();
            long duration = System.currentTimeMillis() - startTime;
            
            sender.sendMessage(miniMessage.deserialize(
                "<gradient:#4CAF50:#45a049>✅ MysterriaLobby configuration reloaded successfully! " +
                "<gray>(" + duration + "ms)</gray></gradient>"
            ));
        } catch (Exception e) {
            sender.sendMessage(miniMessage.deserialize(
                "<red>❌ Failed to reload configuration: " + e.getMessage() + "</red>"
            ));
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Execute(name = "setlang")
    @Description("Change your language preference or use 'auto' to use client locale")
    public void setLanguage(@Context Player player, @Arg(value = "language") String language) {
        if (!plugin.getConfigManager().isManualLanguageSelectionAllowed()) {
            player.sendMessage(miniMessage.deserialize(
                "<red>⛔ Manual language selection is disabled.</red>"
            ));
            return;
        }
        
        if (language.isEmpty()) {
            String currentLang = plugin.getLangManager().getPlayerLang(player);
            String clientLang = plugin.getLangManager().getClientLang(player);
            boolean usingClientLocale = plugin.getLangManager().isUsingClientLocale(player);
            
            player.sendMessage(miniMessage.deserialize(
                "<gradient:#ffd700:#ffaa00>🌍 Your current language: <white>" + currentLang + "</white></gradient>"
            ));
            player.sendMessage(miniMessage.deserialize(
                "<gray>Client locale: <white>" + clientLang + "</white> " + 
                (usingClientLocale ? "<green>(auto-detected)</green>" : "<yellow>(overridden)</yellow>") + "</gray>"
            ));
            player.sendMessage(miniMessage.deserialize(
                "<gray>Available languages: <yellow>" + 
                String.join(", ", plugin.getLangManager().getAvailableLanguages()) + "</yellow></gray>"
            ));
            player.sendMessage(miniMessage.deserialize(
                "<gray>Use <yellow>/lobby setlang auto</yellow> to revert to client locale</gray>"
            ));
            return;
        }
        
        String newLang = language.toLowerCase();
        
        if ("auto".equals(newLang)) {
            plugin.getLangManager().clearPlayerLangOverride(player);
            String clientLang = plugin.getLangManager().getClientLang(player);
            String actualLang = plugin.getLangManager().getPlayerLang(player);
            
            player.sendMessage(miniMessage.deserialize(
                "<gradient:#4CAF50:#45a049>✅ Language reverted to client locale: <white>" + actualLang + "</white></gradient>"
            ));
            if (!clientLang.equals(actualLang)) {
                player.sendMessage(miniMessage.deserialize(
                    "<gray>Note: Your client language '<yellow>" + clientLang + "</yellow>' is not supported, using default.</gray>"
                ));
            }
            player.sendMessage(miniMessage.deserialize(
                "<gray>Language will auto-detect from client on next join.</gray>"
            ));
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.getInventory().clear();
                plugin.getJoinItemManager().giveJoinItems(player);
                plugin.getPlayerVisibilityManager().updateVisibilityItem(player);
            }, 1L);
            return;
        }
        
        if (!plugin.getLangManager().isLanguageAvailable(newLang)) {
            player.sendMessage(miniMessage.deserialize(
                "<red>❌ Language '<yellow>" + newLang + "</yellow>' is not available.</red>"
            ));
            player.sendMessage(miniMessage.deserialize(
                "<gray>Available languages: <yellow>" + 
                String.join(", ", plugin.getLangManager().getAvailableLanguages()) + "</yellow></gray>"
            ));
            return;
        }
        
        plugin.getLangManager().setPlayerLang(player, newLang);
        player.sendMessage(miniMessage.deserialize(
            "<gradient:#4CAF50:#45a049>✅ Language temporarily set to: <white>" + newLang + "</white></gradient>"
        ));
        player.sendMessage(miniMessage.deserialize(
            "<gray>This override will reset when you rejoin the server.</gray>"
        ));
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.getInventory().clear();
            plugin.getJoinItemManager().giveJoinItems(player);
            plugin.getPlayerVisibilityManager().updateVisibilityItem(player);
        }, 1L);
    }
    
    @Execute(name = "visibility")
    @Description("Toggle player visibility")
    public void toggleVisibility(@Context Player player) {
        plugin.getPlayerVisibilityManager().togglePlayerVisibility(player);
    }
    
    @Execute(name = "spawn")
    @Permission("mysterria.lobby.spawn")
    @Description("Set the spawn location for the lobby")
    public void setSpawn(@Context Player player) {
        plugin.getSpawnManager().setSpawnLocation(player.getLocation());
        player.sendMessage(miniMessage.deserialize(
            "<gradient:#4CAF50:#45a049>✅ Spawn location set at your current position!</gradient>"
        ));
    }
    
    @Execute(name = "info")
    @Description("Display lobby information")
    public void info(@Context CommandSender sender) {
        sender.sendMessage(miniMessage.deserialize(
            "<gradient:#667eea:#764ba2><bold>🔮 MysterriaLobby v" + plugin.getDescription().getVersion() + "</bold></gradient>"
        ));
        sender.sendMessage(miniMessage.deserialize(
            "<gray>→ Teleport Zones: <yellow>" + plugin.getTeleportManager().getZones().size() + "</yellow></gray>"
        ));
        sender.sendMessage(miniMessage.deserialize(
            "<gray>→ Available Languages: <yellow>" + 
            String.join(", ", plugin.getLangManager().getAvailableLanguages()) + "</yellow></gray>"
        ));
        sender.sendMessage(miniMessage.deserialize(
            "<gray>→ Author: <yellow>Mysterria</yellow></gray>"
        ));
    }
}