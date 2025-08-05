package net.mysterria.lobby.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Stream;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }
    
    public ItemBuilder(Material material, String textureUrl) {
        if (material == Material.PLAYER_HEAD && textureUrl != null && !textureUrl.isEmpty()) {
            this.item = SkullUtil.createCustomHead(textureUrl);
        } else {
            this.item = new ItemStack(material);
        }
        this.meta = item.getItemMeta();
    }

    public ItemBuilder displayName(Component name) {
        meta.displayName(name);
        return this;
    }

    public ItemBuilder displayName(String name) {
        meta.displayName(miniMessage.deserialize(name));
        return this;
    }

    public ItemBuilder localizedDisplayName(Player player, String path) {
        String name = MysterriaLobby.getInstance().getLangManager().getLocalizedString(player, path);
        meta.displayName(miniMessage.deserialize(name).decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder lore(List<Component> lore) {
        meta.lore(lore);
        return this;
    }

    public ItemBuilder lore(String... lore) {
        List<Component> loreComponents = Stream.of(lore)
                .map(miniMessage::deserialize)
                .toList();
        meta.lore(loreComponents);
        return this;
    }

    public ItemBuilder localizedLore(Player player, String path) {
        List<Component> lore = MysterriaLobby.getInstance().getLangManager().getLocalizedComponentList(player, path);
        meta.lore(lore);
        return this;
    }

    public ItemBuilder customModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }
    
    public ItemBuilder skullTexture(String textureUrl) {
        if (item.getType() == Material.PLAYER_HEAD && textureUrl != null && !textureUrl.isEmpty()) {
            ItemStack customHead = SkullUtil.createCustomHead(textureUrl);
            // Copy existing meta to the new head
            customHead.setItemMeta(meta);
            return new ItemBuilder(customHead);
        }
        return this;
    }
    
    private ItemBuilder(ItemStack existingItem) {
        this.item = existingItem;
        this.meta = item.getItemMeta();
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}