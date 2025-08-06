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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriumphMenuManager {

    private final MysterriaLobby plugin;
    private final Map<String, MenuData> menuConfigs = new HashMap<>();

    public TriumphMenuManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        menuConfigs.clear();
        loadMenus();
    }

    private void loadMenus() {
        ConfigurationSection menusSection = plugin.getConfig().getConfigurationSection("menus");
        if (menusSection == null) return;

        for (String menuId : menusSection.getKeys(false)) {
            ConfigurationSection menuSection = menusSection.getConfigurationSection(menuId);
            if (menuSection == null) continue;

            int rows = menuSection.getInt("rows", 1);
            MenuData menuData = new MenuData(menuId, rows);

            ConfigurationSection itemsSection = menuSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemId : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                    if (itemSection == null) continue;

                    int slot = itemSection.getInt("slot", 0);
                    Material material = Material.valueOf(itemSection.getString("material", "STONE"));
                    String displayNamePath = "menus." + menuId + ".items." + itemId + ".display_name";
                    String lorePath = "menus." + menuId + ".items." + itemId + ".lore";
                    List<String> actions = itemSection.getStringList("actions");
                    int customModelData = itemSection.getInt("custom_model_data", 0);
                    String textureUrl = itemSection.getString("texture_url", "");

                    MenuItemData itemData = new MenuItemData(slot, material, displayNamePath, lorePath, actions, customModelData, textureUrl);
                    menuData.addItem(itemData);
                }
            }

            menuConfigs.put(menuId, menuData);
        }

        plugin.getLogger().info("Loaded " + menuConfigs.size() + " menus with Triumph GUI");
    }

    public void openMenu(Player player, String menuId) {
        MenuData menuData = menuConfigs.get(menuId);
        if (menuData == null) {
            plugin.getLogger().warning("Menu '" + menuId + "' not found!");
            return;
        }

        Component title = plugin.getLangManager().getLocalizedComponent(player, "menus." + menuId + ".title");

        Gui gui = Gui.gui()
                .title(title)
                .rows(menuData.rows)
                .disableAllInteractions()
                .create();

        for (MenuItemData itemData : menuData.items.values()) {
            GuiItem guiItem = createGuiItem(player, itemData);
            gui.setItem(itemData.slot, guiItem);
        }

        addDecorations(gui, menuData);

        gui.open(player);
    }

    private GuiItem createGuiItem(Player player, MenuItemData itemData) {
        Component displayName = plugin.getLangManager().getLocalizedComponent(player, itemData.displayNamePath);
        List<Component> lore = plugin.getLangManager().getLocalizedComponentList(player, itemData.lorePath);

        ItemStack item;
        
        // Handle custom player head textures
        if (itemData.material == Material.PLAYER_HEAD && !itemData.textureUrl.isEmpty()) {
            plugin.getLogger().info("Creating custom head with texture: " + itemData.textureUrl);
            item = SkullUtil.createCustomHead(itemData.textureUrl);
        } else {
            item = new ItemStack(itemData.material);
        }
        
        PaperItemBuilder builder = PaperItemBuilder.from(item)
                .name(displayName)
                .lore(lore);

        if (itemData.customModelData > 0) {
            builder.model(itemData.customModelData);
        }

        return new GuiItem(builder.build(), event -> {
            event.setCancelled(true);
            if (itemData.actions != null && !itemData.actions.isEmpty()) {
                ActionExecutor.executeActions(player, itemData.actions);
            }
        });
    }

    public boolean hasMenu(String menuId) {
        return menuConfigs.containsKey(menuId);
    }

    private void addDecorations(Gui gui, MenuData menuData) {
        GuiItem glassPane = PaperItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.empty())
                .asGuiItem(event -> event.setCancelled(true));

        int totalSlots = menuData.rows * 9;
        for (int i = 0; i < totalSlots; i++) {
            if (gui.getGuiItem(i) == null) {
                gui.setItem(i, glassPane);
            }
        }
    }

    private static class MenuData {
        private final String id;
        private final int rows;
        private final Map<Integer, MenuItemData> items = new HashMap<>();

        public MenuData(String id, int rows) {
            this.id = id;
            this.rows = rows;
        }

        public void addItem(MenuItemData item) {
            items.put(item.slot, item);
        }
    }

    private static class MenuItemData {
        private final int slot;
        private final Material material;
        private final String displayNamePath;
        private final String lorePath;
        private final List<String> actions;
        private final int customModelData;
        private final String textureUrl;

        public MenuItemData(int slot, Material material, String displayNamePath, String lorePath,
                            List<String> actions, int customModelData, String textureUrl) {
            this.slot = slot;
            this.material = material;
            this.displayNamePath = displayNamePath;
            this.lorePath = lorePath;
            this.actions = actions;
            this.customModelData = customModelData;
            this.textureUrl = textureUrl != null ? textureUrl : "";
        }
    }
}