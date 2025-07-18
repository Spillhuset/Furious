package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.GuildSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for joining a guild.
 */
public class JoinSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new JoinSubCommand.
     *
     * @param plugin The plugin instance
     */
    public JoinSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Joins a guild or requests to join.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild join <guild>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Joins the specified guild if it's open or you've been invited.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If the guild is not open and you haven't been invited, sends a join request.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkGuildPermission(sender)) {
            return true;
        }

        if (args.length != 2) {
            getUsage(sender);
            return true;
        }

        Player player = (Player) sender;

        // Get the guild
        String guildName = args[1];
        Guild guild = plugin.getGuildManager().getGuildByName(guildName);

        if (guild == null) {
            player.sendMessage(Component.text("Guild not found!", NamedTextColor.RED));
            return true;
        }

        // Check if player is invited or guild is open
        if (guild.isInvited(player.getUniqueId())) {
            // Player is invited, join the guild
            if (plugin.getGuildManager().addPlayerToGuild(guild, player.getUniqueId())) {
                player.sendMessage(Component.text("You have joined " + guild.getName() + "!", NamedTextColor.GREEN));

                // Notify online guild members
                for (Player member : guild.getOnlineMembers()) {
                    if (!member.equals(player)) {
                        member.sendMessage(Component.text(player.getName() + " has joined the guild!", NamedTextColor.GREEN));
                    }
                }
            } else {
                player.sendMessage(Component.text("Failed to join " + guild.getName() + "!", NamedTextColor.RED));
            }
        } else if (guild.isOpen()) {
            // Guild is open, join without invitation
            if (plugin.getGuildManager().addPlayerToGuild(guild, player.getUniqueId())) {
                player.sendMessage(Component.text("You have joined " + guild.getName() + "!", NamedTextColor.GREEN));

                // Notify online guild members
                for (Player member : guild.getOnlineMembers()) {
                    if (!member.equals(player)) {
                        member.sendMessage(Component.text(player.getName() + " has joined the guild!", NamedTextColor.GREEN));
                    }
                }
            } else {
                player.sendMessage(Component.text("Failed to join " + guild.getName() + "!", NamedTextColor.RED));
            }
        } else if (guild.hasJoinRequest(player.getUniqueId())) {
            // Player already has a pending request
            player.sendMessage(Component.text("You already have a pending request to join " + guild.getName() + "!", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Please wait for a guild admin or owner to accept your request.", NamedTextColor.YELLOW));
        } else {
            // Guild is not open and player is not invited, send a join request
            if (plugin.getGuildManager().addJoinRequest(guild, player.getUniqueId())) {
                player.sendMessage(Component.text("You have requested to join " + guild.getName() + "!", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Your request will be reviewed by the guild admins and owner.", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("Failed to send join request to " + guild.getName() + "!", NamedTextColor.RED));
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Check permissions first
        if (!checkGuildPermission(sender, false)) {
            return completions;
        }

        if (sender instanceof Player player && args.length == 2) {
            String partial = args[1].toLowerCase();

            // Show all guilds, but prioritize guilds the player has been invited to or that are open
            List<String> invitedOrOpen = new ArrayList<>();
            List<String> others = new ArrayList<>();

            for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
                String guildName = guild.getName();
                if (guildName.toLowerCase().startsWith(partial)) {
                    if (guild.isInvited(player.getUniqueId()) || guild.isOpen()) {
                        invitedOrOpen.add(guildName);
                    } else {
                        others.add(guildName);
                    }
                }
            }

            // Add invited or open guilds first, then others
            completions.addAll(invitedOrOpen);
            completions.addAll(others);
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.join";
    }

    @Override
    public boolean denyOp() {
        return true;
    }

    @Override
    public GuildRole getRequiredRole() {
        // This command doesn't require a guild role since it's used by players who are not in a guild
        return null;
    }

    @Override
    public boolean checkGuildPermission(@NotNull CommandSender sender, boolean feedback) {
        // First check regular permissions
        if (!checkPermission(sender, feedback)) {
            return false;
        }

        // If not a player, they can't join a guild
        if (!(sender instanceof Player player)) {
            if (feedback) {
                sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            }
            return false;
        }

        // Check if player is already in a guild
        if (plugin.getGuildManager().isInGuild(player.getUniqueId())) {
            if (feedback) {
                sender.sendMessage(Component.text("You are already in a guild! Leave your current guild first.", NamedTextColor.RED));
            }
            return false;
        }

        return true;
    }
}
