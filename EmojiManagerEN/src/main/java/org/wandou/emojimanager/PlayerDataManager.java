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
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public List<String> getUnlockedOrder(Player player) {
        List<String> list = config.getStringList(player.getUniqueId() + ".order");
        return new ArrayList<>(list != null ? list : Collections.emptyList());
    }

    public void setUnlockedOrder(Player player, List<String> order) {
        config.set(player.getUniqueId() + ".order", order);
        save();
    }

    public void unlockEmote(Player player, String emoteId) {
        List<String> order = getUnlockedOrder(player);
        if (!order.contains(emoteId)) {
            order.add(emoteId);
            setUnlockedOrder(player, order);
        }
    }

    public void lockEmote(Player player, String emoteId) {
        List<String> order = getUnlockedOrder(player);
        if (order.remove(emoteId)) {
            setUnlockedOrder(player, order);
        }
    }

    public Set<String> getUnlockedEmotes(Player player) {
        return new HashSet<>(getUnlockedOrder(player));
    }

    public void swapOrder(Player player, int i1, int i2) {
        List<String> order = getUnlockedOrder(player);
        if (i1 >= 0 && i1 < order.size() && i2 >= 0 && i2 < order.size()) {
            Collections.swap(order, i1, i2);
            setUnlockedOrder(player, order);
        }
    }
}