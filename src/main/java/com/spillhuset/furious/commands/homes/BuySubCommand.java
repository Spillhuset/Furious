package com.spillhuset.furious.commands.homes;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for buying additional home slots.
 */
public class BuySubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new BuySubCommand.
     *
     * @param plugin The plugin instance
     */
    public BuySubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "buy";
    }

    @Override
    public String getDescription() {
        return "Purchases an additional home slot.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/homes buy - Purchases an additional home slot", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/homes buy guild - Purchases an additional guild home slot", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Guild home slot purchase: /homes buy guild
        if (args.length >= 2 && args[1].equalsIgnoreCase("guild")) {
            // Check if player is in a guild
            Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
            if (guild == null) {
                player.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
                return true;
            }

            // Check if player is the guild owner
            if (!guild.getOwner().equals(player.getUniqueId())) {
                player.sendMessage(Component.text("Only the guild owner can purchase guild home slots!", NamedTextColor.RED));
                return true;
            }

            // Get the cost
            double cost = plugin.getHomesManager().getGuildHomeSlotCost(guild);

            // Confirm purchase
            if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                player.sendMessage(Component.text("A guild home slot will cost " + cost + ". Type /homes buy guild confirm to purchase.", NamedTextColor.YELLOW));
                return true;
            }

            // Purchase the slot
            if (plugin.getHomesManager().purchaseGuildHomeSlot(player, guild)) {
                // Success message is sent by the manager
                return true;
            } else {
                // Failure message is sent by the manager
                return true;
            }
        }

        // Regular home slot purchase: /homes buy
        // Get the cost
        double cost = plugin.getHomesManager().getHomeSlotCost(player);

        // Confirm purchase
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage(Component.text("A home slot will cost " + cost + ". Type /homes buy confirm to purchase.", NamedTextColor.YELLOW));
            return true;
        }

        // Purchase the slot
        if (plugin.getHomesManager().purchaseHomeSlot(player)) {
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

        if (args.length == 2) {
            String partial = args[1].toLowerCase();

            // Suggest "guild" if player is in a guild and is the owner
            if (sender instanceof Player player) {
                Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (guild != null && guild.getOwner().equals(player.getUniqueId())) {
                    if ("guild".startsWith(partial)) {
                        completions.add("guild");
                    }
                }
            }

            // Suggest "confirm" for regular purchase
            if ("confirm".startsWith(partial)) {
                completions.add("confirm");
            }
        } else if (args.length == 3 && args[1].equalsIgnoreCase("guild")) {
            // Suggest "confirm" for guild purchase
            String partial = args[2].toLowerCase();
            if ("confirm".startsWith(partial)) {
                completions.add("confirm");
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.homes.buy";
    }
}