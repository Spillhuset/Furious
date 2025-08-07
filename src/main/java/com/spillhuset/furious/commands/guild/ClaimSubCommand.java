package com.spillhuset.furious.commands.guild;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.GuildSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for claiming a chunk for a guild.
 */
public class ClaimSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new ClaimSubCommand.
     *
     * @param plugin The plugin instance
     */
    public ClaimSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getDescription() {
        return "Claims the current chunk for your guild or an unmanned guild (op only).";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild claim", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Claims the chunk you are standing in for your guild.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("You must be the guild owner to claim chunks.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Your guild can claim up to " + plugin.getConfig().getInt("guilds.max-plots-per-guild", 16) + " chunks.", NamedTextColor.YELLOW));

        if (sender.isOp() || sender.hasPermission("furious.guild.claim.unmanned")) {
            sender.sendMessage(Component.text("/guild claim <guild>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Claims the chunk you are standing in for the specified unmanned guild (SAFE, WAR, WILD).", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/guild claim WILDLIFE", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Unclaims the chunk you are standing in from the WILD guild.", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/guild claim SAFE", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Claims all chunks between your WorldEdit selection points for the SAFE guild.", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("This command is only available to server operators.", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if this is a claim for an unmanned guild
        if (args.length == 2) {
            // Only ops can claim for unmanned guilds
            if (!player.isOp() && !player.hasPermission("furious.guild.claim.unmanned")) {
                player.sendMessage(Component.text("You don't have permission to claim chunks for unmanned guilds!", NamedTextColor.RED));
                return true;
            }

            String guildName = args[1].toUpperCase();
            Guild unmannedGuild;

            // Get the appropriate unmanned guild
            switch (guildName) {
                case "SAFE" -> {
                    unmannedGuild = plugin.getGuildManager().getSafeGuild();

                    // Check if this is a WorldEdit selection claim
                    WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
                    if (worldEdit != null) {
                        try {
                            // Get the WorldEdit selection
                            LocalSession session = worldEdit.getSession(player);
                            Region region = session.getSelection(BukkitAdapter.adapt(player.getWorld()));

                            if (region != null) {
                                // Get the world
                                World world = player.getWorld();

                                // Convert WorldEdit region to Bukkit locations
                                org.bukkit.Location minLocation = BukkitAdapter.adapt(world, region.getMinimumPoint());
                                org.bukkit.Location maxLocation = BukkitAdapter.adapt(world, region.getMaximumPoint());

                                // Get the chunks at the min and max points
                                Chunk minChunk = minLocation.getChunk();
                                Chunk maxChunk = maxLocation.getChunk();

                                // Claim all chunks in the selection
                                claimChunksInArea(unmannedGuild, minChunk, maxChunk, player);
                                return true;
                            }
                        } catch (IncompleteRegionException e) {
                            player.sendMessage(Component.text("You need to make a WorldEdit selection first!", NamedTextColor.RED));
                            return true;
                        } catch (Exception e) {
                            player.sendMessage(Component.text("Error accessing WorldEdit selection: " + e.getMessage(), NamedTextColor.RED));
                            return true;
                        }
                    }

                    // If we get here, either WorldEdit is not available or there was no selection
                    // Fall back to claiming the current chunk
                    plugin.getGuildManager().adminClaimChunk(unmannedGuild, player.getLocation().getChunk(), player);
                }
                case "WAR" -> unmannedGuild = plugin.getGuildManager().getWarGuild();
                case "WILD", "WILDLIFE" -> {
                    // For WILDLIFE, unclaim the chunk instead of claiming it
                    if (guildName.equals("WILDLIFE")) {
                        Guild wildGuild = plugin.getGuildManager().getWildGuild();
                        plugin.getGuildManager().unclaimChunk(wildGuild, player.getLocation().getChunk(), player);
                        return true;
                    } else {
                        unmannedGuild = plugin.getGuildManager().getWildGuild();
                    }
                }
                default -> {
                    player.sendMessage(Component.text("Unknown unmanned guild: " + guildName, NamedTextColor.RED));
                    player.sendMessage(Component.text("Valid unmanned guilds are: SAFE, WAR, WILD, WILDLIFE", NamedTextColor.RED));
                    return true;
                }
            }

            // For WAR and WILD, or if the SAFE WorldEdit selection failed, claim the current chunk
            if (guildName.equals("WAR") || guildName.equals("WILD")) {
                plugin.getGuildManager().adminClaimChunk(unmannedGuild, player.getLocation().getChunk(), player);
            }
            return true;
        } else if (args.length != 1) {
            getUsage(sender);
            return true;
        }

        // Regular claim for player's guild
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

        // Check if player is in a guild
        if (guild == null) {
            player.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
            return true;
        }

        // Get the chunk the player is standing in
        plugin.getGuildManager().claimChunk(guild, player.getLocation().getChunk(), player);

        return true;
    }

    /**
     * Claims all chunks in the rectangular area defined by the two corner chunks.
     *
     * @param guild The guild to claim the chunks for
     * @param corner1 The first corner chunk
     * @param corner2 The second corner chunk
     * @param player The player performing the claim
     */
    private void claimChunksInArea(Guild guild, Chunk corner1, Chunk corner2, Player player) {
        // Get the min and max chunk coordinates
        int minX = Math.min(corner1.getX(), corner2.getX());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        World world = corner1.getWorld();
        int totalChunks = (maxX - minX + 1) * (maxZ - minZ + 1);
        int claimedChunks = 0;

        // Inform the player about the operation
        player.sendMessage(Component.text("Attempting to claim " + totalChunks + " chunks for " + guild.getName() + "...", NamedTextColor.YELLOW));

        // Claim each chunk in the area
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Chunk chunk = world.getChunkAt(x, z);
                plugin.getGuildManager().adminClaimChunk(guild, chunk, player);
                claimedChunks++;
            }
        }

        // Inform the player about the result
        player.sendMessage(Component.text("Operation complete. " + claimedChunks + " chunks processed.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Provide unmanned guild completions for ops and admins
        if ((sender.isOp() || sender.hasPermission("furious.guild.claim.unmanned") || sender.hasPermission("furious.guild.admin")) && args.length == 2) {
            String partial = args[1].toUpperCase();
            List<String> unmannedGuilds = List.of("SAFE", "WAR", "WILD", "WILDLIFE");

            // Filter based on what the player has typed so far
            for (String guild : unmannedGuilds) {
                if (guild.startsWith(partial)) {
                    completions.add(guild);
                }
            }

            // Add a hint for WorldEdit selection with SAFE
            if ("SAFE".startsWith(partial) && sender instanceof Player) {
                WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
                if (worldEdit != null) {
                    try {
                        Player player = (Player) sender;
                        LocalSession session = worldEdit.getSession(player);
                        session.getSelection(BukkitAdapter.adapt(player.getWorld()));
                        // If we get here, there is a valid selection
                        completions.add("SAFE (WorldEdit selection)");
                    } catch (IncompleteRegionException e) {
                        // No valid selection, don't add the hint
                    }
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.claim";
    }

    @Override
    public GuildRole getRequiredRole() {
        return GuildRole.ADMIN;
    }

    @Override
    public Guild isInGuild(@NotNull Player player) {
        return plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
    }

    @Override
    public Guild isInGuild(@NotNull UUID playerUUID) {
        return plugin.getGuildManager().getPlayerGuild(playerUUID);
    }

    @Override
    public boolean isGuildOwner(@NotNull Player player) {
        Guild guild = isInGuild(player);
        if (guild == null) {
            return false;
        }
        return guild.getOwner().equals(player.getUniqueId());
    }

    @Override
    public boolean isGuildOwner(@NotNull UUID playerUUID) {
        Guild guild = isInGuild(playerUUID);
        if (guild == null) {
            return false;
        }
        return guild.getOwner().equals(playerUUID);
    }

    @Override
    public boolean hasRole(@NotNull Player player, @NotNull GuildRole role) {
        Guild guild = isInGuild(player);
        if (guild == null) {
            return false;
        }

        // Get the player's guild and check their role
        return guild.hasRole(player.getUniqueId(), role);
    }
}
