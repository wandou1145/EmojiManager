package org.wandou.emojimanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerDataManager {
    private final EmojiManager plugin;
    private final File file;
    private YamlConfiguration config;

    public PlayerDataManager(EmojiManager plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("无法创建 players.yml");
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存玩家数据！");
        }
    }

    /** 获取已解锁表情的顺序列表（用于 GUI 显示） */
    public List<String> getUnlockedOrder(Player player) {
        List<String> list = config.getStringList(player.getUniqueId() + ".order");
        if (list == null) list = new ArrayList<>();
        return new ArrayList<>(list);
    }

    /** 覆盖整个顺序列表 */
    public void setUnlockedOrder(Player player, List<String> order) {
        config.set(player.getUniqueId() + ".order", order);
        save();
    }

    /** 解锁表情（自动追加到顺序末尾） */
    public void unlockEmote(Player player, String emoteId) {
        List<String> order = getUnlockedOrder(player);
        if (!order.contains(emoteId)) {
            order.add(emoteId);
            setUnlockedOrder(player, order);
        }
    }

    /** 上锁表情（从顺序中移除） */
    public void lockEmote(Player player, String emoteId) {
        List<String> order = getUnlockedOrder(player);
        if (order.remove(emoteId)) {
            setUnlockedOrder(player, order);
        }
    }

    /** 检查是否已解锁（保留兼容性） */
    public Set<String> getUnlockedEmotes(Player player) {
        return new HashSet<>(getUnlockedOrder(player));
    }

    /** 交换顺序中两个位置的表情 */
    public void swapOrder(Player player, int index1, int index2) {
        List<String> order = getUnlockedOrder(player);
        if (index1 >= 0 && index1 < order.size() && index2 >= 0 && index2 < order.size()) {
            Collections.swap(order, index1, index2);
            setUnlockedOrder(player, order);
        }
    }
}