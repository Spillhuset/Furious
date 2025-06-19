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
import java.util.UUID;

/**
 * Subcommand for transferring guild ownership to another player.
 */
public class TransferSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new TransferSubCommand.
     *
     * @param plugin The plugin instance
     */
    public TransferSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "transfer";
    }

    @Override
    public String getDescription() {
        return "Transfers guild ownership to another member.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild transfer <player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Transfers guild ownership to the specified player.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("The player must be a member of your guild.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("You will become an admin in the guild.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkGuildPermission(sender)) {
            return true;
        }

        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        Player player = (Player) sender;
        Guild guild = isInGuild(player);

        // Get target player
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return true;
        }

        UUID targetId = targetPlayer.getUniqueId();

        // Check if target is in the guild
        if (!guild.isMember(targetId)) {
            sender.sendMessage(Component.text(targetName + " is not a member of your guild!", NamedTextColor.RED));
            return true;
        }

        // Check if target is already the owner
        if (guild.getOwner().equals(targetId)) {
            sender.sendMessage(Component.text(targetName + " is already the owner of your guild!", NamedTextColor.RED));
            return true;
        }

        // Transfer ownership
        if (plugin.getGuildManager().transferOwnership(guild, targetId)) {
            // Notify both players
            sender.sendMessage(Component.text("You have transferred ownership of your guild to " + targetName + ".", NamedTextColor.GREEN));
            targetPlayer.sendMessage(Component.text(player.getName() + " has transferred ownership of the guild to you!", NamedTextColor.GREEN));

            // Notify other online guild members
            for (Player member : guild.getOnlineMembers()) {
                if (!member.equals(player) && !member.equals(targetPlayer)) {
                    member.sendMessage(Component.text(player.getName() + " has transferred guild ownership to " + targetName + ".", NamedTextColor.YELLOW));
                }
            }

            return true;
        } else {
            sender.sendMessage(Component.text("Failed to transfer guild ownership.", NamedTextColor.RED));
            return true;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        Guild guild = isInGuild(player);
        if (guild == null) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            String partial = args[1].toLowerCase();

            // Add guild members except the owner
            for (UUID memberId : guild.getMembers()) {
                if (!memberId.equals(guild.getOwner())) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null && member.getName().toLowerCase().startsWith(partial)) {
                        completions.add(member.getName());
                    }
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.transfer";
    }

    @Override
    public GuildRole getRequiredRole() {
        // This command requires the player to be the guild owner
        return GuildRole.OWNER;
    }
}