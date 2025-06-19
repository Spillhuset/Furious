package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.entities.Tombstone;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.managers.GuildManager;
import com.spillhuset.furious.managers.LocksManager;
import com.spillhuset.furious.managers.TombstoneManager;
import com.spillhuset.furious.managers.WalletManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Load wallet from storage if needed
        // New players start with 50S
        if (player.getFirstPlayed() == 0) {
            walletManager.init(playerId, 50.0);

            // Initialize bank account in RubberBank with 100S
            plugin.getBankManager().initPlayer(playerId);
        }

        // Display information to the player
        player.sendMessage(Component.text("=== Welcome to the Server ===", NamedTextColor.GOLD));

        // Guild information
        GuildManager guildManager = plugin.getGuildManager();
        if (guildManager.isInGuild(playerId)) {
            Guild guild = guildManager.getPlayerGuild(playerId);
            if (guild != null) {
                GuildRole role = guild.getMemberRole(playerId);
                UUID guildMasterId = guild.getOwner();
                String guildMasterName = Bukkit.getOfflinePlayer(guildMasterId).getName();
                int claimedChunks = guild.getClaimedChunkCount();
                // Assuming there's a max chunks limit per guild, if not, this can be removed or adjusted
                int maxChunks = 100; // This should be retrieved from config or a constant
                int availableChunks = maxChunks - claimedChunks;

                player.sendMessage(Component.text("Guild: ", NamedTextColor.YELLOW)
                        .append(Component.text(guild.getName(), NamedTextColor.WHITE)));
                player.sendMessage(Component.text("  Role: ", NamedTextColor.YELLOW)
                        .append(Component.text(role.toString(), NamedTextColor.WHITE)));
                player.sendMessage(Component.text("  Guild Master: ", NamedTextColor.YELLOW)
                        .append(Component.text(guildMasterName, NamedTextColor.WHITE)));
                player.sendMessage(Component.text("  Chunks: ", NamedTextColor.YELLOW)
                        .append(Component.text(claimedChunks + " claimed, " + availableChunks + " available", NamedTextColor.WHITE)));
            }
        }

        // Wallet balance
        double walletBalance = walletManager.getBalance(player);
        player.sendMessage(Component.text("Wallet: ", NamedTextColor.YELLOW)
                .append(Component.text(walletManager.formatAmount(walletBalance), NamedTextColor.WHITE)));

        // Bank balances
        BankManager bankManager = plugin.getBankManager();
        Map<String, Double> bankBalances = new HashMap<>();
        for (Map.Entry<String, Bank> entry : bankManager.getBanks().entrySet()) {
            String bankName = entry.getKey();
            if (bankManager.hasAccount(player, bankName)) {
                double balance = bankManager.getBalance(player, bankName);
                bankBalances.put(bankName, balance);
            }
        }

        if (!bankBalances.isEmpty()) {
            player.sendMessage(Component.text("Banks:", NamedTextColor.YELLOW));
            for (Map.Entry<String, Double> entry : bankBalances.entrySet()) {
                player.sendMessage(Component.text("  " + entry.getKey() + ": ", NamedTextColor.YELLOW)
                        .append(Component.text(walletManager.formatAmount(entry.getValue()), NamedTextColor.WHITE)));
            }
        }

        // Locks information
        LocksManager locksManager = plugin.getLocksManager();
        int locksUsed = locksManager.getLockedBlocksByPlayer(playerId).size();
        // Assuming there's a max locks limit per player, if not, this can be removed or adjusted
        int maxLocks = 100; // This should be retrieved from config or a constant
        int locksAvailable = maxLocks - locksUsed;

        player.sendMessage(Component.text("Locks: ", NamedTextColor.YELLOW)
                .append(Component.text(locksUsed + " used, " + locksAvailable + " available", NamedTextColor.WHITE)));

        // Tombstone information
        TombstoneManager tombstoneManager = plugin.getTombstoneManager();
        Set<Tombstone> tombstones = tombstoneManager.getPlayerTombstones(playerId);
        if (tombstones != null && !tombstones.isEmpty()) {
            player.sendMessage(Component.text("Tombstones:", NamedTextColor.YELLOW));
            for (Tombstone tombstone : tombstones) {
                // Calculate time left
                long currentTime = System.currentTimeMillis();
                long creationTime = tombstone.getCreationTime();
                // Get the timeout from TombstoneManager (default is 1800 seconds = 30 minutes)
                int timeoutSeconds = plugin.getConfig().getInt("tombstones.timeout-seconds", 1800);
                long expiryTime = creationTime + (timeoutSeconds * 1000);
                long timeLeftSeconds = (expiryTime - currentTime) / 1000;
                long timeLeftMinutes = timeLeftSeconds / 60;

                if (timeLeftMinutes > 0) {
                    player.sendMessage(Component.text("  Time left: ", NamedTextColor.YELLOW)
                            .append(Component.text(timeLeftMinutes + " minutes", NamedTextColor.WHITE)));
                }
            }
        }

        // Online players information
        int totalOnline = Bukkit.getOnlinePlayers().size();
        int guildMembersOnline = 0;

        if (guildManager.isInGuild(playerId)) {
            Guild guild = guildManager.getPlayerGuild(playerId);
            if (guild != null) {
                guildMembersOnline = guild.getOnlineMembers().size();
            }
        }

        player.sendMessage(Component.text("Online: ", NamedTextColor.YELLOW)
                .append(Component.text(totalOnline + " players, " + guildMembersOnline + " guild members", NamedTextColor.WHITE)));
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save wallet data to storage when player quits
        // This ensures that wallet data is not lost if the server crashes
        // The wallet data is also saved periodically and on server shutdown
        walletManager.saveWalletData();
    }

}
