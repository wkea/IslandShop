package io.Sriptirc_wp_1.islandshop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {
    
    private final Islandshop plugin;
    
    public TabCompleter(Islandshop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 只对玩家显示补全
        if (!(sender instanceof Player)) {
            return completions;
        }
        
        Player player = (Player) sender;
        
        // 检查权限
        if (!player.hasPermission("islandshop.use")) {
            return completions;
        }
        
        // 第一个参数
        if (args.length == 1) {
            // 如果有管理员权限，显示reload
            if (player.hasPermission("islandshop.admin")) {
                completions.add("reload");
            }
            
            // 过滤匹配的补全
            String input = args[0].toLowerCase();
            List<String> filtered = new ArrayList<>();
            for (String completion : completions) {
                if (completion.toLowerCase().startsWith(input)) {
                    filtered.add(completion);
                }
            }
            return filtered;
        }
        
        return completions;
    }
}