package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for displaying information about a guild.
 */
public class InfoSubCommand implements SubCommand {
    private final Furious plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Creates a new InfoSubCommand.
     *
     * @param plugin The plugin instance
     */
    public InfoSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Displays information about a guild.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild info [guild]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Displays information about your guild or the specified guild.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        Guild guild = null;

        if (args.length == 1) {
            // No guild specified, use player's guild
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Please specify a guild name.", NamedTextColor.RED));
                return true;
            }

            if (!plugin.getGuildManager().isInGuild(player.getUniqueId())) {
                player.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
                return true;
            }

            guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        } else {
            // Guild specified
            String guildName = args[1];
            guild = plugin.getGuildManager().getGuildByName(guildName);

            if (guild == null) {
                sender.sendMessage(Component.text("Guild not found!", NamedTextColor.RED));
                return true;
            }
        }

        // Display guild information
        displayGuildInfo(sender, guild);
        return true;
    }

    /**
     * Displays information about a guild to a command sender.
     *
     * @param sender The command sender
     * @param guild  The guild to display information about
     */
    private void displayGuildInfo(CommandSender sender, Guild guild) {
        sender.sendMessage(Component.text("=== Guild: " + guild.getName() + " ===", NamedTextColor.GOLD));

        // Owner
        Player owner = Bukkit.getPlayer(guild.getOwner());
        String ownerName = owner != null ? owner.getName() : "Offline Player";
        sender.sendMessage(Component.text("Owner: ", NamedTextColor.YELLOW)
                .append(Component.text(ownerName, NamedTextColor.WHITE)));

        // Description
        if (!guild.getDescription().isEmpty()) {
            sender.sendMessage(Component.text("Description: ", NamedTextColor.YELLOW)
                    .append(Component.text(guild.getDescription(), NamedTextColor.WHITE)));
        }

        // Created
        sender.sendMessage(Component.text("Created: ", NamedTextColor.YELLOW)
                .append(Component.text(dateFormat.format(guild.getCreationDate()), NamedTextColor.WHITE)));

        // Members
        sender.sendMessage(Component.text("Members (" + guild.getMemberCount() + "): ", NamedTextColor.YELLOW));

        // List online members
        List<Player> onlineMembers = guild.getOnlineMembers();
        if (!onlineMembers.isEmpty()) {
            StringBuilder onlineMembersStr = new StringBuilder();
            for (Player member : onlineMembers) {
                if (onlineMembersStr.length() > 0) {
                    onlineMembersStr.append(", ");
                }
                onlineMembersStr.append(member.getName());
            }
            sender.sendMessage(Component.text("  Online: ", NamedTextColor.GREEN)
                    .append(Component.text(onlineMembersStr.toString(), NamedTextColor.WHITE)));
        }

        // List offline members
        List<String> offlineMembers = new ArrayList<>();
        for (UUID memberId : guild.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null || !member.isOnline()) {
                // Try to get the player's name from their UUID
                String name = Bukkit.getOfflinePlayer(memberId).getName();
                if (name != null) {
                    offlineMembers.add(name);
                }
            }
        }

        if (!offlineMembers.isEmpty()) {
            StringBuilder offlineMembersStr = new StringBuilder();
            for (String name : offlineMembers) {
                if (offlineMembersStr.length() > 0) {
                    offlineMembersStr.append(", ");
                }
                offlineMembersStr.append(name);
            }
            sender.sendMessage(Component.text("  Offline: ", NamedTextColor.RED)
                    .append(Component.text(offlineMembersStr.toString(), NamedTextColor.WHITE)));
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
                if (guild.getName().toLowerCase().startsWith(partial)) {
                    completions.add(guild.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.info";
    }
}
