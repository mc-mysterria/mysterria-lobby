package net.mysterria.lobby.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.domain.collectibles.CollectibleHead;
import net.mysterria.lobby.domain.collectibles.CollectibleHeadsManager;
import net.mysterria.lobby.domain.collectibles.PlayerCollectionProgress;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

@Command(name = "collectibles", aliases = {"collect", "heads"})
public class CollectiblesCommand {
    
    private final MysterriaLobby plugin;
    private final CollectibleHeadsManager headsManager;
    
    public CollectiblesCommand(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.headsManager = plugin.getCollectibleHeadsManager();
    }
    
    @Execute
    public void executeProgress(@Context Player player) {
        if (!headsManager.isEnabled()) {
            String disabledMessage = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.system_disabled");
            player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(disabledMessage));
            return;
        }
        
        PlayerCollectionProgress progress = headsManager.getPlayerProgress(player);
        
        String message = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.progress_info")
            .replace("%collected%", String.valueOf(progress.getCollectedCount()))
            .replace("%total%", String.valueOf(headsManager.getTotalHeads()))
            .replace("%percentage%", String.format("%.1f", progress.getCompletionPercentage(headsManager.getTotalHeads())));
        
        player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(message));
        
        if (progress.getCollectedCount() > 0) {
            String collectedListHeader = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.collected_list_header");
            player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(collectedListHeader));
            
            for (String headId : progress.getCollectedHeads()) {
                CollectibleHead head = headsManager.getHeadById(headId);
                if (head != null) {
                    String headName = plugin.getLangManager().getLocalizedString(player, "collectible_heads.head_names." + headId);
                    String listItem = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.collected_list_item")
                        .replace("%head_name%", headName);
                    
                    String listItemMessage = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.collected_list_item")
                        .replace("%head_name%", headName);
                    player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(listItemMessage));
                }
            }
        }
    }
    
    @Execute(name = "reload")
    @Permission("mysterria.lobby.collectibles.reload")
    public void executeReload(@Context Player player) {
        headsManager.reload();
        
        String message = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.system_reloaded")
            .replace("%count%", String.valueOf(headsManager.getTotalHeads()));
        
        String reloadMessage = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.system_reloaded")
            .replace("%count%", String.valueOf(headsManager.getTotalHeads()));
        player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(reloadMessage));
    }
    
    @Execute(name = "list")
    @Permission("mysterria.lobby.collectibles.list")
    public void executeList(@Context Player player) {
        if (!headsManager.isEnabled()) {
            String disabledMessage = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.system_disabled");
            player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(disabledMessage));
            return;
        }
        
        Collection<CollectibleHead> allHeads = headsManager.getAllHeads();
        
        String header = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.all_heads_header")
            .replace("%total%", String.valueOf(allHeads.size()));
        
        String headerMessage = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.all_heads_header")
            .replace("%total%", String.valueOf(allHeads.size()));
        player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(headerMessage));
        
        for (CollectibleHead head : allHeads) {
            String headName = plugin.getLangManager().getLocalizedString(player, "collectible_heads.head_names." + head.getId());
            String location = String.format("%.0f, %.0f, %.0f in %s", 
                head.getLocation().getX(), 
                head.getLocation().getY(), 
                head.getLocation().getZ(), 
                head.getLocation().getWorld().getName());
            
            String listItem = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.head_list_item")
                .replace("%head_id%", head.getId())
                .replace("%head_name%", headName)
                .replace("%location%", location);
            
            String headListMessage = plugin.getLangManager().getLocalizedString(player, "collectible_heads.messages.head_list_item")
                .replace("%head_id%", head.getId())
                .replace("%head_name%", headName)
                .replace("%location%", location);
            player.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(headListMessage));
        }
    }
    
    @Execute(name = "progress")
    @Permission("mysterria.lobby.collectibles.progress.others")
    public void executeProgressOther(@Context Player sender, @Arg("player") String targetPlayerName) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = plugin.getLangManager().getLocalizedString(sender, "collectible_heads.messages.player_not_found")
                .replace("%player%", targetPlayerName);
            
            String notFoundMessage = plugin.getLangManager().getLocalizedString(sender, "collectible_heads.messages.player_not_found")
                .replace("%player%", targetPlayerName);
            sender.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(notFoundMessage));
            return;
        }
        
        if (!headsManager.isEnabled()) {
            String disabledMessage = plugin.getLangManager().getLocalizedString(sender, "collectible_heads.messages.system_disabled");
            sender.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(disabledMessage));
            return;
        }
        
        PlayerCollectionProgress progress = headsManager.getPlayerProgress(targetPlayer);
        
        String message = plugin.getLangManager().getLocalizedString(sender, "collectible_heads.messages.other_progress_info")
            .replace("%player%", targetPlayer.getName())
            .replace("%collected%", String.valueOf(progress.getCollectedCount()))
            .replace("%total%", String.valueOf(headsManager.getTotalHeads()))
            .replace("%percentage%", String.format("%.1f", progress.getCompletionPercentage(headsManager.getTotalHeads())));
        
        String otherProgressMessage = plugin.getLangManager().getLocalizedString(sender, "collectible_heads.messages.other_progress_info")
            .replace("%player%", targetPlayer.getName())
            .replace("%collected%", String.valueOf(progress.getCollectedCount()))
            .replace("%total%", String.valueOf(headsManager.getTotalHeads()))
            .replace("%percentage%", String.format("%.1f", progress.getCompletionPercentage(headsManager.getTotalHeads())));
        sender.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(otherProgressMessage));
    }
    
    @Execute(name = "reset")
    @Permission("mysterria.lobby.collectibles.reset.others")
    public void executeReset(@Context Player sender, @Arg("player") String targetPlayerName) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = plugin.getLangManager().getLocalizedString(sender, "collectible_heads.messages.player_not_found")
                .replace("%player%", targetPlayerName);
            
            String notFoundMessage = plugin.getLangManager().getLocalizedString(sender, "collectible_heads.messages.player_not_found")
                .replace("%player%", targetPlayerName);
            sender.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(notFoundMessage));
            return;
        }
        
        headsManager.getCollectionPersistence().clearProgress(targetPlayer.getUniqueId());
        
        String message = plugin.getLangManager().getLocalizedString(sender, "collectible_heads.messages.progress_reset")
            .replace("%player%", targetPlayer.getName());
        
        String resetMessage = plugin.getLangManager().getLocalizedString(sender, "collectible_heads.messages.progress_reset")
            .replace("%player%", targetPlayer.getName());
        sender.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(resetMessage));
        
        if (targetPlayer.isOnline()) {
            String playerMessage = plugin.getLangManager().getLocalizedString(targetPlayer, "collectible_heads.messages.your_progress_reset");
            targetPlayer.sendMessage(plugin.getLangManager().getMiniMessage().deserialize(playerMessage));
        }
    }
}