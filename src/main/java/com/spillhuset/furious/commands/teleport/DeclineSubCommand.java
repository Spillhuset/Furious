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
import java.util.Set;
import java.util.UUID;

public class DeclineSubCommand implements SubCommand {
    private final Furious plugin;

    public DeclineSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        Player target = (Player) sender;
        Set<UUID> requests = plugin.getTeleportManager().getIncomingRequests(target);

        if (requests.isEmpty()) {
            sender.sendMessage(Component.text("You have no pending teleport requests!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 1) {
            if (requests.size() == 1) {
                Player requester = Bukkit.getPlayer(requests.iterator().next());
                if (requester != null) {
                    declineRequest(target, requester);
                }
            } else {
                sender.sendMessage(Component.text("You have multiple requests. Please specify a player:", NamedTextColor.YELLOW));
                for (UUID requesterId : requests) {
                    Player requester = Bukkit.getPlayer(requesterId);
                    if (requester != null) {
                        sender.sendMessage(Component.text("- " + requester.getName(), NamedTextColor.GRAY));
                    }
                }
            }
            return true;
        }

        Player requester = Bukkit.getPlayer(args[1]);
        if (requester == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        if (!plugin.getTeleportManager().hasIncomingRequest(target, requester)) {
            sender.sendMessage(Component.text("No pending request from that player!", NamedTextColor.RED));
            return true;
        }

        declineRequest(target, requester);
        return true;
    }

    private void declineRequest(Player target, Player requester) {
        plugin.getTeleportManager().declineRequest(target, requester);
        target.sendMessage(Component.text("Teleport request declined!", NamedTextColor.YELLOW));
        requester.sendMessage(Component.text(target.getName() + " declined your teleport request!", NamedTextColor.RED));
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2 && sender instanceof Player) {
            String partial = args[1].toLowerCase();
            Set<UUID> requests = plugin.getTeleportManager().getIncomingRequests((Player) sender);

            for (UUID requesterId : requests) {
                Player requester = Bukkit.getPlayer(requesterId);
                if (requester != null && requester.getName().toLowerCase().startsWith(partial)) {
                    completions.add(requester.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.teleport.decline";
    }
}