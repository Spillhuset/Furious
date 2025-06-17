package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.GuildSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Subcommand for listing all claimed chunks of a guild.
 */
public class ClaimsSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new ClaimsSubCommand.
     *
     * @param plugin The plugin instance
     */
    public ClaimsSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "claims";
    }

    @Override
    public String getDescription() {
        return "Lists all chunks claimed by your guild.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild claims", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Lists all chunks claimed by your guild.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkGuildPermission(sender)) {
            return true;
        }

        if (args.length != 1) {
            getUsage(sender);
            return true;
        }

        Player player = (Player) sender;

        // Get the guild
        Guild guild = isInGuild(player);

        // Get claimed chunks
        Set<String> claimedChunks = guild.getClaimedChunks();

        if (claimedChunks.isEmpty()) {
            player.sendMessage(Component.text("Your guild has not claimed any chunks yet!", NamedTextColor.YELLOW));
            return true;
        }

        // Group chunks by world
        Map<UUID, List<String>> chunksByWorld = new HashMap<>();

        for (String chunkStr : claimedChunks) {
            String[] parts = chunkStr.split(":");
            UUID worldId = UUID.fromString(parts[0]);
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            chunksByWorld.computeIfAbsent(worldId, k -> new ArrayList<>())
                    .add("(" + x + ", " + z + ")");
        }

        // Display claimed chunks
        player.sendMessage(Component.text("=== " + guild.getName() + "'s Claimed Chunks (" + claimedChunks.size() + "/" +
                plugin.getConfig().getInt("guilds.max-plots-per-guild", 16) + ") ===", NamedTextColor.GOLD));

        for (Map.Entry<UUID, List<String>> entry : chunksByWorld.entrySet()) {
            UUID worldId = entry.getKey();
            List<String> chunks = entry.getValue();

            World world = Bukkit.getWorld(worldId);
            String worldName = world != null ? world.getName() : "Unknown World";

            player.sendMessage(Component.text("World: " + worldName + " (" + chunks.size() + " chunks)", NamedTextColor.YELLOW));

            StringBuilder chunksStr = new StringBuilder();
            for (int i = 0; i < chunks.size(); i++) {
                chunksStr.append(chunks.get(i));
                if (i < chunks.size() - 1) {
                    chunksStr.append(", ");
                }

                // Split into multiple lines if too long
                if (chunksStr.length() > 50 && i < chunks.size() - 1) {
                    player.sendMessage(Component.text("  " + chunksStr.toString(), NamedTextColor.WHITE));
                    chunksStr = new StringBuilder();
                }
            }

            if (chunksStr.length() > 0) {
                player.sendMessage(Component.text("  " + chunksStr.toString(), NamedTextColor.WHITE));
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completion for listing claims
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.claims";
    }

    @Override
    public GuildRole getRequiredRole() {
        // This command requires the player to be in a guild but doesn't require a specific role
        return GuildRole.USER;
    }

    @Override
    public boolean checkGuildPermission(@NotNull CommandSender sender, boolean feedback) {
        // First check regular permissions
        if (!checkPermission(sender, feedback)) {
            return false;
        }

        // If not a player, they can't view guild claims
        if (!(sender instanceof Player player)) {
            if (feedback) {
                sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            }
            return false;
        }

        // Check if player is in a guild
        Guild guild = isInGuild(player);
        if (guild == null) {
            if (feedback) {
                sender.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
            }
            return false;
        }

        return true;
    }
}
