package io.Sriptirc_wp_1.islandshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;
import java.util.*;

public class ShopManager implements Listener {
    
    private final Islandshop plugin;
    private final EconomyManager economyManager;
    private final FileConfiguration config;
    
    private final Map<UUID, Integer> playerQuantities = new HashMap<>();
    private final Map<UUID, Material> selectedItems = new HashMap<>();
    private final Map<String, Double> itemPrices = new HashMap<>();
    private final Map<Integer, Material> slotItems = new HashMap<>();
    
    private final int[] itemSlots = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    
    private final int QUANTITY_ONE_SLOT = 46;
    private final int QUANTITY_SIXTEEN_SLOT = 47;
    private final int QUANTITY_SIXTYFOUR_SLOT = 48;
    private final int BALANCE_SLOT = 50;
    private final int CLOSE_SLOT = 52;
    
    public ShopManager(Islandshop plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.config = plugin.getPluginConfig();
        
        // 注册事件监听器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // 加载商品价格
        loadItemPrices();
        
        // 初始化槽位物品映射
        initializeSlotItems();
    }
    
    private void loadItemPrices() {
        itemPrices.clear();
        
        if (config.contains("items")) {
            Set<String> keys = config.getConfigurationSection("items").getKeys(false);
            for (String key : keys) {
                double price = config.getDouble("items." + key);
                itemPrices.put(key.toUpperCase(), price);
            }
        }
        
        plugin.getLogger().info("已加载 " + itemPrices.size() + " 种商品价格");
    }
    
    private void initializeSlotItems() {
        slotItems.clear();
        
        List<String> itemList = new ArrayList<>(itemPrices.keySet());
        
        for (int i = 0; i < itemSlots.length && i < itemList.size(); i++) {
            String itemName = itemList.get(i);
            Material material = Material.getMaterial(itemName);
            if (material != null) {
                slotItems.put(itemSlots[i], material);
            } else {
                plugin.getLogger().warning("无法识别的物品ID: " + itemName + "，请检查配置");
            }
        }
    }
    
    public void reloadConfig() {
        loadItemPrices();
        initializeSlotItems();
    }
    
    public void openShop(Player player) {
        // 设置默认购买数量为1
        playerQuantities.put(player.getUniqueId(), 1);
        selectedItems.remove(player.getUniqueId());
        
        // 创建商店界面
        String title = ChatColor.translateAlternateColorCodes('&', 
            config.getString("gui.title", "&6&l空岛方块商店"));
        Inventory shop = Bukkit.createInventory(null, 54, title);
        
        // 设置边框
        setBorder(shop);
        
        // 设置商品
        setItems(shop);
        
        // 设置功能按钮
        setFunctionButtons(shop, player);
        
        // 打开界面
        player.openInventory(shop);
    }
    
    private void setBorder(Inventory inventory) {
        String borderItemName = config.getString("gui.border-item", "BLACK_STAINED_GLASS_PANE");
        Material borderMaterial = Material.getMaterial(borderItemName);
        if (borderMaterial == null) {
            plugin.getLogger().warning("边框物品ID无效: " + borderItemName + "，使用默认值");
            borderMaterial = Material.BLACK_STAINED_GLASS_PANE;
        }
        
        String borderName = ChatColor.translateAlternateColorCodes('&',
            config.getString("gui.border-name", "&8边框"));
        
        ItemStack borderItem = new ItemStack(borderMaterial);
        ItemMeta meta = borderItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(borderName);
            borderItem.setItemMeta(meta);
        }
        
