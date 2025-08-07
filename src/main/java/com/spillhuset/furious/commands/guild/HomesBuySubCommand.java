package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for buying a guild home slot.
 */
public class HomesBuySubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new HomesBuySubCommand.
     *
     * @param plugin The plugin instance
     */
    public HomesBuySubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "buy";
    }

    @Override
    public String getDescription() {
        return "Shows information about buying a guild home slot.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild homes buy - Shows information about buying a guild home slot", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild homes buy confirm - Purchases a guild home slot", NamedTextColor.YELLOW));
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

        // Check if player has permission to buy homes (admin or owner)
        if (!guild.hasRole(player.getUniqueId(), GuildRole.ADMIN) && !guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a guild admin or owner to buy guild home slots!", NamedTextColor.RED));
            return true;
        }

        // Check if this is a "buy confirm" command
        if (args.length >= 3 && args[2].equalsIgnoreCase("confirm")) {
            // Purchase the guild home slot
            if (plugin.getHomesManager().purchaseGuildHomeSlot(player, guild)) {
                // Success message is sent by the manager
                return true;
            } else {
                // Failure message is sent by the manager
                return true;
            }
        }

        // Get the cost of a new home slot
        int currentHomes = plugin.getHomesManager().getGuildHomes(guild.getId()).size();
        double cost = plugin.getHomesManager().getGuildHomeSlotCost(guild);

        // Show information about buying a home slot
        player.sendMessage(Component.text("Guild Home Slot Purchase", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Current home slots: " + currentHomes, NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Cost for next slot: $" + cost, NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Your balance: $" + plugin.getWalletManager().getBalance(player), NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Use '/guild homes buy confirm' to purchase a new home slot.", NamedTextColor.GREEN));

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 3) {
            String partial = args[2].toLowerCase();
            if ("confirm".startsWith(partial)) {
                completions.add("confirm");
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.homes.buy";
    }
}