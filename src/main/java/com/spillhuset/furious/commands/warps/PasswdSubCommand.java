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
 * Subcommand for setting the password of a warp.
 */
public class PasswdSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new PasswdSubCommand.
     *
     * @param plugin The plugin instance
     */
    public PasswdSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "passwd";
    }

    @Override
    public String getDescription() {
        return "Sets or removes the password for a warp.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/warps passwd <name> <password> - Sets the password for a warp", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/warps passwd <name> - Removes the password from a warp", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if player has permission
        if (!player.hasPermission(getPermission())) {
            sender.sendMessage(Component.text("You don't have permission to set warp passwords!", NamedTextColor.RED));
            return true;
        }

        // Check if enough arguments
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String warpName = args[1];
        String password = args.length >= 3 ? args[2] : null;

        // Set or remove the password
        // Let WarpsManager.setPassword() handle the success message to avoid double notification
        plugin.getWarpsManager().setPassword(player, warpName, password);

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2 && sender.hasPermission(getPermission())) {
            // Suggest warp names
            String partial = args[1].toLowerCase();
            plugin.getWarpsManager().getAllWarps().forEach(warp -> {
                if (warp.getName().startsWith(partial)) {
                    completions.add(warp.getName());
                }
            });
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.warps.passwd";
    }
}