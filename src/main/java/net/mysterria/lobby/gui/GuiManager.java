package net.mysterria.lobby.gui;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.domain.actions.ActionExecutor;
import net.mysterria.lobby.util.SkullUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuiManager {

    private final MysterriaLobby plugin;
    private final Map<String, GuiDefinition> definitions = new HashMap<>();
    private final Map<UUID, Deque<String>> menuHistory = new ConcurrentHashMap<>();

    public GuiManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        copyDefaults();
        reload();
    }

    private void copyDefaults() {
        File guisFolder = new File(plugin.getDataFolder(), "guis");
        if (!guisFolder.exists()) guisFolder.mkdirs();
        for (String name : List.of("server_info.yml", "server_selector.yml", "language_menu.yml")) {
            File target = new File(guisFolder, name);
            if (!target.exists()) {
                try {
                    plugin.saveResource("guis/" + name, false);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void reload() {
        definitions.clear();

        // Load from guis/ folder — one file per GUI
        File guisFolder = new File(plugin.getDataFolder(), "guis");
        if (guisFolder.exists()) {
            File[] files = guisFolder.listFiles((d, n) -> n.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String id = file.getName().replace(".yml", "");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    definitions.put(id, new FileGuiDefinition(id, config));
                }
            }
        }

        // Backward compat: load from config.yml#menus (files take precedence)
        ConfigurationSection menusSection = plugin.getConfig().getConfigurationSection("menus");
        if (menusSection != null) {
            for (String menuId : menusSection.getKeys(false)) {
                if (!definitions.containsKey(menuId)) {
                    ConfigurationSection sec = menusSection.getConfigurationSection(menuId);
                    if (sec != null) definitions.put(menuId, new LegacyGuiDefinition(menuId, sec));
                }
            }
        }

        plugin.getLogger().info("Loaded " + definitions.size() + " GUIs");
    }

    public void openMenu(Player player, String menuId) {
        openMenu(player, menuId, null);
    }

    public void openMenu(Player player, String menuId, String previousMenuId) {
        GuiDefinition def = definitions.get(menuId);
        if (def == null) {
            plugin.getLogger().warning("GUI '" + menuId + "' not found!");
            return;
        }

        if (previousMenuId != null) {
            menuHistory.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>()).push(previousMenuId);
        }

        Component title = def.getTitle(player);
        int rows = def.getRows();

        Gui gui = Gui.gui().title(title).rows(rows).disableAllInteractions().create();

        for (ItemDefinition itemDef : def.getItems()) {
            gui.setItem(itemDef.slot, buildGuiItem(player, itemDef, def, menuId));
        }

        if (def.isFillEnabled()) {
            Material fill = def.getFillMaterial();
            GuiItem filler = PaperItemBuilder.from(fill).name(Component.empty())
                    .asGuiItem(e -> e.setCancelled(true));
            for (int i = 0; i < rows * 9; i++) {
                if (gui.getGuiItem(i) == null) gui.setItem(i, filler);
            }
        }

        gui.open(player);
    }

    private GuiItem buildGuiItem(Player player, ItemDefinition itemDef, GuiDefinition def, String currentMenuId) {
        Component displayName = def.getItemDisplayName(player, itemDef.id);
        List<Component> lore = def.getItemLore(player, itemDef.id);

        ItemStack item;
        if (itemDef.material == Material.PLAYER_HEAD && !itemDef.textureUrl.isEmpty()) {
            item = SkullUtil.createCustomHead(itemDef.textureUrl);
        } else {
            item = new ItemStack(itemDef.material, Math.max(1, itemDef.amount));
        }

        PaperItemBuilder builder = PaperItemBuilder.from(item).name(displayName).lore(lore);
        if (itemDef.customModelData > 0) builder.model(itemDef.customModelData);

        List<String> actions = itemDef.actions;
        return new GuiItem(builder.build(), event -> {
            event.setCancelled(true);
            if (actions == null || actions.isEmpty()) return;
            // Push current menu to history if any action opens another GUI
            boolean opensMenu = actions.stream().anyMatch(a -> a.startsWith("[MENU] "));
            if (opensMenu) {
                menuHistory.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>()).push(currentMenuId);
            }
            ActionExecutor.executeActions(player, actions);
        });
    }

    public void goBack(Player player) {
        Deque<String> stack = menuHistory.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            player.closeInventory();
            return;
        }
        openMenu(player, stack.pop(), null);
    }

    public void clearHistory(Player player) {
        menuHistory.remove(player.getUniqueId());
    }

    public boolean hasMenu(String menuId) {
        return definitions.containsKey(menuId);
    }

    // -- Definition types --

    private interface GuiDefinition {
        Component getTitle(Player player);
        int getRows();
        boolean isFillEnabled();
        Material getFillMaterial();
        List<ItemDefinition> getItems();
        Component getItemDisplayName(Player player, String itemId);
        List<Component> getItemLore(Player player, String itemId);
    }

    private class FileGuiDefinition implements GuiDefinition {
        private final String id;
        private final FileConfiguration config;

        FileGuiDefinition(String id, FileConfiguration config) {
            this.id = id;
            this.config = config;
        }

        @Override public Component getTitle(Player player) {
            return plugin.getLangManager().getLocalizedComponent(player, config, "title");
        }

        @Override public int getRows() { return config.getInt("rows", 3); }

        @Override public boolean isFillEnabled() { return config.getBoolean("fill.enabled", true); }

        @Override public Material getFillMaterial() {
            try { return Material.valueOf(config.getString("fill.material", "GRAY_STAINED_GLASS_PANE")); }
            catch (Exception e) { return Material.GRAY_STAINED_GLASS_PANE; }
        }

        @Override public List<ItemDefinition> getItems() {
            ConfigurationSection sec = config.getConfigurationSection("items");
            return sec != null ? parseItems(sec) : List.of();
        }

        @Override public Component getItemDisplayName(Player player, String itemId) {
            return plugin.getLangManager().getLocalizedComponent(player, config, "items." + itemId + ".display_name");
        }

        @Override public List<Component> getItemLore(Player player, String itemId) {
            return plugin.getLangManager().getLocalizedComponentList(player, config, "items." + itemId + ".lore");
        }
    }

    private class LegacyGuiDefinition implements GuiDefinition {
        private final String id;
        private final ConfigurationSection section;

        LegacyGuiDefinition(String id, ConfigurationSection section) {
            this.id = id;
            this.section = section;
        }

        @Override public Component getTitle(Player player) {
            return plugin.getLangManager().getLocalizedComponent(player, "menus." + id + ".title");
        }

        @Override public int getRows() { return section.getInt("rows", 1); }
        @Override public boolean isFillEnabled() { return true; }
        @Override public Material getFillMaterial() { return Material.GRAY_STAINED_GLASS_PANE; }

        @Override public List<ItemDefinition> getItems() {
            ConfigurationSection sec = section.getConfigurationSection("items");
            return sec != null ? parseItems(sec) : List.of();
        }

        @Override public Component getItemDisplayName(Player player, String itemId) {
            return plugin.getLangManager().getLocalizedComponent(player, "menus." + id + ".items." + itemId + ".display_name");
        }

        @Override public List<Component> getItemLore(Player player, String itemId) {
            return plugin.getLangManager().getLocalizedComponentList(player, "menus." + id + ".items." + itemId + ".lore");
        }
    }

    private List<ItemDefinition> parseItems(ConfigurationSection itemsSection) {
        List<ItemDefinition> items = new ArrayList<>();
        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection s = itemsSection.getConfigurationSection(itemId);
            if (s == null) continue;
            Material mat;
            try { mat = Material.valueOf(s.getString("material", "STONE")); }
            catch (Exception e) { mat = Material.STONE; }
            String texture = s.getString("texture_url", "");
            items.add(new ItemDefinition(
                    itemId,
                    s.getInt("slot", 0),
                    mat,
                    s.getInt("amount", 1),
                    s.getInt("custom_model_data", 0),
                    texture != null ? texture : "",
                    s.getStringList("actions")
            ));
        }
        return items;
    }

    private record ItemDefinition(String id, int slot, Material material, int amount,
                                   int customModelData, String textureUrl, List<String> actions) {}
}
