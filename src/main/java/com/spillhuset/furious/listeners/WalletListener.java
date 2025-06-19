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

public class WalletListener implements Listener {
    private final Furious plugin;
    private final WalletManager walletManager;

    public WalletListener(Furious plugin) {
        this.plugin = plugin;
        this.walletManager = plugin.getWalletManager();
    }

    // Player death is now handled by TombstoneManager
    // This ensures scraps appear in the tombstone instead of being dropped in the world

    // Player join event is now handled by MessageListener

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save wallet data to storage when player quits
        // This ensures that wallet data is not lost if the server crashes
        // The wallet data is also saved periodically and on server shutdown
        walletManager.saveWalletData();
    }

}
