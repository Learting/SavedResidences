package cc.globalserver.SavedResidences;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class Main extends JavaPlugin implements Listener {
    
    private final Map<UUID, List<String>> residences = new HashMap<>();
    private final Map<UUID, Boolean> addingResidence = new HashMap<>();

    private File dataFolder;
    private FileConfiguration config;

    private LanguageProperty messages;

    private String inventoryName;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        dataFolder = new File(getDataFolder(), "residences");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        messages = new LanguageProperty();
        File langFile = new File(getDataFolder(), "messages.yml");
        if (!langFile.exists()) {
            saveResource("messages.yml", false);
        }
        messages.loadConfiguration(langFile);

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        inventoryName = ChatColor.translateAlternateColorCodes('&', messages.get("gui.title"));

        for (Player player : Bukkit.getOnlinePlayers()) {
            loadResidences(player);
        }
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            saveResidences(player);
        }
        residences.clear();
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        if ("/srgui".equals(command)) {
            event.setCancelled(true);
            openInventory(event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(inventoryName)) {
            return;
        }
        if (e.getClick().isKeyboardClick()) {
            if (e.getClickedInventory() == e.getView().getTopInventory()) {
                e.setCancelled(true);
                return;
            }
        }
        e.setCancelled(true);

        ItemStack currentItem = e.getCurrentItem();
        if (currentItem == null || currentItem.getType() == Material.AIR) {
            return; // Ignore empty slots
        }

        if (e.getCurrentItem().getType().equals(Material.WHITE_STAINED_GLASS_PANE)) {
            String resLoc = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            Player player = (Player) e.getWhoClicked();
            if (e.isLeftClick()) {
                // Execute res tp
                player.performCommand("res tp " + resLoc);
                player.closeInventory();
            } else if (e.isRightClick()) {
                residences.get(player.getUniqueId()).remove(resLoc);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.get("residence.deleted").replace("{name}", resLoc)));
                openInventory(player);
            }
        } else if (e.getCurrentItem().getType().equals(Material.LIME_STAINED_GLASS_PANE)) {
            addingResidence.put(e.getWhoClicked().getUniqueId(), true);
            Player player = (Player) e.getWhoClicked();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.get("residence.prompt")));
            e.getWhoClicked().closeInventory();
        } else if (e.getCurrentItem().getType().equals(Material.ORANGE_STAINED_GLASS_PANE)) {
            Player player = (Player) e.getWhoClicked();
            String command = config.getString("backCommand");
            player.performCommand(command);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (addingResidence.remove(uuid) != null) {
            event.setCancelled(true);

            List<String> playerResidences = residences.get(uuid);
            if (playerResidences == null) {
                playerResidences = new ArrayList<>();
                residences.put(uuid, playerResidences);
            }
            residences.get(uuid).add(event.getMessage());
            saveResidences(event.getPlayer());
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', messages.get("residence.saved").replace("{name}", event.getMessage())));
        }
    }

    private void openInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, inventoryName);

        List<String> residenceList = residences.get(player.getUniqueId());

        // If null then init
        if (residenceList != null) {
            for (int i = 0; i < residenceList.size(); i++) {
                inv.setItem(i, createItem(residenceList.get(i)));
            }
        }

        if (config.getBoolean("backButton")) {
            inv.setItem(inv.getSize() - 9, createBackButton());
        }
        inv.setItem(inv.getSize() - 1, createAddItem());
        player.openInventory(inv);
    }

    private void loadResidences(Player player) {
        File playerFile = new File(dataFolder, player.getUniqueId().toString() + ".yml");
        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            residences.put(player.getUniqueId(), new ArrayList<>(config.getStringList("residences")));
        } else {
            residences.put(player.getUniqueId(), new ArrayList<>());
        }
    }

    private void saveResidences(Player player) {
        File playerFile = new File(dataFolder, player.getUniqueId().toString() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("residences", new ArrayList<>(residences.get(player.getUniqueId())));
        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ItemStack createBackButton() {
        ItemStack itemStack = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', messages.get("gui.back")));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private ItemStack createItem(String resName) {
        ItemStack itemStack = new ItemStack(Material.WHITE_STAINED_GLASS_PANE, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(resName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', messages.get("gui.left")));
        lore.add(ChatColor.translateAlternateColorCodes('&', messages.get("gui.right")));
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private ItemStack createAddItem() {
        ItemStack itemStack = new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', messages.get("gui.add")));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
