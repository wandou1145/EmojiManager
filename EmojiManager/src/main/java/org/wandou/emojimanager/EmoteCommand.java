package org.wandou.emojimanager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class EmoteCommand implements TabExecutor {

    private final EmojiManager plugin;
    private final EmoteGUI gui;
    private final EmoteManager emoteManager;
    private final PlayerDataManager playerDataManager;

    public EmoteCommand(EmojiManager plugin, EmoteGUI gui, EmoteManager emoteManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.gui = gui;
        this.emoteManager = emoteManager;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行！");
            return true;
        }

        // 无参数 → 打开 GUI
        if (args.length == 0) {
            gui.open(player);
            return true;
        }

        // 有参数 → 快捷发送表情
        String input = args[0];
        EmoteManager.Emote emote = emoteManager.getEmote(input);
        if (emote == null) {
            // 尝试通过 symbol 查找
            for (EmoteManager.Emote e : emoteManager.getEmotes()) {
                if (e.getSymbol().equals(input)) {
                    emote = e;
                    break;
                }
            }
        }

        if (emote == null) {
            player.sendMessage("§c未找到表情: " + input);
            return true;
        }

        if (!playerDataManager.getUnlockedEmotes(player).contains(emote.getId())) {
            player.sendMessage("§c你尚未解锁该表情！");
            return true;
        }

        // 发送表情（特效和聊天同步在 EmoteDisplayer 中处理）
        EmoteDisplayer.display(plugin, player, emote);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            for (EmoteManager.Emote e : emoteManager.getEmotes()) {
                options.add(e.getId());
                options.add(e.getSymbol());
            }
            return filter(options, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}