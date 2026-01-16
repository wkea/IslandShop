package io.Sriptirc_wp_1.islandshop;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;

public class EconomyManager {
    
    private final Islandshop plugin;
    private Economy economy;
    private boolean vaultEnabled;
    
    public EconomyManager(Islandshop plugin) {
        this.plugin = plugin;
        setupEconomy();
    }
    
    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("未找到Vault插件！经济系统将无法使用。");
            vaultEnabled = false;
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("未找到经济服务提供者！请确保已安装经济插件（如EssentialsX）。");
            vaultEnabled = false;
            return;
        }
        
        economy = rsp.getProvider();
        vaultEnabled = true;
        plugin.getLogger().info("已成功连接到Vault经济系统！");
    }
    
    public boolean isVaultEnabled() {
        return vaultEnabled;
    }
    
    public double getBalance(Player player) {
        if (!vaultEnabled || economy == null) {
            return 0.0;
        }
        
        return economy.getBalance(player);
    }
    
    public boolean has(Player player, double amount) {
        if (!vaultEnabled || economy == null) {
            return false;
        }
        
        return economy.has(player, amount);
    }
    
    public boolean withdraw(Player player, double amount) {
        if (!vaultEnabled || economy == null) {
            return false;
        }
        
        if (!has(player, amount)) {
            return false;
        }
        
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    public boolean deposit(Player player, double amount) {
        if (!vaultEnabled || economy == null) {
            return false;
        }
        
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    
    public String format(double amount) {
        if (!vaultEnabled || economy == null) {
            return String.format("%.2f", amount);
        }
        
        return economy.format(amount);
    }
    
    public void sendBalanceMessage(Player player) {
        if (!vaultEnabled) {
            player.sendMessage(plugin.getMessage("vault-not-found"));
            return;
        }
        
        double balance = getBalance(player);
        String formattedBalance = format(balance);
        player.sendMessage(plugin.getMessage("balance-check", "balance", formattedBalance));
    }
    
    public boolean checkAndWithdraw(Player player, double price, String itemName, int amount) {
        if (!vaultEnabled) {
            player.sendMessage(plugin.getMessage("vault-not-found"));
            return false;
        }
        
        if (!has(player, price)) {
            String formattedPrice = format(price);
            String formattedBalance = format(getBalance(player));
            player.sendMessage(plugin.getMessage("insufficient-funds", 
                "price", formattedPrice, 
                "balance", formattedBalance));
            return false;
        }
        
        if (withdraw(player, price)) {
            String formattedPrice = format(price);
            player.sendMessage(plugin.getMessage("purchase-success",
                "amount", String.valueOf(amount),
                "item", itemName,
                "price", formattedPrice));
            return true;
        }
        
        return false;
    }
}