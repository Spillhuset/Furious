package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.entities.Home;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Subcommand for listing guild homes.
 */
public class HomesListSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new HomesListSubCommand.
     *
     * @param plugin The plugin instance
     */
    public HomesListSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Lists all guild homes.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guilds homes list - Lists all guild homes", NamedTextColor.YELLOW));
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

        // Get all guild homes
        Collection<Home> homes = plugin.getHomesManager().getGuildHomes(guild.getId());

        if (homes.isEmpty()) {
            player.sendMessage(Component.text("Your guild has no homes set!", NamedTextColor.YELLOW));
            return true;
        }

        // Display the list of homes
        player.sendMessage(Component.text("Guild Homes:", NamedTextColor.GOLD));
        for (Home home : homes) {
            // Format the location for display
            Location loc = home.getLocation();
            String worldName = loc != null ? loc.getWorld().getName() : "Unknown";
            String location = String.format("World: %s, X: %.1f, Y: %.1f, Z: %.1f",
                worldName,
                home.getX(),
                home.getY(),
                home.getZ());

            player.sendMessage(Component.text("• " + home.getName() + " - " + location, NamedTextColor.YELLOW));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completions needed for list command
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.homes.list";
    }
}