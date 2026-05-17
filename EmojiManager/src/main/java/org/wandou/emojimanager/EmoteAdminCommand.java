package org.wandou.emojimanager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class EmoteAdminCommand implements TabExecutor {

    private final EmojiManager plugin;
    private final EmoteManager emoteManager;
    private final PlayerDataManager playerDataManager;

    public EmoteAdminCommand(EmojiManager plugin, EmoteManager emoteManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.emoteManager = emoteManager;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("emojimanager.admin")) {
            sender.sendMessage("§cYou don't have permission!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                return handleGive(sender, args);
            case "gui":
                return handleGui(sender, args);
            case "list":
                return handleList(sender);
            case "reload":
                plugin.reloadConfig();
                emoteManager.reload();
                sender.sendMessage("§aConfiguration reloaded! All changes take effect immediately.");
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /em give <player/-all/-op/-people> <emojiID/-all> <true/false>");
            return true;
        }
        String targetArg = args[1];
        String emoteArg = args[2];
        boolean unlock = args.length >= 4 && args[3].equalsIgnoreCase("true");

        List<Player> targets = new ArrayList<>();
        if (targetArg.equalsIgnoreCase("-all")) {
            targets.addAll(Bukkit.getOnlinePlayers());
        } else if (targetArg.equalsIgnoreCase("-op")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOp()) targets.add(p);
            }
        } else if (targetArg.equalsIgnoreCase("-people")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.isOp()) targets.add(p);
            }
        } else {
            Player p = Bukkit.getPlayer(targetArg);
            if (p == null) {
                sender.sendMessage("§cPlayer not found or offline!");
                return true;
            }
            targets.add(p);
        }

        List<EmoteManager.Emote> emotesToGive = new ArrayList<>();
        if (emoteArg.equalsIgnoreCase("-all")) {
            emotesToGive.addAll(emoteManager.getAllEmotes());
        } else {
            EmoteManager.Emote emote = emoteManager.getEmote(emoteArg);
            if (emote == null) {
                sender.sendMessage("§cEmoji '" + emoteArg + "' does not exist!");
                return true;
            }
            emotesToGive.add(emote);
        }

        for (Player target : targets) {
            for (EmoteManager.Emote emote : emotesToGive) {
                if (unlock) {
                    playerDataManager.unlockEmote(target, emote.getId());
                } else {
                    playerDataManager.lockEmote(target, emote.getId());
                }
            }
        }

        sender.sendMessage("§aOperation complete! Affected players: " + targets.size() + ", emojis: " + emotesToGive.size());
        return true;
    }

    private boolean handleGui(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("edit")) {
            sender.sendMessage("§cUsage: /em gui edit <remove/add/delete> <emojiID>");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("§cPlease specify an emoji ID");
            return true;
        }
        String action = args[2].toLowerCase();
        String id = args[3];

        switch (action) {
            case "remove":
                if (emoteManager.removeEmote(id)) {
                    sender.sendMessage("§eEmoji " + id + " has been removed from the GUI (config preserved).");
                } else {
                    sender.sendMessage("§cEmoji not found.");
                }
                return true;
            case "add":
                if (emoteManager.addEmote(id)) {
                    sender.sendMessage("§aEmoji " + id + " has been re-enabled in the GUI.");
                } else {
                    sender.sendMessage("§cEmoji not found.");
                }
                return true;
            case "delete":
                if (emoteManager.deleteEmote(id)) {
                    sender.sendMessage("§cEmoji " + id + " has been permanently deleted from config.");
                } else {
                    sender.sendMessage("§cEmoji not found.");
                }
                return true;
            default:
                sender.sendMessage("§cUnknown edit action: " + action);
                return true;
        }
    }

    private boolean handleList(CommandSender sender) {
        Collection<EmoteManager.Emote> all = emoteManager.getAllEmotes();
        if (all.isEmpty()) {
            sender.sendMessage("§eNo emojis configured.");
            return true;
        }
        sender.sendMessage("§6===== Emoji List =====");
        for (EmoteManager.Emote e : all) {
            String status = e.isEnabled() ? "§aEnabled" : "§cDisabled";
            sender.sendMessage(" §7- " + e.getId() + " (" + e.getName() + ") " + status + " §7Price: " + e.getPrice());
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== EmojiManager Admin Commands =====");
        sender.sendMessage("§e/em give <player/-all/-op/-people> <emojiID/-all> <true/false> §7- Batch unlock/lock");
        sender.sendMessage("§e/em gui edit remove <emojiID> §7- Remove from GUI");
        sender.sendMessage("§e/em gui edit add <emojiID> §7- Re-add to GUI");
        sender.sendMessage("§e/em gui edit delete <emojiID> §7- Permanently delete");
        sender.sendMessage("§e/em list §7- List all emojis");
        sender.sendMessage("§e/em reload §7- Reload all configuration files");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("emojimanager.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return filter(Arrays.asList("give", "gui", "list", "reload"), args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                List<String> options = new ArrayList<>();
                options.add("-all");
                options.add("-op");
                options.add("-people");
                for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
                return filter(options, args[1]);
            } else if (args[0].equalsIgnoreCase("gui")) {
                return filter(Collections.singletonList("edit"), args[1]);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                List<String> options = new ArrayList<>();
                options.add("-all");
                options.addAll(getAllEmoteIds());
                return filter(options, args[2]);
            } else if (args[0].equalsIgnoreCase("gui") && args[1].equalsIgnoreCase("edit")) {
                return filter(Arrays.asList("remove", "add", "delete"), args[2]);
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("give")) {
                return filter(Arrays.asList("true", "false"), args[3]);
            } else if (args[0].equalsIgnoreCase("gui") && args[1].equalsIgnoreCase("edit")) {
                return filter(getAllEmoteIds(), args[3]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> getAllEmoteIds() {
        return emoteManager.getAllEmotes().stream()
                .map(EmoteManager.Emote::getId)
                .collect(Collectors.toList());
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}