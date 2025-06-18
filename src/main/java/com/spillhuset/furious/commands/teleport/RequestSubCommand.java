package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RequestSubCommand implements SubCommand {
    private final Furious plugin;

    public RequestSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "request";
    }

    @Override
    public String getDescription() {
        return "Requests a teleport to another player.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("/teleport request <player>", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("/teleport request <player>", NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("Requests a teleport to another player.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If the player is not online, the teleport will be attempted when they log in.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If the player has a pending teleport request to another player, this command will be ignored.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player requester)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /teleport request <player>", NamedTextColor.RED));
            return true;
        }

        // Sanitize the player name
        String targetName = com.spillhuset.furious.utils.InputSanitizer.sanitizePlayerName(args[1]);

        // Check if the player name is valid
        if (targetName == null) {
            sender.sendMessage(Component.text("Invalid player name! Please use a valid Minecraft username.", NamedTextColor.RED));
            return true;
        }

        // Check if the input is safe
        if (!com.spillhuset.furious.utils.InputSanitizer.isSafeInput(args[1])) {
            sender.sendMessage(Component.text("Invalid input detected! Please use only alphanumeric characters and underscores.", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        if (target == requester) {
            sender.sendMessage(Component.text("You cannot request to teleport to yourself!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getTeleportManager().sendRequest(requester, target)) {
            requester.sendMessage(Component.text("Teleport request sent to " + target.getName(), NamedTextColor.GREEN));
            target.sendMessage(Component.text(requester.getName() + " has requested to teleport to you", NamedTextColor.GOLD)
                    .append(Component.newline())
                    .append(Component.text("Use /teleport accept " + requester.getName() + " to accept", NamedTextColor.YELLOW))
                    .append(Component.newline())
                    .append(Component.text("Use /teleport decline " + requester.getName() + " to decline", NamedTextColor.YELLOW)));
        } else {
            sender.sendMessage(Component.text("Could not send teleport request!", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player != sender && player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.teleport.request";
    }
}
