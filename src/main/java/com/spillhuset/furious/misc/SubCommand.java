package com.spillhuset.furious.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SubCommand {
    String getName();

    String getDescription();

    String getUsage(CommandSender sender);

    default boolean allowPlayer() {
        return true;
    }

    default boolean allowNonPlayer() {
        return true;
    }

    default boolean allowOp() {
        return true;
    }

    default boolean denyPlayer() {
        return false;
    }

    default boolean denyNonPlayer() {
        return false;
    }

    default boolean denyOp() {
        return false;
    }

    boolean execute(@NotNull CommandSender sender, @NotNull String[] args);

    List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args);

    String getPermission();

    default boolean checkPermission(@NotNull CommandSender sender) {
        if (sender.isOp() && !allowOp() && denyOp()) {
            sender.sendMessage(Component.text("Op players cannot use this command! Please use ")
                    .append(Component.text("/furious op")
                            .clickEvent(ClickEvent.runCommand("/furious op"))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to run /furious op")))
                            .color(NamedTextColor.AQUA))
                    .append(Component.text(" to remove your op status."))
                    .color(NamedTextColor.RED));

            return false;
        }
        if (sender instanceof ConsoleCommandSender && !allowNonPlayer() && denyNonPlayer()) {
            sender.sendMessage(Component.text("Console cannot use this command!", NamedTextColor.RED));
            return false;
        }
        if (sender instanceof Player && !allowPlayer() && denyPlayer()) {
            sender.sendMessage(Component.text("Players cannot use this command!", NamedTextColor.RED));
            return false;
        }

        if (getPermission() == null) {
            return true;
        }
        if (sender.hasPermission(getPermission())) {
            return true;
        } else {
            sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
            return false;
        }
    }
}