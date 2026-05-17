package org.wandou.emojimanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class PlayerDataManager {
    private final EmojiManager plugin;
    private final DatabaseManager dbManager;
    private final boolean useMySQL;

    // YAML 回退
    private final File yamlFile;
    private YamlConfiguration yamlConfig;

    public PlayerDataManager(EmojiManager plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.useMySQL = "mysql".equalsIgnoreCase(plugin.getConfig().getString("storage.type", "yaml"))
                && dbManager != null && dbManager.isConnected();

        this.yamlFile = new File(plugin.getDataFolder(), "players.yml");
        if (!useMySQL) {
            loadYaml();
        }
    }

    private void loadYaml() {
        if (!yamlFile.exists()) {
            try { yamlFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
    }

    private void saveYaml() {
        try { yamlConfig.save(yamlFile); } catch (IOException e) { e.printStackTrace(); }
    }

    // 获取解锁顺序列表
    public List<String> getUnlockedOrder(Player player) {
        if (useMySQL) {
            return loadOrderFromMySQL(player);
        }
        List<String> list = yamlConfig.getStringList(player.getUniqueId() + ".order");
        return new ArrayList<>(list != null ? list : Collections.emptyList());
    }

    // 获取解锁集合（用于快速检查）
    public Set<String> getUnlockedEmotes(Player player) {
        return new HashSet<>(getUnlockedOrder(player));
    }

    // 解锁表情
    public void unlockEmote(Player player, String emoteId) {
        List<String> order = getUnlockedOrder(player);
        if (!order.contains(emoteId)) {
            order.add(emoteId);
            saveOrder(player, order);
        }
    }

    // 上锁表情
    public void lockEmote(Player player, String emoteId) {
        List<String> order = getUnlockedOrder(player);
        if (order.remove(emoteId)) {
            saveOrder(player, order);
        }
    }

    // 交换两个表情位置
    public void swapOrder(Player player, int i1, int i2) {
        List<String> order = getUnlockedOrder(player);
        if (i1 >= 0 && i1 < order.size() && i2 >= 0 && i2 < order.size()) {
            Collections.swap(order, i1, i2);
            saveOrder(player, order);
        }
    }

    // 保存顺序（根据模式选择存储方式）
    private void saveOrder(Player player, List<String> order) {
        if (useMySQL) {
            saveOrderToMySQL(player, order);
        } else {
            yamlConfig.set(player.getUniqueId() + ".order", order);
            saveYaml();
        }
    }

    // ========== MySQL 操作 ==========
    private List<String> loadOrderFromMySQL(Player player) {
        String sql = "SELECT emote_order FROM emoji_players WHERE uuid = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String raw = rs.getString("emote_order");
                if (raw != null && !raw.isEmpty()) {
                    return new ArrayList<>(Arrays.asList(raw.split(",")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL 读取失败: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private void saveOrderToMySQL(Player player, List<String> order) {
        String orderStr = String.join(",", order);
        String sql = "INSERT INTO emoji_players (uuid, player_name, emote_order, last_updated) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), " +
                "emote_order = VALUES(emote_order), last_updated = VALUES(last_updated)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, player.getName());
            ps.setString(3, orderStr);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL 写入失败: " + e.getMessage());
        }
    }
}