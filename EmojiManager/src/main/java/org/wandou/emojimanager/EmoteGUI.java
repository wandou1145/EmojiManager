package org.wandou.emojimanager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.net.URL;
import java.util.*;

public class EmoteGUI implements Listener {

    private final EmojiManager plugin;
    private final EmoteManager emoteManager;
    private final PlayerDataManager playerDataManager;
    private Economy economy;

    private final Map<UUID, Integer> mainPage = new HashMap<>();
    private final Map<UUID, Integer> editPage = new HashMap<>();
    private final Map<UUID, Integer> lockedPage = new HashMap<>();
    private final Map<UUID, String> editSelected = new HashMap<>();

    private static final int GUI_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 35; // 5 rows x 7 slots

    public EmoteGUI(EmojiManager plugin, EmoteManager emoteManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.emoteManager = emoteManager;
        this.playerDataManager = playerDataManager;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }
    }

    public void open(Player player) {
        openMain(player);
    }

    // ========== Main GUI ==========
    private void openMain(Player player) {
        mainPage.put(player.getUniqueId(), 0);
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, "§8Emoji Manager §7(Unlocked)");
        drawMain(inv, player, 0);
        player.openInventory(inv);
    }

    private void drawMain(Inventory inv, Player player, int page) {
        inv.clear();
        List<String> unlockedOrder = playerDataManager.getUnlockedOrder(player);
        List<String> unlockedList = new ArrayList<>(unlockedOrder);

        fillBorder(inv);
        inv.setItem(4, createItem(Material.BARRIER, "§cExit", null));
        inv.setItem(8, createItem(Material.CHEST, "§eLocked Emojis", "§7Click to view locked emojis"));
        inv.setItem(45, createItem(Material.ARROW, "§aPrevious Page", null));
        inv.setItem(49, createItem(Material.ITEM_FRAME, "§6Edit Order", "§7Drag to rearrange unlocked emojis"));
        inv.setItem(53, createItem(Material.ARROW, "§aNext Page", null));

        int startIndex = page * ITEMS_PER_PAGE;
        int slot = 10; // start at 2nd row, 2nd column
        for (int i = startIndex; i < unlockedList.size() && slot < 44; i++) {
            EmoteManager.Emote emote = emoteManager.getEmote(unlockedList.get(i));
            if (emote != null && emote.isEnabled()) {
                inv.setItem(slot, createEmoteHead(emote));
            }
            slot++;
            if ((slot + 1) % 9 == 0 || slot % 9 == 0) slot += 2; // skip last and first column
        }
    }

    // ========== Edit Order GUI ==========
    private void openEdit(Player player) {
        editPage.put(player.getUniqueId(), 0);
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, "§8Edit Emoji Order");
        drawEdit(inv, player, 0);
        player.openInventory(inv);
    }

    private void drawEdit(Inventory inv, Player player, int page) {
        inv.clear();
        List<String> unlockedOrder = playerDataManager.getUnlockedOrder(player);
        List<String> list = new ArrayList<>(unlockedOrder);

        fillBorder(inv);
        inv.setItem(4, createItem(Material.BARRIER, "§cBack", "§7Return to main menu"));
        inv.setItem(45, createItem(Material.ARROW, "§aPrevious Page", null));
        inv.setItem(53, createItem(Material.ARROW, "§aNext Page", null));

        int startIndex = page * ITEMS_PER_PAGE;
        int slot = 10;
        for (int i = startIndex; i < list.size() && slot < 44; i++) {
            EmoteManager.Emote emote = emoteManager.getEmote(list.get(i));
            if (emote != null) {
                inv.setItem(slot, createEmoteHead(emote));
            }
            slot++;
            if ((slot + 1) % 9 == 0 || slot % 9 == 0) slot += 2;
        }
    }

    // ========== Locked Emojis GUI ==========
    private void openLocked(Player player) {
        lockedPage.put(player.getUniqueId(), 0);
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, "§8Locked Emojis");
        drawLocked(inv, player, 0);
        player.openInventory(inv);
    }

    private void drawLocked(Inventory inv, Player player, int page) {
        inv.clear();
        List<EmoteManager.Emote> all = new ArrayList<>(emoteManager.getEmotes());
        Set<String> unlocked = playerDataManager.getUnlockedEmotes(player);
        List<EmoteManager.Emote> lockedList = new ArrayList<>();
        for (EmoteManager.Emote e : all) {
            if (!unlocked.contains(e.getId())) lockedList.add(e);
        }

        fillBorder(inv);
        inv.setItem(4, createItem(Material.BARRIER, "§cBack", "§7Return to main menu"));
        inv.setItem(45, createItem(Material.ARROW, "§aPrevious Page", null));
        inv.setItem(53, createItem(Material.ARROW, "§aNext Page", null));

        int startIndex = page * ITEMS_PER_PAGE;
        int slot = 10;
        for (int i = startIndex; i < lockedList.size() && slot < 44; i++) {
            EmoteManager.Emote emote = lockedList.get(i);
            inv.setItem(slot, createLockedItem(emote));
            slot++;
            if ((slot + 1) % 9 == 0 || slot % 9 == 0) slot += 2;
        }
    }

    // ========== Click Handlers ==========
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title.startsWith("§8Emoji Manager")) {
            event.setCancelled(true);
            handleMainClick(event, player);
        } else if (title.startsWith("§8Edit Emoji Order")) {
            event.setCancelled(true);
            handleEditClick(event, player);
        } else if (title.startsWith("§8Locked Emojis")) {
            event.setCancelled(true);
            handleLockedClick(event, player);
        }
    }

    private void handleMainClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot == 4) { player.closeInventory(); }
        else if (slot == 8) { openLocked(player); }
        else if (slot == 45) { changeMainPage(player, -1); }
        else if (slot == 49) { openEdit(player); }
        else if (slot == 53) { changeMainPage(player, 1); }
        else if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
            EmoteManager.Emote emote = getEmoteFromHead(event.getCurrentItem());
            if (emote != null) {
                player.closeInventory();
                EmoteDisplayer.display(plugin, player, emote);
            }
        }
    }

    private void handleEditClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot == 4) { openMain(player); }
        else if (slot == 45) { changeEditPage(player, -1); }
        else if (slot == 53) { changeEditPage(player, 1); }
        else if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
            String emoteId = getEmoteIdFromHead(event.getCurrentItem());
            if (emoteId == null) return;
            if (editSelected.containsKey(player.getUniqueId())) {
                String firstId = editSelected.remove(player.getUniqueId());
                List<String> order = playerDataManager.getUnlockedOrder(player);
                int idx1 = order.indexOf(firstId);
                int idx2 = order.indexOf(emoteId);
                if (idx1 != -1 && idx2 != -1) {
                    playerDataManager.swapOrder(player, idx1, idx2);
                }
                drawEdit(event.getInventory(), player, editPage.getOrDefault(player.getUniqueId(), 0));
            } else {
                editSelected.put(player.getUniqueId(), emoteId);
                player.sendMessage("§eSelected. Click another emoji to swap positions.");
            }
        }
    }

    private void handleLockedClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot == 4) { openMain(player); }
        else if (slot == 45) { changeLockedPage(player, -1); }
        else if (slot == 53) { changeLockedPage(player, 1); }
        else if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.GRAY_DYE) {
            buyEmote(player, event.getCurrentItem());
        }
    }

    private void buyEmote(Player player, ItemStack item) {
        if (economy == null) {
            player.sendMessage("§cEconomy system is not available.");
            return;
        }
        String rawName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        String emoteName = rawName.replace(" (Locked)", "");
        EmoteManager.Emote target = null;
        for (EmoteManager.Emote e : emoteManager.getEmotes()) {
            if (e.getName().equals(emoteName)) { target = e; break; }
        }
        if (target == null) return;
        if (economy.getBalance(player) >= target.getPrice()) {
            economy.withdrawPlayer(player, target.getPrice());
            playerDataManager.unlockEmote(player, target.getId());
            player.sendMessage("§aPurchase successful! You can now use the " + target.getName() + " emoji.");
            openLocked(player);
        } else {
            player.sendMessage("§cNot enough coins! You need " + target.getPrice() + " coins.");
        }
    }

    // ========== Helper Methods ==========
    private ItemStack createEmoteHead(EmoteManager.Emote emote) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setDisplayName("§e" + emote.getName());
        meta.setLore(List.of("§aUnlocked", "§7Click to display overhead"));
        if (emote.getTexture() != null) {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            try {
                String hash = extractHashFromBase64(emote.getTexture());
                if (hash != null && !hash.isEmpty()) {
                    textures.setSkin(new URL("http://textures.minecraft.net/texture/" + hash));
                }
            } catch (Exception e) { /* ignore */ }
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        }
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createLockedItem(EmoteManager.Emote emote) {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§8" + emote.getName() + " (Locked)");
        meta.setLore(List.of("§7Price: §6" + emote.getPrice() + " coins", "§eClick to buy!"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(List.of(lore.split("\n")));
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorder(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        for (int i = 0; i < GUI_SIZE; i++) {
            if (i / 9 == 0 || i / 9 == 5 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, glass);
            }
        }
    }

    private String extractHashFromBase64(String base64) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            int start = decoded.indexOf("\"url\":\"") + 7;
            int end = decoded.indexOf("\"", start);
            String url = decoded.substring(start, end);
            return url.substring(url.lastIndexOf("/") + 1);
        } catch (Exception e) {
            return "";
        }
    }

    private EmoteManager.Emote getEmoteFromHead(ItemStack head) {
        if (head == null || head.getType() != Material.PLAYER_HEAD) return null;
        String name = ChatColor.stripColor(head.getItemMeta().getDisplayName());
        for (EmoteManager.Emote e : emoteManager.getEmotes()) {
            if (e.getName().equals(name)) return e;
        }
        return null;
    }

    private String getEmoteIdFromHead(ItemStack head) {
        EmoteManager.Emote e = getEmoteFromHead(head);
        return e != null ? e.getId() : null;
    }

    private void changeMainPage(Player player, int delta) {
        int page = mainPage.getOrDefault(player.getUniqueId(), 0) + delta;
        int maxPage = Math.max(0, (playerDataManager.getUnlockedOrder(player).size() - 1) / ITEMS_PER_PAGE);
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;
        mainPage.put(player.getUniqueId(), page);
        drawMain(player.getOpenInventory().getTopInventory(), player, page);
    }

    private void changeEditPage(Player player, int delta) {
        int page = editPage.getOrDefault(player.getUniqueId(), 0) + delta;
        int maxPage = Math.max(0, (playerDataManager.getUnlockedOrder(player).size() - 1) / ITEMS_PER_PAGE);
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;
        editPage.put(player.getUniqueId(), page);
        drawEdit(player.getOpenInventory().getTopInventory(), player, page);
    }

    private void changeLockedPage(Player player, int delta) {
        int page = lockedPage.getOrDefault(player.getUniqueId(), 0) + delta;
        List<EmoteManager.Emote> all = new ArrayList<>(emoteManager.getEmotes());
        Set<String> unlocked = playerDataManager.getUnlockedEmotes(player);
        int count = (int) all.stream().filter(e -> !unlocked.contains(e.getId())).count();
        int maxPage = Math.max(0, (count - 1) / ITEMS_PER_PAGE);
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;
        lockedPage.put(player.getUniqueId(), page);
        drawLocked(player.getOpenInventory().getTopInventory(), player, page);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        editSelected.remove(event.getPlayer().getUniqueId());
    }
}