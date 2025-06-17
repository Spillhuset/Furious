package com.spillhuset.furious.commands.warps;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Warp;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Subcommand for listing available warps.
 */
public class ListSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new ListSubCommand.
     *
     * @param plugin The plugin instance
     */
    public ListSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Lists all available warps.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/warps list - Lists all available warps", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        Collection<Warp> warps = plugin.getWarpsManager().getAllWarps();

        if (warps.isEmpty()) {
            sender.sendMessage(Component.text("There are no warps available.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("Available Warps:", NamedTextColor.GOLD));

        for (Warp warp : warps) {
            Component warpComponent = Component.text("- ", NamedTextColor.YELLOW)
                    .append(Component.text(warp.getName(), NamedTextColor.GREEN));

            // Add cost information if applicable
            if (warp.getCost() > 0) {
                warpComponent = warpComponent.append(Component.text(" (Cost: " + warp.getCost() + ")", NamedTextColor.YELLOW));
            }

            // Add password indicator if applicable
            if (warp.hasPassword()) {
                warpComponent = warpComponent.append(Component.text(" [Password Protected]", NamedTextColor.RED));
            }

            // Add portal indicator if applicable
            if (warp.hasPortal()) {
                warpComponent = warpComponent.append(Component.text(" [Portal]", NamedTextColor.AQUA));
            }

            // Make the warp name clickable for players
            if (sender instanceof Player) {
                warpComponent = warpComponent.clickEvent(ClickEvent.runCommand("/warps warp " + warp.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to " + warp.getName(), NamedTextColor.GRAY)));
            }

            sender.sendMessage(warpComponent);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completions for this command
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.warps.list";
    }
}