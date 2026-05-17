package org.wandou.emojimanager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final EmojiManager plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(EmojiManager plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        String host = plugin.getConfig().getString("mysql.host", "localhost");
        int port = plugin.getConfig().getInt("mysql.port", 3306);
        String database = plugin.getConfig().getString("mysql.database", "emojimanager");
        String username = plugin.getConfig().getString("mysql.username", "root");
        String password = plugin.getConfig().getString("mysql.password", "");
        boolean useSSL = plugin.getConfig().getBoolean("mysql.use-ssl", false);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(plugin.getConfig().getInt("mysql.pool.maximum-pool-size", 10));
        config.setMinimumIdle(plugin.getConfig().getInt("mysql.pool.minimum-idle", 5));
        config.setConnectionTimeout(plugin.getConfig().getInt("mysql.pool.connection-timeout", 5000));
        config.setIdleTimeout(plugin.getConfig().getInt("mysql.pool.idle-timeout", 300000));
        config.setMaxLifetime(plugin.getConfig().getInt("mysql.pool.max-lifetime", 600000));

        try {
            dataSource = new HikariDataSource(config);
            createTables();
            plugin.getLogger().info("MySQL 连接成功！所有子服将共享数据。");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("MySQL 连接失败：" + e.getMessage());
            plugin.getLogger().warning("将回退到 YAML 存储。");
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS emoji_players (" +
                            "  uuid VARCHAR(36) NOT NULL," +
                            "  player_name VARCHAR(32) NOT NULL," +
                            "  emote_order TEXT NOT NULL," +
                            "  last_updated BIGINT NOT NULL," +
                            "  PRIMARY KEY (uuid)" +
                            ")"
            );
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}