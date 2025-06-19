package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.WalletManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

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
            if (walletManager.setBalance(player, 0)) {
                // Drop physical scraps that other players can pick up
                player.getWorld().dropItem(player.getLocation(),
                        plugin.createScrapItem(droppedAmount));

                player.sendMessage(Component.text("You dropped " +
                                walletManager.formatAmount(droppedAmount) + " upon death!",
                        NamedTextColor.RED));
            } else {
                plugin.getLogger().warning("Failed to set balance to 0 for player " + player.getName() + " on death");
            }
        }
    }

    // Player join event is now handled by MessageListener

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save wallet data to storage when player quits
        // This ensures that wallet data is not lost if the server crashes
        // The wallet data is also saved periodically and on server shutdown
        walletManager.saveWalletData();
    }

}
