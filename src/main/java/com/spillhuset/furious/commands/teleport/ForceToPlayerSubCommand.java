package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import com.spillhuset.furious.utils.AuditLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ForceToPlayerSubCommand implements SubCommand {

    private final Furious plugin;
    private final AuditLogger auditLogger;

    public ForceToPlayerSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "force";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 1 || args.length > 2) {
            getUsage(sender);
            return true;
        }

        String command;
        String sourcePlayer;
        String destinationPlayer;

        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Console must specify both source and destination players!", NamedTextColor.RED));
                return true;
            }
            sourcePlayer = sender.getName();
            destinationPlayer = args[0];
            command = "minecraft:teleport " + sourcePlayer + " " + args[0];
        } else {
            sourcePlayer = args[0];
            destinationPlayer = args[1];
            command = "minecraft:teleport " + args[0] + " " + args[1];
        }

        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        // Log the teleport operation
        if (success) {
            auditLogger.logTeleportOperation(
                sender,
                sourcePlayer,
                destinationPlayer,
                "Force teleport command executed"
            );
        } else {
            auditLogger.logFailedAccess(
                sender,
                sourcePlayer,
                "force teleport to " + destinationPlayer,
                "Command execution failed"
            );
        }

        return success;
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(Component.text("/teleport force <source player> <destination player>", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("/teleport force <source player>", NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("/teleport force <destination player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If no destination player is specified, the player will be teleported to their current location.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If the source player is not online, the teleport will be attempted when they log in.", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length <= 2) {
            String partial = args[args.length - 1].toLowerCase();
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
        return "furious.teleport.force";
    }
}
