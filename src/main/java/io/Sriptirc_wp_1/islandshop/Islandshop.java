package io.Sriptirc_wp_1.islandshop;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import java.util.logging.Logger;

public class Islandshop extends JavaPlugin {
    
    private static Islandshop instance;
    private FileConfiguration config;
    private Logger logger;
    private EconomyManager economyManager;
    private ShopManager shopManager;
    
    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        // 保存默认配置文件
        saveDefaultConfig();
        config = getConfig();
        
        // 初始化经济管理器
        economyManager = new EconomyManager(this);
        
        // 初始化商店管理器
        shopManager = new ShopManager(this, economyManager);
        
        // 注册命令执行器和补全器
        getCommand("islandshop").setExecutor(this);
        getCommand("islandshop").setTabCompleter(new TabCompleter(this));
        getCommand("is").setExecutor(this);
        getCommand("is").setTabCompleter(new TabCompleter(this));
        
        logger.info("空岛方块商店插件已启用！");
        logger.info("作者: ScriptIrc Engine");
        logger.info("版本: " + getDescription().getVersion());
    }
    
    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.cleanup();
        }
        
        logger.info("空岛方块商店插件已禁用！");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // 检查使用权限
        if (!player.hasPermission("islandshop.use")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        // 处理重载命令
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("islandshop.admin")) {
                player.sendMessage(getMessage("no-permission"));
                return true;
            }
            
            reloadConfig();
            config = getConfig();
            shopManager.reloadConfig();
            
            player.sendMessage(getMessage("reload-success"));
            return true;
        }
        
        // 打开商店界面
        shopManager.openShop(player);
        player.sendMessage(getMessage("shop-opened"));
        
        return true;
    }
    
    public static Islandshop getInstance() {
        return instance;
    }
    
    public FileConfiguration getPluginConfig() {
        return config;
    }
    
    public String getMessage(String key) {
        String message = config.getString("messages." + key, "");
        if (message.isEmpty()) {
            return ChatColor.RED + "消息配置缺失: " + key;
        }
        
        String prefix = config.getString("messages.prefix", "&8[&6空岛商店&8] &7");
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }
    
    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
            }
        }
        
        return message;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public ShopManager getShopManager() {
        return shopManager;
    }
}
