package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.WalletManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class WalletListener implements Listener {
    private final Furious plugin;
    private final WalletManager walletManager;

    public WalletListener(Furious plugin) {
        this.plugin = plugin;
        this.walletManager = plugin.getWalletManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        double droppedAmount = walletManager.getBalance(player);
        if (droppedAmount > 0) {
            walletManager.setBalance(player, 0);
            // Optionally drop physical scraps that other players can pick up
            player.getWorld().dropItem(player.getLocation(),
                    plugin.createScrapItem(droppedAmount));

            player.sendMessage(Component.text("You dropped " +
                            walletManager.formatAmount(droppedAmount) + " upon death!",
                    NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load wallet from storage if needed
        // For now, new players start with 0
        if (event.getPlayer().getFirstPlayed() == 0) {
            walletManager.init(event.getPlayer().getUniqueId());
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Optionally save wallet to storage
        // Wallet stays in memory for now
    }

}
