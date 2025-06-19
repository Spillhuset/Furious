package com.spillhuset.furious.commands.warps;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Subcommand for creating a warp.
 */
public class CreateSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new CreateSubCommand.
     *
     * @param plugin The plugin instance
     */
    public CreateSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Creates a warp at your current location.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/warps create <name> [cost=<cost>] [passwd=<password>] - Creates a warp at your current location", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if player is op
        if (!player.isOp()) {
            sender.sendMessage(Component.text("Only operators can create warps!", NamedTextColor.RED));
            return true;
        }

        // Check if enough arguments
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String warpName = args[1];
        double cost = 0.0;
        String password = null;

        // Parse optional arguments
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];

            if (arg.startsWith("cost=")) {
                try {
                    cost = Double.parseDouble(arg.substring(5));
                    if (cost < 0) {
                        sender.sendMessage(Component.text("Cost cannot be negative!", NamedTextColor.RED));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid cost value!", NamedTextColor.RED));
                    return true;
                }
            } else if (arg.startsWith("passwd=")) {
                password = arg.substring(7);
                if (password.isEmpty()) {
                    sender.sendMessage(Component.text("Password cannot be empty!", NamedTextColor.RED));
                    return true;
                }
            }
        }

        // Create the warp
        if (plugin.getWarpsManager().createWarp(player, warpName, cost, password)) {
            Component message = Component.text("Warp '" + warpName + "' created successfully!", NamedTextColor.GREEN);

            if (cost > 0) {
                message = message.append(Component.text(" Cost: " + cost, NamedTextColor.YELLOW));
            }

            if (password != null) {
                message = message.append(Component.text(" Password protected.", NamedTextColor.YELLOW));
            }

            sender.sendMessage(message);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Suggest some common warp names
            String partial = args[1].toLowerCase();
            List<String> suggestions = Arrays.asList("spawn", "shop", "mine", "farm", "nether", "end", "pvp", "arena");

            for (String suggestion : suggestions) {
                if (suggestion.startsWith(partial)) {
                    completions.add(suggestion);
                }
            }
        } else if (args.length >= 3) {
            String partial = args[args.length - 1].toLowerCase();

            // Suggest cost and password parameters
            if ("cost=".startsWith(partial)) {
                completions.add("cost=0");
            }

            if ("passwd=".startsWith(partial)) {
                completions.add("passwd=");
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.warps.create";
    }
}
