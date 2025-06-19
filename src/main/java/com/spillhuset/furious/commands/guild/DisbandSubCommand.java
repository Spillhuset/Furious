package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.GuildSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for disbanding a guild.
 */
public class DisbandSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new DisbandSubCommand.
     *
     * @param plugin The plugin instance
     */
    public DisbandSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "disband";
    }

    @Override
    public String getDescription() {
        return "Disbands a guild.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));

        if (sender instanceof Player) {
            sender.sendMessage(Component.text("/guild disband", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Disbands your guild. This action cannot be undone!", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("/guild disband <guild>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Disbands the specified guild. This action cannot be undone!", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        // Check if sender is a player or console
        if (sender instanceof Player player) {
            // Player execution
            if (!checkGuildPermission(sender)) {
                return true;
            }

            Guild guild = isInGuild(player);

            // Confirm disbanding
            if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
                sender.sendMessage(Component.text("WARNING: This will permanently disband your guild!", NamedTextColor.RED));
                sender.sendMessage(Component.text("All claimed chunks will be unclaimed and all members will be removed.", NamedTextColor.RED));
                sender.sendMessage(Component.text("To confirm, type: /guild disband confirm", NamedTextColor.YELLOW));
                return true;
            }

            // Disband the guild
            if (plugin.getGuildManager().adminDisbandGuild(guild, player)) {
                // Notifications are handled in the adminDisbandGuild method
                return true;
            } else {
                sender.sendMessage(Component.text("Failed to disband your guild.", NamedTextColor.RED));
                return true;
            }
        } else {
            // Console execution
            if (!sender.hasPermission("furious.guild.admin")) {
                sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
                return true;
            }

            if (args.length < 2) {
                getUsage(sender);
                return true;
            }

            // Get the guild by name
            String guildName = args[1];
            Guild guild = plugin.getGuildManager().getGuildByName(guildName);

            if (guild == null) {
                sender.sendMessage(Component.text("Guild not found: " + guildName, NamedTextColor.RED));
                return true;
            }

            // Confirm disbanding
            if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                sender.sendMessage(Component.text("WARNING: This will permanently disband the guild " + guildName + "!", NamedTextColor.RED));
                sender.sendMessage(Component.text("All claimed chunks will be unclaimed and all members will be removed.", NamedTextColor.RED));
                sender.sendMessage(Component.text("To confirm, type: /guild disband " + guildName + " confirm", NamedTextColor.YELLOW));
                return true;
            }

            // Create a "fake" admin player for the adminDisbandGuild method
            // This is a workaround since we're executing from console
            Player adminPlayer = Bukkit.getConsoleSender().getServer().getPlayer(Bukkit.getConsoleSender().getName());

            if (adminPlayer == null) {
                // If we can't get a player, just use the first online op
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.isOp()) {
                        adminPlayer = p;
                        break;
                    }
                }

                if (adminPlayer == null) {
                    sender.sendMessage(Component.text("Cannot disband guild: No admin player available.", NamedTextColor.RED));
                    return true;
                }
            }

            // Disband the guild
            if (plugin.getGuildManager().adminDisbandGuild(guild, adminPlayer)) {
                sender.sendMessage(Component.text("Guild " + guildName + " has been disbanded!", NamedTextColor.GREEN));
                return true;
            } else {
                sender.sendMessage(Component.text("Failed to disband guild " + guildName + ".", NamedTextColor.RED));
                return true;
            }
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (sender instanceof Player) {
            // Player tab completion
            if (args.length == 2) {
                if ("confirm".startsWith(args[1].toLowerCase())) {
                    completions.add("confirm");
                }
            }
        } else {
            // Console tab completion
            if (args.length == 2) {
                // Suggest guild names
                String partial = args[1].toLowerCase();
                for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
                    if (guild.getName().toLowerCase().startsWith(partial)) {
                        completions.add(guild.getName());
                    }
                }
            } else if (args.length == 3) {
                if ("confirm".startsWith(args[2].toLowerCase())) {
                    completions.add("confirm");
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.disband";
    }

    @Override
    public GuildRole getRequiredRole() {
        // This command requires the player to be the guild owner
        return GuildRole.OWNER;
    }
}