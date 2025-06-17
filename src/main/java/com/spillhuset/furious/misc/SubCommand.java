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

    void getUsage(CommandSender sender);


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
        return checkPermission(sender, true);
    }

    default boolean checkPermission(@NotNull CommandSender sender, boolean feedback) {
        if (sender.isOp() && denyOp() && (sender instanceof Player)) {
            if (feedback) {
                sender.sendMessage(Component.text("Op players cannot use this command! ")
                        .append(Component.text("Please use "))
                        .append(Component.text("/deop " + sender.getName())
                                .clickEvent(ClickEvent.runCommand("/deop " + sender.getName()))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to deop yourself")))
                                .color(NamedTextColor.AQUA))
                        .append(Component.text(" to remove your op status."))
                        .color(NamedTextColor.RED));
            }
            return false;
        }
        if (sender instanceof ConsoleCommandSender && denyNonPlayer()) {
            if (feedback) sender.sendMessage(Component.text("Console cannot use this command!", NamedTextColor.RED));
            return false;
        }
        if (sender instanceof Player && denyPlayer()) {
            if (feedback) sender.sendMessage(Component.text("Players cannot use this command!", NamedTextColor.RED));
            return false;
        }

        if (getPermission() == null) {
            return true;
        }
        if (sender.hasPermission(getPermission())) {
            return true;
        } else {
            if (feedback)
                sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
            return false;
        }
    }

}
