package cc.globalserver.SavedResidences;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private File configFile;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        getCommand("srgui").setExecutor(this);
        getCommand("srcheck").setExecutor(this);
        getCommand("sradd").setExecutor(this);

        configFile = new File(getDataFolder(), "messages.yml");
        if (!configFile.exists()) {
            saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("srgui")) {
            openGui(player, false);
            return true;
        } else if (command.getName().equalsIgnoreCase("srcheck")) {
            if (player.hasPermission("srgui.check")) {
                if(args.length == 1) {
                    Player target = Bukkit.getPlayerExact(args[0]);
                    if(target != null) {
                        openGui(player, true, target);
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.targetNotFound")));
                    }
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.usageSrcheck")));
                }
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.noPermission")));
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("sradd")) {
            if (args.length == 1) {
                addSavedName(player, args[0]);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.added")));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.usageSradd")));
            }
            return true;
        }

        return false;
    }

    private void openGui(Player player, boolean isAdmin, Player... targetPlayers) {
        Player target = (targetPlayers.length == 1) ? targetPlayers[0] : player;
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', config.getString("messages.guiTitle")));

        List<String> savedNames = getSavedNames(target);
        for (int i = 0; i < savedNames.size(); i++) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + savedNames.get(i));
            item.setItemMeta(meta);

            inv.setItem(i, item);
        }

        if (!isAdmin) {
            ItemStack addButton = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
            ItemMeta addButtonMeta = addButton.getItemMeta();
            addButtonMeta.setDisplayName(ChatColor.GREEN + config.getString("messages.addButton"));
            addButton.setItemMeta(addButtonMeta);

            ItemStack deleteButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta deleteButtonMeta = deleteButton.getItemMeta();
            deleteButtonMeta.setDisplayName(ChatColor.RED + config.getString("messages.deleteButton"));
            deleteButton.setItemMeta(deleteButtonMeta);

            inv.setItem(53, addButton);
            inv.setItem(52, deleteButton);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', config.getString("messages.guiTitle"))) &&
                !event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', config.getString("messages.deleteModeTitle")))) {
            return;
        }

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        boolean isDeleteMode = event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', config.getString("messages.deleteModeTitle")));

        if (item == null || item.getType() == Material.AIR) return;

        if (item.getType() == Material.PAPER && !isDeleteMode) {
            player.performCommand("res tp " + ChatColor.stripColor(item.getItemMeta().getDisplayName()));
            player.closeInventory();
        } else if (item.getType() == Material.PAPER && isDeleteMode) {
            deleteSavedName(player, ChatColor.stripColor(item.getItemMeta().getDisplayName()));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.deleted")));
            openGui(player, true);
        } else if (item.getType() == Material.GREEN_STAINED_GLASS_PANE) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.enterName")));
            player.closeInventory();
        } else if (item.getType() == Material.RED_STAINED_GLASS_PANE) {
            if (isDeleteMode) {
                openGui(player, false);
            } else {
                Inventory deleteModeInv = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', config.getString("messages.deleteModeTitle")));
                deleteModeInv.setContents(event.getClickedInventory().getContents());

                ItemStack goBackButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta goBackButtonMeta = goBackButton.getItemMeta();
                goBackButtonMeta.setDisplayName(ChatColor.RED + config.getString("messages.goBackButton"));
                goBackButton.setItemMeta(goBackButtonMeta);

                deleteModeInv.setItem(52, goBackButton);
                player.openInventory(deleteModeInv);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', config.getString("messages.deleteModeTitle")))) {
            openGui((Player)event.getPlayer(), false);
        }
    }

    private List<String> getSavedNames(Player player) {
        return getPlayerData(player).getStringList("names");
    }

    private void addSavedName(Player player, String name) {
        FileConfiguration playerData = getPlayerData(player);
        List<String> savedNames = playerData.getStringList("names");
        savedNames.add(name);
        playerData.set("names", savedNames);
        savePlayerData(player, playerData);
    }

    private void deleteSavedName(Player player, String name) {
        FileConfiguration playerData = getPlayerData(player);
        List<String> savedNames = playerData.getStringList("names");
        savedNames.remove(name);
        playerData.set("names", savedNames);
        savePlayerData(player, playerData);
    }

    private FileConfiguration getPlayerData(Player player) {
        File playerDataFile = new File(getDataFolder() + File.separator + "playerdata", player.getUniqueId().toString() + ".yml");
        return YamlConfiguration.loadConfiguration(playerDataFile);
    }

    private void savePlayerData(Player player, FileConfiguration playerData) {
        File playerDataFile = new File(getDataFolder() + File.separator + "playerdata", player.getUniqueId().toString() + ".yml");
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
