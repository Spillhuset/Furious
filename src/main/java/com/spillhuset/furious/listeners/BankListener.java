package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for bank-related events.
 */
public class BankListener implements Listener {
    private final Furious plugin;
    private final Map<UUID, String> lastPlayerBank = new HashMap<>();

    /**
     * Creates a new BankListener.
     *
     * @param plugin The plugin instance
     */
    public BankListener(Furious plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player movement between chunks to show bank title-screens.
     * When a player moves between chunks owned by different banks, a title-screen
     * is shown indicating which bank they are entering.
     *
     * @param event The player move event
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Get the player
        Player player = event.getPlayer();

        // Get the from and to locations
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();

        // Check if the player has moved to a different chunk
        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()) {
            // Player is still in the same chunk, no need to check bank
            return;
        }

        // Get the guild that owns the chunk the player is moving to
        Guild toGuild = plugin.getGuildManager().getChunkOwner(toChunk);

        // Only process if the chunk is in a SAFE zone
        if (toGuild == null || toGuild.getType() != GuildType.SAFE) {
            // Not in a SAFE zone, clear last bank
            lastPlayerBank.remove(player.getUniqueId());
            return;
        }

        // Get the bank that owns the chunk the player is moving to
        Bank toBank = plugin.getBankManager().getBankByChunk(toChunk);

        // Get the player's UUID
        UUID playerUUID = player.getUniqueId();

        // Get the name of the last bank the player was in
        String lastBankName = lastPlayerBank.get(playerUUID);

        // Get the name of the bank the player is moving to
        String toBankName = toBank != null ? toBank.getName() : null;

        // Check if the player has moved to a different bank's territory
        if (toBankName != null && (lastBankName == null || !lastBankName.equals(toBankName))) {
            // Show title-screen with SAFE as title and bank name as subtitle
            Title title = Title.title(
                Component.text(toGuild.getName()),
                Component.text("Bank: " + toBankName),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))
            );
            player.showTitle(title);

            // Update the last bank the player was in
            lastPlayerBank.put(playerUUID, toBankName);
        } else if (toBankName == null && lastBankName != null) {
            // Player has moved out of any bank's territory but still in SAFE zone
            Title title = Title.title(
                Component.text(toGuild.getName()),
                Component.text(toGuild.getDescription()),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))
            );
            player.showTitle(title);

            // Remove from last bank tracking
            lastPlayerBank.remove(playerUUID);
        }
    }
}