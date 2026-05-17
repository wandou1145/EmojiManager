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
            sender.sendMessage("§c你没有权限！");
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
                sender.sendMessage("§a配置已重载！所有参数即时生效。");
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /em give <玩家名/-all/-op/-people> <表情ID/-all> <true/false>");
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
                sender.sendMessage("§c玩家不存在或不在线！");
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
                sender.sendMessage("§c表情包 '" + emoteArg + "' 不存在！");
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

        sender.sendMessage("§a操作完成！影响玩家数: " + targets.size() + "，表情数: " + emotesToGive.size());
        return true;
    }

    private boolean handleGui(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("edit")) {
            sender.sendMessage("§c用法: /em gui edit <remove/add/delete> <表情ID>");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("§c请指定表情ID");
            return true;
        }
        String action = args[2].toLowerCase();
        String id = args[3];

        switch (action) {
            case "remove":
                if (emoteManager.removeEmote(id)) {
                    sender.sendMessage("§e表情 " + id + " 已从GUI中移除（配置仍保留）");
                } else {
                    sender.sendMessage("§c未找到该表情");
                }
                return true;
            case "add":
                if (emoteManager.addEmote(id)) {
                    sender.sendMessage("§a表情 " + id + " 已重新启用在GUI中");
                } else {
                    sender.sendMessage("§c未找到该表情");
                }
                return true;
            case "delete":
                if (emoteManager.deleteEmote(id)) {
                    sender.sendMessage("§c表情 " + id + " 已从配置文件中永久删除");
                } else {
                    sender.sendMessage("§c未找到该表情");
                }
                return true;
            default:
                sender.sendMessage("§c未知编辑操作: " + action);
                return true;
        }
    }

    private boolean handleList(CommandSender sender) {
        Collection<EmoteManager.Emote> all = emoteManager.getAllEmotes();
        if (all.isEmpty()) {
            sender.sendMessage("§e没有配置任何表情。");
            return true;
        }
        sender.sendMessage("§6===== 表情列表 =====");
        for (EmoteManager.Emote e : all) {
            String status = e.isEnabled() ? "§a启用" : "§c禁用";
            sender.sendMessage(" §7- " + e.getId() + " (" + e.getName() + ") " + status + " §7价格: " + e.getPrice());
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== EmojiManager 管理员命令 =====");
        sender.sendMessage("§e/em give <玩家/-all/-op/-people> <表情ID/-all> <true/false> §7- 批量解锁/上锁");
        sender.sendMessage("§e/em gui edit remove <表情ID> §7- 从GUI移除表情");
        sender.sendMessage("§e/em gui edit add <表情ID> §7- 重新添加表情到GUI");
        sender.sendMessage("§e/em gui edit delete <表情ID> §7- 永久删除表情");
        sender.sendMessage("§e/em list §7- 查看所有表情");
        sender.sendMessage("§e/em reload §7- 重载所有配置文件");
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