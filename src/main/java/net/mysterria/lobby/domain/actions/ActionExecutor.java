package net.mysterria.lobby.domain.actions;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

public class ActionExecutor {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static void executeActions(Player player, List<String> actions) {
        if (actions == null || actions.isEmpty()) return;

        for (String action : actions) {
            executeAction(player, action);
        }
    }

    public static void executeAction(Player player, String action) {
        if (action == null || action.isEmpty()) return;

        if (action.startsWith("[COMMAND] ")) {
            executeCommand(player, action.substring(10));
        } else if (action.startsWith("[MENU] ")) {
            openMenu(player, action.substring(7));
        } else if (action.startsWith("[MESSAGE] ")) {
            sendMessage(player, action.substring(10));
        } else if (action.startsWith("[CLOSE]")) {
            player.closeInventory();
        } else if (action.startsWith("[CONSOLE] ")) {
            executeConsoleCommand(player, action.substring(10));
        } else if (action.equals("[TOGGLE_VISIBILITY]")) {
            MysterriaLobby.getInstance().getPlayerVisibilityManager().togglePlayerVisibility(player);
        } else if (action.startsWith("[TELEPORT] ")) {
            String serverName = action.substring(11);
            MysterriaLobby.getInstance().getTeleportManager().teleportToServer(player, serverName);
        } else if (action.equals("[SERVER_SELECTOR]")) {
            openMenu(player, "server_selector");
        } else if (action.startsWith("[SOUND] ")) {
            playSound(player, action.substring(8));
        }
    }

    private static void executeCommand(Player player, String command) {
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        player.performCommand(command);
    }

    private static void executeConsoleCommand(Player player, String command) {
        command = command.replace("{player}", player.getName());
        MysterriaLobby.getInstance().getServer().dispatchCommand(
                MysterriaLobby.getInstance().getServer().getConsoleSender(),
                command
        );
    }

    private static void openMenu(Player player, String menuId) {
        if (MysterriaLobby.getInstance().getTriumphMenuManager().hasMenu(menuId)) {
            MysterriaLobby.getInstance().getTriumphMenuManager().openMenu(player, menuId);
        }
    }

    private static void sendMessage(Player player, String message) {
        String localizedMessage = MysterriaLobby.getInstance().getLangManager()
                .getLocalizedString(player, message);
        player.sendMessage(miniMessage.deserialize(localizedMessage));
    }

    private static void playSound(Player player, String soundData) {
        try {
            String[] parts = soundData.split(" ");
            Sound sound = Sound.valueOf(parts[0]);
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            MysterriaLobby.getInstance().getLogger().warning("Invalid sound format: " + soundData);
        }
    }
}