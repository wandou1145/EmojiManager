package org.wandou.emojimanager;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EmoteManager {

    private final File file;
    private final Map<String, Emote> emotes = new LinkedHashMap<>();

    public EmoteManager(File file) {
        this.file = file;
        reload();
    }

    public void reload() {
        emotes.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains("emotes")) {
            for (String key : config.getConfigurationSection("emotes").getKeys(false)) {
                String name = config.getString("emotes." + key + ".name");
                String texture = config.getString("emotes." + key + ".texture");
                int price = config.getInt("emotes." + key + ".price", 0);
                boolean enabled = config.getBoolean("emotes." + key + ".enabled", true);
                String symbol = config.getString("emotes." + key + ".symbol", name);
                String sound = config.getString("emotes." + key + ".sound", "");
                String particle = config.getString("emotes." + key + ".particle", "");
                emotes.put(key, new Emote(key, name, symbol, texture, price, enabled, sound, particle));
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Emote emote : emotes.values()) {
            String path = "emotes." + emote.getId();
            config.set(path + ".name", emote.getName());
            config.set(path + ".symbol", emote.getSymbol());
            config.set(path + ".texture", emote.getTexture());
            config.set(path + ".price", emote.getPrice());
            config.set(path + ".enabled", emote.isEnabled());
            config.set(path + ".sound", emote.getSound());
            config.set(path + ".particle", emote.getParticle());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Collection<Emote> getEmotes() {
        return emotes.values().stream()
                .filter(Emote::isEnabled)
                .collect(Collectors.toList());
    }

    public Collection<Emote> getAllEmotes() {
        return Collections.unmodifiableCollection(emotes.values());
    }

    public Emote getEmote(String id) {
        return emotes.get(id);
    }

    public boolean removeEmote(String id) {
        Emote emote = emotes.get(id);
        if (emote != null) {
            emote.setEnabled(false);
            save();
            return true;
        }
        return false;
    }

    public boolean addEmote(String id) {
        Emote emote = emotes.get(id);
        if (emote != null) {
            emote.setEnabled(true);
            save();
            return true;
        }
        return false;
    }

    public boolean deleteEmote(String id) {
        if (emotes.remove(id) != null) {
            save();
            return true;
        }
        return false;
    }

    public static class Emote {
        private final String id;
        private final String name;
        private final String symbol;
        private final String texture;
        private final int price;
        private boolean enabled;
        private String sound;
        private String particle;

        public Emote(String id, String name, String symbol, String texture, int price, boolean enabled,
                     String sound, String particle) {
            this.id = id;
            this.name = name;
            this.symbol = symbol;
            this.texture = texture;
            this.price = price;
            this.enabled = enabled;
            this.sound = sound;
            this.particle = particle;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getSymbol() { return symbol; }
        public String getTexture() { return texture; }
        public int getPrice() { return price; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSound() { return sound; }
        public String getParticle() { return particle; }
        public void setSound(String sound) { this.sound = sound; }
        public void setParticle(String particle) { this.particle = particle; }
    }
}