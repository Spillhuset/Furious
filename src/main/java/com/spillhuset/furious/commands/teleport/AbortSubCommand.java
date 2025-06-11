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
import java.util.UUID;

public class AbortSubCommand implements SubCommand {
    private final Furious plugin;

    public AbortSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "abort";
    }

    @Override
    public String getDescription() {
        return "Aborts the current teleport request.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("/teleport abort", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("/teleport abort", NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("Aborts the current teleport request.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If no teleport request is in progress, this command will have no effect.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If the player has a pending teleport request to another player, this command will be ignored.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player requester)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        UUID targetId = plugin.getTeleportManager().getOutgoingRequest(requester);

        if (targetId == null) {
            sender.sendMessage(Component.text("You have no outgoing teleport request to abort!", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(targetId);
        plugin.getTeleportManager().cancelRequest(requester);

        sender.sendMessage(Component.text("Teleport request aborted!", NamedTextColor.YELLOW));

        if (target != null) {
            target.sendMessage(Component.text(requester.getName() + " cancelled their teleport request.",
                    NamedTextColor.YELLOW));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions needed for abort command
    }

    @Override
    public String getPermission() {
        return "furious.teleport.abort";
    }
}