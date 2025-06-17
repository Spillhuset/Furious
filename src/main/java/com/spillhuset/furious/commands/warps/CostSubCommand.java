package com.spillhuset.furious.commands.warps;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for setting the cost of a warp.
 */
public class CostSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new CostSubCommand.
     *
     * @param plugin The plugin instance
     */
    public CostSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "cost";
    }

    @Override
    public String getDescription() {
        return "Sets the cost to use a warp.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/warps cost <name> <cost> - Sets the cost to use a warp", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if player is op
        if (!player.isOp()) {
            sender.sendMessage(Component.text("Only operators can set warp costs!", NamedTextColor.RED));
            return true;
        }

        // Check if enough arguments
        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        String warpName = args[1];
        double cost;

        // Parse cost
        try {
            cost = Double.parseDouble(args[2]);
            if (cost < 0) {
                sender.sendMessage(Component.text("Cost cannot be negative!", NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid cost value!", NamedTextColor.RED));
            return true;
        }

        // Set the cost
        if (plugin.getWarpsManager().setCost(player, warpName, cost)) {
            sender.sendMessage(Component.text("Cost for warp '" + warpName + "' set to " + cost + "!", NamedTextColor.GREEN));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2 && sender.isOp()) {
            // Suggest warp names
            String partial = args[1].toLowerCase();
            plugin.getWarpsManager().getAllWarps().forEach(warp -> {
                if (warp.getName().startsWith(partial)) {
                    completions.add(warp.getName());
                }
            });
        } else if (args.length == 3 && sender.isOp()) {
            // Suggest some common cost values
            String partial = args[2].toLowerCase();
            for (String cost : new String[]{"0", "10", "50", "100", "500", "1000"}) {
                if (cost.startsWith(partial)) {
                    completions.add(cost);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.warps.cost";
    }
}