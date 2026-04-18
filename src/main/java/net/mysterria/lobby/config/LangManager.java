package net.mysterria.lobby.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LangManager {

    private final MysterriaLobby plugin;
    private final MiniMessage miniMessage;
    private String defaultLang;
    private final Map<String, Boolean> availableLanguages = new ConcurrentHashMap<>();
    private final Map<UUID, String> temporaryLanguageOverrides = new ConcurrentHashMap<>();

    public LangManager(MysterriaLobby plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        reload();
    }

    public void reload() {
        this.defaultLang = plugin.getConfig().getString("language.default", "en");
        availableLanguages.clear();

        // Prefer explicit list in config — simpler than auto-detection
        List<String> explicit = plugin.getConfig().getStringList("language.available");
        if (!explicit.isEmpty()) {
            for (String lang : explicit) availableLanguages.put(lang, true);
        } else {
            // Backward compat: scan join.items and menus for language keys
            ConfigurationSection joinItems = plugin.getConfig().getConfigurationSection("join.items");
            if (joinItems != null) {
                for (String itemKey : joinItems.getKeys(false)) {
                    ConfigurationSection displayName = joinItems.getConfigurationSection(itemKey + ".display_name");
                    if (displayName != null) {
                        for (String lang : displayName.getKeys(false)) availableLanguages.put(lang, true);
                    }
                }
            }
            ConfigurationSection menus = plugin.getConfig().getConfigurationSection("menus");
            if (menus != null) {
                for (String menuKey : menus.getKeys(false)) {
                    ConfigurationSection title = menus.getConfigurationSection(menuKey + ".title");
                    if (title != null) {
                        for (String lang : title.getKeys(false)) availableLanguages.put(lang, true);
                    }
                }
            }
        }

        if (!availableLanguages.containsKey(defaultLang)) availableLanguages.put(defaultLang, true);
    }

    public String getPlayerLang(Player player) {
        String tempLang = temporaryLanguageOverrides.get(player.getUniqueId());
        if (tempLang != null && availableLanguages.containsKey(tempLang)) return tempLang;
        return mapClientLocaleToLanguage(player.locale());
    }

    public void setPlayerLang(Player player, String lang) {
        if (availableLanguages.containsKey(lang)) temporaryLanguageOverrides.put(player.getUniqueId(), lang);
    }

    public void clearPlayerLangOverride(Player player) {
        temporaryLanguageOverrides.remove(player.getUniqueId());
    }

    public String getClientLang(Player player) {
        return mapClientLocaleToLanguage(player.locale());
    }

    private String mapClientLocaleToLanguage(Locale clientLocale) {
        String language = clientLocale.getLanguage().toLowerCase();
        String fullLocale = language + "_" + clientLocale.getCountry().toLowerCase();

        if (language.startsWith("uk") || language.startsWith("ua") || fullLocale.startsWith("uk"))
            return availableLanguages.containsKey("ua") ? "ua" : defaultLang;
        if (language.startsWith("es") || fullLocale.startsWith("es"))
            return availableLanguages.containsKey("es") ? "es" : defaultLang;
        if (language.startsWith("en") || fullLocale.startsWith("en"))
            return availableLanguages.containsKey("en") ? "en" : defaultLang;

        return defaultLang;
    }

    public boolean isUsingClientLocale(Player player) {
        return !temporaryLanguageOverrides.containsKey(player.getUniqueId());
    }

    public void onPlayerQuit(Player player) {
        temporaryLanguageOverrides.remove(player.getUniqueId());
    }

    // -- config.yml-backed methods --

    public Component getLocalizedComponent(Player player, String path) {
        return miniMessage.deserialize(getLocalizedString(player, path)).decoration(TextDecoration.ITALIC, false);
    }

    public List<Component> getLocalizedComponentList(Player player, String path) {
        return getLocalizedStringList(player, path).stream()
                .map(s -> miniMessage.deserialize(s).decoration(TextDecoration.ITALIC, false))
                .toList();
    }

    public String getLocalizedString(Player player, String path) {
        String lang = getPlayerLang(player);
        String text = plugin.getConfig().getString(path + "." + lang);
        if (text == null) text = plugin.getConfig().getString(path + "." + defaultLang);
        if (text == null) text = "Missing translation: " + path;
        return text;
    }

    public List<String> getLocalizedStringList(Player player, String path) {
        String lang = getPlayerLang(player);
        List<String> texts = plugin.getConfig().getStringList(path + "." + lang);
        if (texts.isEmpty()) texts = plugin.getConfig().getStringList(path + "." + defaultLang);
        return texts;
    }

    // -- FileConfiguration-backed methods (for per-GUI-file localization) --

    public Component getLocalizedComponent(Player player, FileConfiguration config, String path) {
        return miniMessage.deserialize(getLocalizedString(player, config, path)).decoration(TextDecoration.ITALIC, false);
    }

    public List<Component> getLocalizedComponentList(Player player, FileConfiguration config, String path) {
        return getLocalizedStringList(player, config, path).stream()
                .map(s -> miniMessage.deserialize(s).decoration(TextDecoration.ITALIC, false))
                .toList();
    }

    public String getLocalizedString(Player player, FileConfiguration config, String path) {
        String lang = getPlayerLang(player);
        String text = config.getString(path + "." + lang);
        if (text == null) text = config.getString(path + "." + defaultLang);
        if (text == null) text = "Missing: " + path;
        return text;
    }

    public List<String> getLocalizedStringList(Player player, FileConfiguration config, String path) {
        String lang = getPlayerLang(player);
        List<String> texts = config.getStringList(path + "." + lang);
        if (texts.isEmpty()) texts = config.getStringList(path + "." + defaultLang);
        return texts;
    }

    public String getDefaultLang() { return defaultLang; }
    public boolean isLanguageAvailable(String lang) { return availableLanguages.containsKey(lang); }
    public java.util.Set<String> getAvailableLanguages() { return availableLanguages.keySet(); }
    public MiniMessage getMiniMessage() { return miniMessage; }
}
