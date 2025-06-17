package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FeedCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    public FeedCommand(Furious furious) {
        this.plugin = furious;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        int maxFood = 20;

        if (args.length == 0 && sender.hasPermission("furious.feed.self")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }

            player.setFoodLevel(maxFood);
            player.sendMessage(Component.text("You have been fed to full stomach!", NamedTextColor.GREEN));
        }
        if (args.length >= 1 && sender.hasPermission("furious.feed.others")) {
            StringBuilder found = new StringBuilder();
            StringBuilder notFound = new StringBuilder();
            for (String arg : args) {
                Player target = plugin.getServer().getPlayer(arg);
                if (target == null) {
                    notFound.append(arg).append(", ");
                } else {
                    found.append(arg).append(", ");
                    target.setFoodLevel(maxFood);
                    target.sendMessage(Component.text("You have been fed to full stomach!", NamedTextColor.GREEN));
                }
            }
            if (!notFound.isEmpty()) {
                notFound.delete(notFound.lastIndexOf(", "),notFound.length() );
                sender.sendMessage(Component.text("Could not find player(s): "+notFound, NamedTextColor.RED));
                return true;
            }
            if (!found.isEmpty()) {
                found.delete(found.lastIndexOf(", "), found.length());
                sender.sendMessage(Component.text("Feed player(s): "+found, NamedTextColor.GREEN));
            }
            if (found.isEmpty() && notFound.isEmpty()) {
                sender.sendMessage(Component.text("No players found!", NamedTextColor.RED));
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length >= 1) {
            String partial = args[args.length - 1].toLowerCase();
            List<String> alreadyAdded = new ArrayList<>();

            // Add all arguments except the current one to the already added list
            if (args.length > 1) {
                for (int i = 0; i < args.length - 1; i++) {
                    alreadyAdded.add(args[i].toLowerCase());
                }
            }

            // Add matching player names that haven't been added yet
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();
                if (playerName.toLowerCase().startsWith(partial) &&
                    !alreadyAdded.contains(playerName.toLowerCase())) {
                    completions.add(playerName);
                }
            }
        }

        return completions.isEmpty() ? Collections.emptyList() : completions;
    }
}
