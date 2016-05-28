package com.inkzzz.headmanager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Luke Denham on 28/05/2016.
 */
public final class HeadManagerPlugin extends JavaPlugin implements Listener
{

    private Economy economy = null;
    private double percentage = 0.05;
    private String sellMessage = "You have sold {0}'s head for ${1}.";
    private String worthlessMessage = "You have discarded {0}'s head due to it being worthless.";
    private String holdHeadMessage = "You must hold a player skull.";

    @Override
    public final void onEnable()
    {
        this.getConfig().addDefault("Percentage", this.percentage);
        this.getConfig().addDefault("sellMessage", this.sellMessage);
        this.getConfig().addDefault("worthlessMessage", this.worthlessMessage);
        this.getConfig().addDefault("holdHeadMessage", this.holdHeadMessage);
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        this.percentage = this.getConfig().getDouble("Percentage");
        this.sellMessage = this.getConfig().getString("sellMessage");
        this.worthlessMessage = this.getConfig().getString("worthlessMessage");
        this.holdHeadMessage = this.getConfig().getString("holdHeadMessage");
        if(!setupEconomy())
        {
            this.getServer().getPluginManager().disablePlugin(this);
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("sellhead"))
        {
            if(sender instanceof Player)
            {
                final Player player = (Player) sender;
                final ItemStack item = player.getItemInHand();
                if(item == null || item.getType() != Material.SKULL_ITEM || !item.hasItemMeta())
                {
                    player.sendMessage(toColor(this.holdHeadMessage));
                }
                else
                {
                    SkullMeta meta = (SkullMeta) item.getItemMeta();
                    if(meta.getOwner() != null)
                    {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(meta.getOwner());
                        int amount = calculate(offlinePlayer);
                        if(amount == -1)
                        {
                            player.sendMessage(toColor(this.worthlessMessage.replace("{0}", offlinePlayer.getName())));
                        }
                        else
                        {
                            this.economy.withdrawPlayer(offlinePlayer, amount);
                            this.economy.depositPlayer(player, amount);
                            player.sendMessage(toColor(this.sellMessage.replace("{0}", offlinePlayer.getName()).replace("{1}", String.valueOf(amount))));
                        }
                        player.getInventory().removeItem(buildHead(offlinePlayer.getName()));
                    }
                }
            }
        }
        return false;
    }

    @EventHandler
    public final void onEvent(final PlayerDeathEvent event)
    {
        final Player player = event.getEntity();
        if(player.getKiller() == null)
        {
            return;
        }
        player.getWorld().dropItemNaturally(player.getLocation(), buildHead(player.getName()));
    }

    private ItemStack buildHead(String name)
    {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(name);
        meta.setDisplayName(ChatColor.GREEN + name + "'s " + ChatColor.GRAY + "head");
        item.setItemMeta(meta);
        return item;
    }

    private String toColor(String x)
    {
        return ChatColor.translateAlternateColorCodes('&', x);
    }

    private int calculate(OfflinePlayer offlinePlayer)
    {
        double value = this.economy.getBalance(offlinePlayer) * this.percentage;
        return (int) (value > 100 ? value : -1);
    }

    private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            this.economy = economyProvider.getProvider();
        }
        return (this.economy != null);
    }

}
