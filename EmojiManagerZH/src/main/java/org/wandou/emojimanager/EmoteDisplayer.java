package org.wandou.emojimanager;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class EmoteDisplayer {

    public static void display(EmojiManager plugin, Player player, EmoteManager.Emote emote) {
        // ---- 冷却检查 ----
        int cooldownSeconds = plugin.getConfig().getInt("cooldown.seconds", 0);
        if (cooldownSeconds > 0) {
            long now = System.currentTimeMillis();
            long last = plugin.getCooldowns().getOrDefault(player.getUniqueId(), 0L);
            long cooldownMillis = cooldownSeconds * 1000L;
            long remaining = (last + cooldownMillis) - now;
            if (remaining > 0) {
                String msg = ChatColor.translateAlternateColorCodes('&',
                                plugin.getConfig().getString("cooldown.message", "&c请等待 %seconds% 秒后再发送表情。"))
                        .replace("%seconds%", String.valueOf((int) Math.ceil(remaining / 1000.0)));
                player.sendMessage(msg);
                return;
            }
            plugin.getCooldowns().put(player.getUniqueId(), now);
        }
        // ---- 冷却检查结束 ----

        float scale = (float) plugin.getConfig().getDouble("display.scale", 2.0);
        double offsetX = plugin.getConfig().getDouble("display.offset-x", 0.0);
        double offsetY = plugin.getConfig().getDouble("display.offset-y", 2.2);
        double offsetZ = plugin.getConfig().getDouble("display.offset-z", 0.0);
        boolean effectsEnabled = plugin.getConfig().getBoolean("effects.enabled", true);

        Location loc = player.getEyeLocation().add(offsetX, offsetY, offsetZ);

        TextDisplay display = player.getWorld().spawn(loc, TextDisplay.class, d -> {
            d.setText(emote.getSymbol());
            d.setBillboard(Display.Billboard.CENTER);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setSeeThrough(true);
            d.setShadowRadius(1.0f);
            d.setShadowStrength(1.0f);
        });

        if (scale != 1.0f) {
            Transformation transform = new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()
            );
            display.setTransformation(transform);
        }

        if (effectsEnabled) {
            playEffects(plugin, player, emote);
        }

        syncChat(plugin, player, emote);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!display.isValid() || !player.isOnline()) {
                display.remove();
                return;
            }
            Location newLoc = player.getEyeLocation().add(offsetX, offsetY, offsetZ);
            display.teleport(newLoc);
        }, 0L, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            task.cancel();
            if (display.isValid()) display.remove();
        }, 60L);
    }

    private static void playEffects(EmojiManager plugin, Player player, EmoteManager.Emote emote) {
        String soundName = emote.getSound();
        if (soundName == null || soundName.isEmpty()) {
            soundName = plugin.getConfig().getString("effects.default-sound", "");
        }
        String particleName = emote.getParticle();
        if (particleName == null || particleName.isEmpty()) {
            particleName = plugin.getConfig().getString("effects.default-particle", "");
        }

        if (!soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效音效: " + soundName);
            }
        }

        if (!particleName.isEmpty()) {
            try {
                Particle particle = Particle.valueOf(particleName);
                int count = plugin.getConfig().getInt("effects.particle-count", 5);
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 2, 0), count, 0.3, 0.3, 0.3, 0);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效粒子: " + particleName);
            }
        }
    }

    private static void syncChat(EmojiManager plugin, Player player, EmoteManager.Emote emote) {
        boolean enabled = plugin.getConfig().getBoolean("chat-sync.enabled", true);
        if (!enabled) return;

        String template = plugin.getConfig().getString("chat-sync.message", "&e%player% &r使用了表情 %symbol%");
        String msg = template
                .replace("%player%", player.getName())
                .replace("%symbol%", emote.getSymbol());
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }
}