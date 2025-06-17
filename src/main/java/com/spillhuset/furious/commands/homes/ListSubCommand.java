package com.spillhuset.furious.commands.homes;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Home;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Subcommand for listing homes.
 */
public class ListSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new ListSubCommand.
     *
     * @param plugin The plugin instance
     */
    public ListSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Lists your homes.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/homes list - Lists your homes", NamedTextColor.YELLOW));

        if (sender.hasPermission("furious.homes.admin")) {
            sender.sendMessage(Component.text("/homes list <player> - Lists another player's homes", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        // Admin command: /homes list <player>
        if (args.length >= 2 && sender.hasPermission("furious.homes.admin")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }

            Collection<Home> homes = plugin.getHomesManager().getPlayerHomes(target.getUniqueId());
            if (homes.isEmpty()) {
                sender.sendMessage(Component.text(target.getName() + " has no homes!", NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text(target.getName() + "'s Homes:", NamedTextColor.GOLD));
                displayHomes(sender, homes, target.getName());
            }
            return true;
        }

        // Regular command: /homes list
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        Collection<Home> homes = plugin.getHomesManager().getPlayerHomes(player.getUniqueId());
        if (homes.isEmpty()) {
            player.sendMessage(Component.text("You have no homes! Use /homes set to create one.", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("Your Homes:", NamedTextColor.GOLD));
            displayHomes(player, homes, null);

            // Show home limit information
            int maxHomes = plugin.getHomesManager().getMaxPlayerHomes(player);
            int currentHomes = homes.size();
            player.sendMessage(Component.text("You have " + currentHomes + " of " + maxHomes + " homes.", NamedTextColor.GRAY));

            // Show purchase information if at limit
            if (currentHomes >= maxHomes) {
                double cost = plugin.getHomesManager().getHomeSlotCost(player);
                player.sendMessage(Component.text("You can purchase an additional home slot for " + cost + " with /homes buy", NamedTextColor.GRAY));
            }
        }

        return true;
    }

    /**
     * Displays a list of homes to a command sender.
     *
     * @param sender The command sender to display the homes to
     * @param homes The collection of homes to display
     * @param playerName The name of the player whose homes are being displayed, or null if displaying the sender's own homes
     */
    private void displayHomes(CommandSender sender, Collection<Home> homes, String playerName) {
        for (Home home : homes) {
            Location loc = home.getLocation();
            if (loc == null) {
                // World doesn't exist
                Component message = Component.text("• " + home.getName() + " (World not found)", NamedTextColor.RED);
                sender.sendMessage(message);
                continue;
            }

            String coords = String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
            String worldName = loc.getWorld().getName();

            Component message = Component.text("• " + home.getName() + " - " + worldName + " (" + coords + ")", NamedTextColor.YELLOW);

            // Add click and hover events if the sender is a player
            if (sender instanceof Player) {
                String command = playerName == null ?
                        "/homes tp " + home.getName() :
                        "/homes tp " + playerName + " " + home.getName();

                message = message
                        .clickEvent(ClickEvent.runCommand(command))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to teleport", NamedTextColor.GREEN)));
            }

            sender.sendMessage(message);
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2 && sender.hasPermission("furious.homes.admin")) {
            // Suggest player names for admin command
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.homes.list";
    }
}