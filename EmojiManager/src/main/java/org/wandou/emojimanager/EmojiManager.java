package org.wandou.emojimanager;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EmojiManager extends JavaPlugin {

    private EmoteManager emoteManager;
    private PlayerDataManager playerDataManager;

    // 冷却记录：玩家 UUID → 上次发送时间戳 (毫秒)
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public Map<UUID, Long> getCooldowns() {
        return cooldowns;
    }

    @Override
    public void onEnable() {
        // 生成默认 config.yml
        saveDefaultConfig();

        // 生成带注释的 emotes.yml（只第一次生成，之后不覆盖）
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

        getLogger().info("EmojiManager-1.1-ZH 已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("EmojiManager-1.1-ZH 已禁用！");
    }
}