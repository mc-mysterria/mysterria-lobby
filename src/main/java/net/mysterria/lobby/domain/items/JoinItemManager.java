package net.mysterria.lobby.domain.items;

import net.mysterria.lobby.MysterriaLobby;
import net.mysterria.lobby.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class JoinItemManager {
    
    private final MysterriaLobby plugin;
    private final NamespacedKey actionsKey;
    
    public JoinItemManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.actionsKey = new NamespacedKey(plugin, "actions");
    }
    
    public void giveJoinItems(Player player) {
        ConfigurationSection joinItemsSection = plugin.getConfig().getConfigurationSection("join.items");
        if (joinItemsSection == null) return;
        
        for (String itemId : joinItemsSection.getKeys(false)) {
            ConfigurationSection itemSection = joinItemsSection.getConfigurationSection(itemId);
            if (itemSection == null) continue;
            
            int slot = itemSection.getInt("slot", 0);
            Material material = Material.valueOf(itemSection.getString("material", "STONE"));
            int amount = itemSection.getInt("amount", 1);
            String displayNamePath = "join.items." + itemId + ".display_name";
            String lorePath = "join.items." + itemId + ".lore";
            int customModelData = itemSection.getInt("custom_model_data", 0);
            List<String> actions = itemSection.getStringList("actions");
            
            ItemBuilder builder = new ItemBuilder(material, amount)
                    .localizedDisplayName(player, displayNamePath)
                    .localizedLore(player, lorePath)
                    .unbreakable(true);
            
            if (customModelData > 0) {
                builder.customModelData(customModelData);
            }
            
            ItemStack item = builder.build();
            
            if (actions != null && !actions.isEmpty()) {
                item.editMeta(meta -> {
                    meta.getPersistentDataContainer().set(
                            actionsKey,
                            PersistentDataType.STRING,
                            String.join(";", actions)
                    );
                });
            }
            
            player.getInventory().setItem(slot, item);
        }
    }
}