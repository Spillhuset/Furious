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
 * Subcommand for linking a warp to a portal.
 */
public class LinkSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new LinkSubCommand.
     *
     * @param plugin The plugin instance
     */
    public LinkSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "link";
    }

    @Override
    public String getDescription() {
        return "Links a warp to a portal.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/warps link <name> [water|lava|air] - Links a warp to a portal", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("You must be looking at a gold block when using this command.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("The gold block must have another gold block placed diagonally from it.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if player is op
        if (!player.isOp()) {
            sender.sendMessage(Component.text("Only operators can link warps to portals!", NamedTextColor.RED));
            return true;
        }

        // Check if enough arguments
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String warpName = args[1];
        String filling = args.length >= 3 ? args[2].toLowerCase() : "air";

        // Validate filling
        if (!filling.equals("water") && !filling.equals("lava") && !filling.equals("air")) {
            sender.sendMessage(Component.text("Invalid filling material! Use 'water', 'lava', or 'air'.", NamedTextColor.RED));
            return true;
        }

        // Link the warp to a portal
        if (plugin.getWarpsManager().linkWarp(player, warpName, filling)) {
            sender.sendMessage(Component.text("Warp '" + warpName + "' linked to portal successfully!", NamedTextColor.GREEN));
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
                // Only suggest warps that don't have a password
                if (!warp.hasPassword() && warp.getName().startsWith(partial)) {
                    completions.add(warp.getName());
                }
            });
        } else if (args.length == 3 && sender.isOp()) {
            // Suggest filling materials
            String partial = args[2].toLowerCase();
            for (String material : new String[]{"water", "lava", "air"}) {
                if (material.startsWith(partial)) {
                    completions.add(material);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.warps.link";
    }
}