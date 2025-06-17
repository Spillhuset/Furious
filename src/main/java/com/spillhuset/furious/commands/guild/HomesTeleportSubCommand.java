package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.entities.Home;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for teleporting to a guild home.
 */
public class HomesTeleportSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new HomesTeleportSubCommand.
     *
     * @param plugin The plugin instance
     */
    public HomesTeleportSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String getDescription() {
        return "Teleports you to a guild home.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guilds homes teleport <name> - Teleports you to a guild home", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guilds homes tp <name> - Alias for teleport command", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Get the player's guild
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
            return true;
        }

        // Get the home name
        String homeName = args.length >= 3 ? args[2] : "default";

        // Teleport to the guild home
        if (plugin.getHomesManager().teleportToGuildHome(player, guild, homeName)) {
            // Success message is sent by the manager
            return true;
        } else {
            // Failure message is sent by the manager
            return true;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 3 && sender instanceof Player player) {
            // Suggest existing home names
            Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
            if (guild != null) {
                String partial = args[2].toLowerCase();
                plugin.getHomesManager().getGuildHomes(guild.getId()).forEach(home -> {
                    if (home.getName().startsWith(partial)) {
                        completions.add(home.getName());
                    }
                });
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.homes.teleport";
    }
}