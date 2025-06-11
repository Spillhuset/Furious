package com.spillhuset.furious;

import com.spillhuset.furious.commands.EnderseeCommand;
import com.spillhuset.furious.commands.InvseeCommand;
import com.spillhuset.furious.commands.guild.GuildCommand;
import com.spillhuset.furious.commands.teleport.TeleportCommand;
import com.spillhuset.furious.listeners.GuildListener;
import com.spillhuset.furious.listeners.TeleportListener;
import com.spillhuset.furious.listeners.WalletListener;
import com.spillhuset.furious.managers.GuildManager;
import com.spillhuset.furious.managers.TeleportManager;
import com.spillhuset.furious.managers.WalletManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class Furious extends JavaPlugin {
    private TeleportManager teleportManager;
    private WalletManager walletManager;
    private GuildManager guildManager;

    @Override
    public void onEnable() {
        // Plugin startup logic

        saveDefaultConfig();

        teleportManager = new TeleportManager(this);
        walletManager = new WalletManager(this);
        guildManager = new GuildManager(this);

        getCommand("invsee").setExecutor(new InvseeCommand(this));
        getCommand("endersee").setExecutor(new EnderseeCommand(this));
        getCommand("teleport").setExecutor(new TeleportCommand(this));
        getCommand("guild").setExecutor(new GuildCommand(this));

        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new WalletListener(this), this);
        getServer().getPluginManager().registerEvents(new GuildListener(this), this);


        getLogger().info("Furious is enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (teleportManager != null) {
            teleportManager.shutdown();
        }
        if (walletManager != null) {
            walletManager.shutdown();
        }
        if (guildManager != null) {
            guildManager.shutdown();
        }
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public WalletManager getWalletManager() {
        return walletManager;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    public ItemStack createScrapItem(double amount) {
        Material scrapMaterial = Material.getMaterial(getConfig().getString("economy.currency.material", "IRON_INGOT"));
        if (scrapMaterial == null) {
            scrapMaterial = Material.IRON_INGOT;
        }
        ItemStack scrapItem = new ItemStack(scrapMaterial, 1);
        ItemMeta scrapMeta = scrapItem.getItemMeta();
        scrapMeta.displayName(Component.text(getConfig().getString("economy.currency.symbol","⚙ ")).append(Component.text(amount, NamedTextColor.GOLD)).append(Component.text(" "+getConfig().getString("economy.currency.name", "Scrap"))));
        scrapItem.setItemMeta(scrapMeta);
        return scrapItem;
    }

}
