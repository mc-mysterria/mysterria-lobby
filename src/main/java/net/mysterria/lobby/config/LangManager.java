package net.mysterria.lobby.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.configuration.ConfigurationSection;
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
        ConfigurationSection joinItems = plugin.getConfig().getConfigurationSection("join.items");
        if (joinItems != null) {
            for (String itemKey : joinItems.getKeys(false)) {
                ConfigurationSection displayName = joinItems.getConfigurationSection(itemKey + ".display_name");
                if (displayName != null) {
                    for (String lang : displayName.getKeys(false)) {
                        availableLanguages.put(lang, true);
                    }
                }
            }
        }

        ConfigurationSection menus = plugin.getConfig().getConfigurationSection("menus");
        if (menus != null) {
            for (String menuKey : menus.getKeys(false)) {
                ConfigurationSection title = menus.getConfigurationSection(menuKey + ".title");
                if (title != null) {
                    for (String lang : title.getKeys(false)) {
                        availableLanguages.put(lang, true);
                    }
                }
            }
        }

        if (!availableLanguages.containsKey(defaultLang)) {
            availableLanguages.put(defaultLang, true);
        }
    }

    public String getPlayerLang(Player player) {
        String tempLang = temporaryLanguageOverrides.get(player.getUniqueId());
        if (tempLang != null && availableLanguages.containsKey(tempLang)) {
            return tempLang;
        }
        
        Locale clientLocale = player.locale();
        String detectedLanguage = mapClientLocaleToLanguage(clientLocale);
        
        return detectedLanguage;
    }

    public void setPlayerLang(Player player, String lang) {
        if (availableLanguages.containsKey(lang)) {
            temporaryLanguageOverrides.put(player.getUniqueId(), lang);
        }
    }

    public void clearPlayerLangOverride(Player player) {
        temporaryLanguageOverrides.remove(player.getUniqueId());
    }

    public String getClientLang(Player player) {
        Locale clientLocale = player.locale();
        return mapClientLocaleToLanguage(clientLocale);
    }

    private String mapClientLocaleToLanguage(Locale clientLocale) {
        String language = clientLocale.getLanguage().toLowerCase();
        String country = clientLocale.getCountry().toLowerCase();
        String fullLocale = language + "_" + country;

        if (language.startsWith("uk") || language.startsWith("ua") || fullLocale.startsWith("uk")) {
            return availableLanguages.containsKey("ua") ? "ua" : defaultLang;
        }
        
        if (language.startsWith("es") || fullLocale.startsWith("es")) {
            return availableLanguages.containsKey("es") ? "es" : defaultLang;
        }
        
        if (language.startsWith("en") || fullLocale.startsWith("en")) {
            return availableLanguages.containsKey("en") ? "en" : defaultLang;
        }
        
        return defaultLang;
    }

    public boolean isUsingClientLocale(Player player) {
        return !temporaryLanguageOverrides.containsKey(player.getUniqueId());
    }

    public void onPlayerQuit(Player player) {
        temporaryLanguageOverrides.remove(player.getUniqueId());
    }

    public Component getLocalizedComponent(Player player, String path) {
        String text = getLocalizedString(player, path);
        return miniMessage.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public List<Component> getLocalizedComponentList(Player player, String path) {
        List<String> strings = getLocalizedStringList(player, path);
        return strings.stream()
                .map(s -> miniMessage.deserialize(s).decoration(TextDecoration.ITALIC, false))
                .toList();
    }

    public String getLocalizedString(Player player, String path) {
        String lang = getPlayerLang(player);
        String localizedPath = path + "." + lang;

        String text = plugin.getConfig().getString(localizedPath);
        if (text == null) {
            text = plugin.getConfig().getString(path + "." + defaultLang);
        }
        if (text == null) {
            text = "Missing translation: " + path;
        }

        return text;
    }

    public List<String> getLocalizedStringList(Player player, String path) {
        String lang = getPlayerLang(player);
        String localizedPath = path + "." + lang;

        List<String> texts = plugin.getConfig().getStringList(localizedPath);
        if (texts.isEmpty()) {
            texts = plugin.getConfig().getStringList(path + "." + defaultLang);
        }
        if (texts.isEmpty()) {
            texts = List.of("Missing translation: " + path);
        }

        return texts;
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public boolean isLanguageAvailable(String lang) {
        return availableLanguages.containsKey(lang);
    }

    public java.util.Set<String> getAvailableLanguages() {
        return availableLanguages.keySet();
    }
    
    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}