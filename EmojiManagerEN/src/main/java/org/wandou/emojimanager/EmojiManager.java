package org.wandou.emojimanager;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EmojiManager extends JavaPlugin {

    private EmoteManager emoteManager;
    private PlayerDataManager playerDataManager;

    // Cooldown tracking: player UUID -> last usage timestamp (millis)
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public Map<UUID, Long> getCooldowns() {
        return cooldowns;
    }

    @Override
    public void onEnable() {
        // Generate default config.yml
        saveDefaultConfig();

        // Generate emotes.yml (only if not exists, to preserve user edits)
        saveResource("emotes.yml", false);
        File emotesFile = new File(getDataFolder(), "emotes.yml");
        emoteManager = new EmoteManager(emotesFile);
        playerDataManager = new PlayerDataManager(this);

        EmoteGUI gui = new EmoteGUI(this, emoteManager, playerDataManager);
        getServer().getPluginManager().registerEvents(gui, this);

        EmoteCommand emoteCmd = new EmoteCommand(this, gui, emoteManager, playerDataManager);
        getCommand("emoji").setExecutor(emoteCmd);
        getCommand("emoji").setTabCompleter(emoteCmd);

        EmoteAdminCommand adminCmd = new EmoteAdminCommand(this, emoteManager, playerDataManager);
        getCommand("em").setExecutor(adminCmd);
        getCommand("em").setTabCompleter(adminCmd);

        getLogger().info("EmojiManager 1.1 has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("EmojiManager 1.1 has been disabled.");
    }
}