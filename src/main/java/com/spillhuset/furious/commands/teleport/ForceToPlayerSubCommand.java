package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
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

    public ForceToPlayerSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 1 || args.length > 2) {
            showUsage(sender);
            return true;
        }

        String command;
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Console must specify both source and destination players!", NamedTextColor.RED));
                return true;
            }
            command = "minecraft:teleport " + args[0];
        } else {
            command = "minecraft:teleport " + args[0] + " " + args[1];
        }

        return Bukkit.dispatchCommand(sender, command);
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(Component.text("/teleport <player> - Teleport yourself to a player", NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("/teleport <source> <destination> - Teleport a player to another player",
                NamedTextColor.YELLOW));
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