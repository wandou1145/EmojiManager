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
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        // No arguments → open GUI
        if (args.length == 0) {
            gui.open(player);
            return true;
        }

        // Quick send: /e <emojiID or symbol>
        String input = args[0];
        EmoteManager.Emote emote = emoteManager.getEmote(input);
        if (emote == null) {
            for (EmoteManager.Emote e : emoteManager.getEmotes()) {
                if (e.getSymbol().equals(input)) {
                    emote = e;
                    break;
                }
            }
        }

        if (emote == null) {
            player.sendMessage("§cEmoji not found: " + input);
            return true;
        }

        if (!playerDataManager.getUnlockedEmotes(player).contains(emote.getId())) {
            player.sendMessage("§cYou have not unlocked this emoji!");
            return true;
        }

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