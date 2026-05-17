package org.wandou.emojimanager;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EmojiManager extends JavaPlugin {

    private EmoteManager emoteManager;
    private PlayerDataManager playerDataManager;
    private DatabaseManager databaseManager;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public Map<UUID, Long> getCooldowns() { return cooldowns; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("mysql.yml", false);

        // 初始化数据库（如果需要）
        databaseManager = new DatabaseManager(this);
        if ("mysql".equalsIgnoreCase(getConfig().getString("storage.type", "yaml"))) {
            databaseManager.connect();
        }

        saveResource("emotes.yml", false);
        File emotesFile = new File(getDataFolder(), "emotes.yml");
        emoteManager = new EmoteManager(emotesFile);
        playerDataManager = new PlayerDataManager(this, databaseManager);

        EmoteGUI gui = new EmoteGUI(this, emoteManager, playerDataManager);
        getServer().getPluginManager().registerEvents(gui, this);

        EmoteCommand emoteCmd = new EmoteCommand(this, gui, emoteManager, playerDataManager);
        getCommand("emoji").setExecutor(emoteCmd);
        getCommand("emoji").setTabCompleter(emoteCmd);

        EmoteAdminCommand adminCmd = new EmoteAdminCommand(this, emoteManager, playerDataManager);
        getCommand("em").setExecutor(adminCmd);
        getCommand("em").setTabCompleter(adminCmd);

        getLogger().info("EmojiManager 1.2 enabled! Storage: " +
                (getConfig().getString("storage.type", "yaml")));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.disconnect();
        getLogger().info("EmojiManager 1.2 disabled.");
    }
}