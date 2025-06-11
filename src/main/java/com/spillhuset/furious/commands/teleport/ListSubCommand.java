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

public class ListSubCommand implements SubCommand {
    private final Furious plugin;

    public ListSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Lists all pending teleport requests.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("/teleport list <in|out|all>", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("/teleport list <in|out|all>", NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("Lists all pending teleport requests.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If no teleport request is in progress, this command will have no effect.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If the player has a pending teleport request to another player, this command will be ignored.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        String type = args.length > 1 ? args[1].toLowerCase() : "all";

        switch (type) {
            case "in":
                listIncoming(player);
                break;
            case "out":
                listOutgoing(player);
                break;
            default:
                listAll(player);
                break;
        }

        return true;
    }

    private void listIncoming(Player player) {
        Set<UUID> requests = plugin.getTeleportManager().getIncomingRequests(player);

        if (requests.isEmpty()) {
            player.sendMessage(Component.text("You have no incoming teleport requests.", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("Incoming teleport requests:", NamedTextColor.GOLD));
        for (UUID requesterId : requests) {
            Player requester = Bukkit.getPlayer(requesterId);
            if (requester != null) {
                player.sendMessage(Component.text("- " + requester.getName(), NamedTextColor.YELLOW));
            }
        }
    }

    private void listOutgoing(Player player) {
        UUID targetId = plugin.getTeleportManager().getOutgoingRequest(player);

        if (targetId == null) {
            player.sendMessage(Component.text("You have no outgoing teleport requests.", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target != null) {
            player.sendMessage(Component.text("Outgoing teleport request:", NamedTextColor.GOLD));
            player.sendMessage(Component.text("- To: " + target.getName(), NamedTextColor.YELLOW));
        }
    }

    private void listAll(Player player) {
        listIncoming(player);
        player.sendMessage(Component.text(""));
        listOutgoing(player);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (String option : new String[]{"in", "out"}) {
                if (option.startsWith(partial)) {
                    completions.add(option);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.teleport.list";
    }
}