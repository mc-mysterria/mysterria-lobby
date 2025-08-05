package net.mysterria.lobby.commands;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.description.Description;
import dev.rollczi.litecommands.annotations.execute.Execute;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.lobby.MysterriaLobby;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

@Command(name = "rules")
public class RulesCommand {
    
    private final MysterriaLobby plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration rulesConfig;
    
    public RulesCommand(MysterriaLobby plugin) {
        this.plugin = plugin;
        loadRulesConfig();
    }
    
    private void loadRulesConfig() {
        File rulesFile = new File(plugin.getDataFolder(), "rules.yml");
        if (!rulesFile.exists()) {
            plugin.saveResource("rules.yml", false);
        }
        rulesConfig = YamlConfiguration.loadConfiguration(rulesFile);
    }
    
    @Execute
    @Description("Display server rules")
    public void showRules(@Context Player player) {
        String playerLang = plugin.getLangManager().getPlayerLang(player);
        
        ConfigurationSection langSection = rulesConfig.getConfigurationSection("rules." + playerLang);
        if (langSection == null) {
            langSection = rulesConfig.getConfigurationSection("rules.en");
        }
        
        if (langSection == null) {
            player.sendMessage(miniMessage.deserialize("<red>❌ Rules configuration not found!</red>"));
            return;
        }
        
        String title = langSection.getString("title", "<red>Server Rules</red>");
        String subtitle = langSection.getString("subtitle", "");
        
        player.sendMessage(Component.empty());
        player.sendMessage(miniMessage.deserialize(title));
        if (!subtitle.isEmpty()) {
            player.sendMessage(miniMessage.deserialize(subtitle));
        }
        player.sendMessage(Component.empty());
        
        List<?> rulesList = langSection.getList("rules");
        if (rulesList != null) {
            for (Object ruleObj : rulesList) {
                if (ruleObj instanceof java.util.LinkedHashMap) {
                    @SuppressWarnings("unchecked")
                    java.util.LinkedHashMap<String, Object> ruleMap = (java.util.LinkedHashMap<String, Object>) ruleObj;

                    String ruleTitle = (String) ruleMap.get("title");
                    @SuppressWarnings("unchecked")
                    List<String> ruleDescription = (List<String>) ruleMap.get("description");

                    if (ruleTitle != null && !ruleTitle.isEmpty()) {
                        player.sendMessage(miniMessage.deserialize(ruleTitle));
                    }

                    if (ruleDescription != null) {
                        for (String line : ruleDescription) {
                            player.sendMessage(miniMessage.deserialize(line));
                        }
                    }

                    player.sendMessage(Component.empty());
                }
            }
        } else {
            plugin.getLogger().warning("No rules list found for language: " + playerLang);
        }
        
        List<String> footer = langSection.getStringList("footer");
        for (String line : footer) {
            player.sendMessage(miniMessage.deserialize(line));
        }
    }
    
    public void reload() {
        loadRulesConfig();
    }
}