package net.mysterria.lobby.listeners;

import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.domain.actions.ActionExecutor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class MenuListener implements Listener {

    private final NamespacedKey actionsKey;

    public MenuListener(MysterriaLobby plugin) {
        this.actionsKey = new NamespacedKey(plugin, "actions");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;

        String actionsString = item.getItemMeta().getPersistentDataContainer()
                .get(actionsKey, PersistentDataType.STRING);

        if (actionsString != null) {
            event.setCancelled(true);
            List<String> actions = Arrays.asList(actionsString.split(";"));
            ActionExecutor.executeActions(player, actions);
        }
    }
}