        // 设置顶部和底部边框
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderItem);
            inventory.setItem(i + 45, borderItem);
        }
        
        // 设置两侧边框
        for (int i = 1; i <= 5; i++) {
            inventory.setItem(i * 9, borderItem);
            inventory.setItem(i * 9 + 8, borderItem);
        }
    }
    
    private void setItems(Inventory inventory) {
        for (Map.Entry<Integer, Material> entry : slotItems.entrySet()) {
            int slot = entry.getKey();
            Material material = entry.getValue();
            String materialName = material.name();
            
            Double price = itemPrices.get(materialName);
            if (price == null) continue;
            
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // 设置显示名称
                String displayName = getItemDisplayName(material);
                meta.setDisplayName(ChatColor.GREEN + displayName);
                
                // 设置Lore
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "价格: " + ChatColor.YELLOW + economyManager.format(price));
                lore.add(ChatColor.GRAY + "物品ID: " + ChatColor.WHITE + materialName);
                lore.add("");
                lore.add(ChatColor.YELLOW + "点击购买");
                
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            
            inventory.setItem(slot, item);
        }
    }
    
    private String getItemDisplayName(Material material) {
        // 这里可以添加更友好的显示名称映射
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
    
    private void setFunctionButtons(Inventory inventory, Player player) {
        // 购买数量选择按钮
        setQuantityButton(inventory, QUANTITY_ONE_SLOT, "one", 1, player);
        setQuantityButton(inventory, QUANTITY_SIXTEEN_SLOT, "sixteen", 16, player);
        setQuantityButton(inventory, QUANTITY_SIXTYFOUR_SLOT, "sixtyfour", 64, player);
        
        // 余额显示按钮
        setBalanceButton(inventory, player);
        
        // 关闭按钮
        setCloseButton(inventory);
    }
    
    private void setQuantityButton(Inventory inventory, int slot, String configKey, int quantity, Player player) {
        Material material = Material.getMaterial(
            config.getString("gui.quantity." + configKey + ".item", "PAPER"));
        if (material == null) {
            material = Material.PAPER;
        }
        
        String name = ChatColor.translateAlternateColorCodes('&',
            config.getString("gui.quantity." + configKey + ".name", "&a购买" + quantity + "个"));
        
        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("gui.quantity." + configKey + ".lore")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            
            // 如果当前选择的是这个数量，添加选中标记
            Integer currentQuantity = playerQuantities.get(player.getUniqueId());
            if (currentQuantity != null && currentQuantity == quantity) {
                List<String> newLore = new ArrayList<>(lore);
                newLore.add("");
                newLore.add(ChatColor.GREEN + "✓ 当前选择");
                meta.setLore(newLore);
            }
            
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
    }
    
    private void setBalanceButton(Inventory inventory, Player player) {
        Material material = Material.getMaterial(
            config.getString("gui.balance-item", "GOLD_INGOT"));
        if (material == null) {
            material = Material.GOLD_INGOT;
        }
        
        double balance = economyManager.getBalance(player);
        String formattedBalance = economyManager.format(balance);
        
        String name = ChatColor.translateAlternateColorCodes('&',
            config.getString("gui.balance-name", "&e我的余额: %balance%")
                .replace("%balance%", formattedBalance));
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "点击查看详细余额");
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        inventory.setItem(BALANCE_SLOT, item);
    }
    
    private void setCloseButton(Inventory inventory) {
        Material material = Material.getMaterial(
            config.getString("gui.close-item", "BARRIER"));
        if (material == null) {
            material = Material.BARRIER;
        }
        
        String name = ChatColor.translateAlternateColorCodes('&',
            config.getString("gui.close-name", "&c关闭商店"));
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        
        inventory.setItem(CLOSE_SLOT, item);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // 检查是否是商店界面
        String title = ChatColor.translateAlternateColorCodes('&', 
            config.getString("gui.title", "&6&l空岛方块商店"));
        if (!inventory.getView().getTitle().equals(title)) return;
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;
        
        // 处理功能按钮点击
        if (slot == QUANTITY_ONE_SLOT) {
            playerQuantities.put(player.getUniqueId(), 1);
            updateShop(player);
            return;
        }
        
        if (slot == QUANTITY_SIXTEEN_SLOT) {
            playerQuantities.put(player.getUniqueId(), 16);
            updateShop(player);
            return;
        }
        
        if (slot == QUANTITY_SIXTYFOUR_SLOT) {
            playerQuantities.put(player.getUniqueId(), 64);
            updateShop(player);
            return;
        }
        
        if (slot == BALANCE_SLOT) {
            economyManager.sendBalanceMessage(player);
            return;
        }
        
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            player.sendMessage(plugin.getMessage("shop-closed"));
            return;
        }
        
        // 处理商品点击
        Material material = slotItems.get(slot);
        if (material != null) {
            handleItemPurchase(player, material, slot);
        }
    }
    
    private void updateShop(Player player) {
        if (player.getOpenInventory() != null) {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            String title = ChatColor.translateAlternateColorCodes('&', 
                config.getString("gui.title", "&6&l空岛方块商店"));
            
            if (inventory.getView().getTitle().equals(title)) {
                // 重新设置功能按钮以更新选中状态
                setFunctionButtons(inventory, player);
            }
        }
    }
    
    private void handleItemPurchase(Player player, Material material, int slot) {
        String materialName = material.name();
        Double price = itemPrices.get(materialName);
        
        if (price == null) {
            player.sendMessage(plugin.getMessage("item-not-found"));
            return;
        }
        
        Integer quantity = playerQuantities.get(player.getUniqueId());
        if (quantity == null) {
            quantity = 1;
        }
        
        double totalPrice = price * quantity;
        
        // 检查背包空间
        if (!hasInventorySpace(player, material, quantity)) {
            player.sendMessage(plugin.getMessage("purchase-fail"));
            return;
        }
        
        // 检查确认购买设置
        boolean confirmPurchase = config.getBoolean("settings.confirm-purchase", true);
        if (confirmPurchase && !material.equals(selectedItems.get(player.getUniqueId()))) {
            // 第一次点击，选择商品
            selectedItems.put(player.getUniqueId(), material);
            
            String displayName = getItemDisplayName(material);
            String formattedPrice = economyManager.format(totalPrice);
            
            player.sendMessage(plugin.getMessage("confirm-message",
                "amount", String.valueOf(quantity),
                "item", displayName,
                "price", formattedPrice));
            player.sendMessage(ChatColor.YELLOW + "再次点击确认购买，或点击其他商品取消选择");
            return;
        }
        
        // 确认购买或直接购买
        if (economyManager.checkAndWithdraw(player, totalPrice, getItemDisplayName(material), quantity)) {
            // 给予物品
            ItemStack item = new ItemStack(material, quantity);
            player.getInventory().addItem(item);
            
            // 记录购买日志
            logPurchase(player, materialName, quantity, totalPrice);
            
            // 清除选择
            selectedItems.remove(player.getUniqueId());
        }
    }
    
    private boolean hasInventorySpace(Player player, Material material, int quantity) {
        ItemStack item = new ItemStack(material, quantity);
        return player.getInventory().addItem(item).isEmpty();
    }
    
    private void logPurchase(Player player, String itemName, int quantity, double price) {
        boolean logPurchases = config.getBoolean("settings.log-purchases", true);
        if (!logPurchases) return;
        
        String logFormat = config.getString("settings.log-format", 
            "[%time%] %player% 购买了 %amount% 个 %item% 花费 %price%");
        
        String logMessage = logFormat
            .replace("%time%", new Date().toString())
            .replace("%player%", player.getName())
            .replace("%amount%", String.valueOf(quantity))
            .replace("%item%", itemName)
            .replace("%price%", economyManager.format(price));
        
        plugin.getLogger().info(logMessage);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        playerQuantities.remove(player.getUniqueId());
        selectedItems.remove(player.getUniqueId());
    }
    
    public void cleanup() {
        playerQuantities.clear();
        selectedItems.clear();
    }
}