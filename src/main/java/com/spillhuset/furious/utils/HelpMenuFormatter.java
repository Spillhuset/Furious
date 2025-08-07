package com.spillhuset.furious.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/**
 * Utility class for formatting help menus consistently across commands.
 */
public class HelpMenuFormatter {

    /**
     * Displays a header for player commands.
     *
     * @param sender The command sender
     * @param commandName The name of the command
     */
    public static void showPlayerCommandsHeader(CommandSender sender, String commandName) {
        sender.sendMessage(Component.text(commandName + " player commands:", NamedTextColor.GOLD));
    }

    /**
     * Displays a header for the command.
     *
     * @param sender The command sender
     * @param commandName The name of the command
     */
    public static void showCommandsHeader(CommandSender sender, String commandName) {
        sender.sendMessage(Component.text(commandName + " commands:", NamedTextColor.GOLD));
    }

    /**
     * Displays a header for admin commands.
     *
     * @param sender The command sender
     * @param commandName The name of the command
     */
    public static void showAdminCommandsHeader(CommandSender sender, String commandName) {
        sender.sendMessage(Component.text(commandName + " admin commands:", NamedTextColor.GOLD));
    }

    /**
     * Formats and displays a basic player command without subcommands.
     *
     * @param sender The command sender
     * @param command The command (e.g., "/wallet")
     * @param description The command description
     */
    public static void formatPlayerCommand(CommandSender sender, String command, String description) {
        sender.sendMessage(Component.text(command + " - " + description, NamedTextColor.YELLOW));
    }

    /**
     * Formats and displays a player command with a subcommand.
     *
     * @param sender The command sender
     * @param command The base command (e.g., "/wallet")
     * @param subCommand The subcommand (e.g., "pay")
     * @param description The command description
     */
    public static void formatPlayerSubCommand(CommandSender sender, String command, String subCommand, String description) {
        sender.sendMessage(Component.text(command + " ", NamedTextColor.YELLOW)
                .append(Component.text(subCommand, NamedTextColor.AQUA))
                .append(Component.text(" - " + description, NamedTextColor.YELLOW)));
    }

    /**
     * Formats and displays a player command with a subcommand and parameters.
     *
     * @param sender The command sender
     * @param command The base command (e.g., "/wallet")
     * @param subCommand The subcommand (e.g., "pay")
     * @param requiredParams Required parameters (e.g., "<player>")
     * @param optionalParams Optional parameters (e.g., "[world]")
     * @param description The command description
     */
    public static void formatPlayerSubCommandWithParams(CommandSender sender, String command, String subCommand,
                                                      String requiredParams, String optionalParams, String description) {
        Component message = Component.text(command + " ", NamedTextColor.YELLOW)
                .append(Component.text(subCommand, NamedTextColor.AQUA));

        if (requiredParams != null && !requiredParams.isEmpty()) {
            message = message.append(Component.text(" " + requiredParams, NamedTextColor.GRAY));
        }

        if (optionalParams != null && !optionalParams.isEmpty()) {
            message = message.append(Component.text(" " + optionalParams, NamedTextColor.LIGHT_PURPLE));
        }

        message = message.append(Component.text(" - " + description, NamedTextColor.YELLOW));

        sender.sendMessage(message);
    }

    /**
     * Formats and displays a basic admin command without subcommands.
     *
     * @param sender The command sender
     * @param command The command (e.g., "/wallet")
     * @param description The command description
     */
    public static void formatAdminCommand(CommandSender sender, String command, String description) {
        sender.sendMessage(Component.text(command + " - " + description, NamedTextColor.GOLD));
    }

    /**
     * Formats and displays an admin command with a subcommand.
     *
     * @param sender The command sender
     * @param command The base command (e.g., "/wallet")
     * @param subCommand The subcommand (e.g., "set")
     * @param description The command description
     */
    public static void formatAdminSubCommand(CommandSender sender, String command, String subCommand, String description) {
        sender.sendMessage(Component.text(command + " ", NamedTextColor.GOLD)
                .append(Component.text(subCommand, NamedTextColor.AQUA))
                .append(Component.text(" - " + description, NamedTextColor.GOLD)));
    }

    /**
     * Formats and displays an admin command with a subcommand and parameters.
     *
     * @param sender The command sender
     * @param command The base command (e.g., "/wallet")
     * @param subCommand The subcommand (e.g., "set")
     * @param requiredParams Required parameters (e.g., "<player>")
     * @param optionalParams Optional parameters (e.g., "[world]")
     * @param description The command description
     */
    public static void formatAdminSubCommandWithParams(CommandSender sender, String command, String subCommand,
                                                     String requiredParams, String optionalParams, String description) {
        Component message = Component.text(command + " ", NamedTextColor.GOLD)
                .append(Component.text(subCommand, NamedTextColor.AQUA));

        if (requiredParams != null && !requiredParams.isEmpty()) {
            message = message.append(Component.text(" " + requiredParams, NamedTextColor.GRAY));
        }

        if (optionalParams != null && !optionalParams.isEmpty()) {
            message = message.append(Component.text(" " + optionalParams, NamedTextColor.LIGHT_PURPLE));
        }

        message = message.append(Component.text(" - " + description, NamedTextColor.GOLD));

        sender.sendMessage(message);
    }
}