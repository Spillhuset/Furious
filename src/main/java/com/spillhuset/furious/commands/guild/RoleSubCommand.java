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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for changing a guild member's role.
 */
public class RoleSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new RoleSubCommand.
     *
     * @param plugin The plugin instance
     */
    public RoleSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "role";
    }

    @Override
    public String getDescription() {
        return "Changes a guild member's role.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild role <player> <role>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Changes the specified player's role in your guild.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Available roles: ADMIN, MOD, USER", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Only the guild owner can change member roles.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkGuildPermission(sender)) {
            return true;
        }

        if (args.length < 3) {
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

        // Parse the role
        String roleStr = args[2].toUpperCase();
        GuildRole role;
        try {
            role = GuildRole.valueOf(roleStr);

            // Don't allow setting someone as OWNER through this command
            if (role == GuildRole.OWNER) {
                sender.sendMessage(Component.text("You cannot set a player's role to OWNER. Use /guild transfer instead.", NamedTextColor.RED));
                return true;
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid role: " + roleStr, NamedTextColor.RED));
            sender.sendMessage(Component.text("Available roles: ADMIN, MOD, USER", NamedTextColor.RED));
            return true;
        }

        // Change the role
        if (plugin.getGuildManager().changeMemberRole(guild, targetId, role, player)) {
            // Success message is sent by the manager
            return true;
        } else {
            // Failure message is sent by the manager
            return true;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        Guild guild = isInGuild(player);
        if (guild == null || !guild.getOwner().equals(player.getUniqueId())) {
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
        } else if (args.length == 3) {
            List<String> completions = new ArrayList<>();
            String partial = args[2].toUpperCase();

            // Add available roles (excluding OWNER)
            for (GuildRole role : GuildRole.values()) {
                if (role != GuildRole.OWNER && role.name().startsWith(partial)) {
                    completions.add(role.name());
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.role";
    }

    @Override
    public GuildRole getRequiredRole() {
        // This command requires the player to be the guild owner
        return GuildRole.OWNER;
    }
}