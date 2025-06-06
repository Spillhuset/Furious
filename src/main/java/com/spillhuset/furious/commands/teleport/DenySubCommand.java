package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DenySubCommand implements SubCommand {
    private final Furious plugin;

    public DenySubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        plugin.getTeleportManager().toggleDenyAll(player);

        boolean isDenying = plugin.getTeleportManager().isDenyingAll(player);

        sender.sendMessage(Component.text("Teleport requests are now " +
                        (isDenying ? "automatically denied" : "allowed") + "!",
                isDenying ? NamedTextColor.RED : NamedTextColor.GREEN));

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions needed for deny command
    }

    @Override
    public String getPermission() {
        return "furious.teleport.deny";
    }
}