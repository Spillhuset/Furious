package com.spillhuset.furious.utils;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommandInterface extends CommandInterface {
    List<String> tabComplete(CommandSender sender, String[] args);
    boolean execute(CommandSender sender, String[] args);

    /**
     * Checks if the sender can use the command. Sends feedback to sender if needed. Not used on others.
     * @param sender The sender to check.
     * @return True if the sender can use the command. False otherwise.
     */
    default boolean can(CommandSender sender) {
        return can(sender,true);
    }

    /**
     * Checks if the sender can use the command. Optional to send feedback to sender if needed. Not used on others.
     * @param sender The sender to check.
     * @return True if the sender can use the command. False otherwise.
     */
    default boolean can(CommandSender sender, boolean feedback) {
        return can(sender,feedback,false);
    }

    /**
     * Checks if the sender can use the command. Optional to send feedback to sender if needed. Optional to check others.
     * @param sender The sender to check.
     * @return True if the sender can use the command. False otherwise.
     */
    default boolean can(CommandSender sender, boolean feedback,boolean others) {
        if (others && !sender.hasPermission(getPermission() + ".others")) {
            if (feedback) {
                Components.sendErrorMessage(sender, "You don't have permission to view others.");
            }
            return false;
        }
        if (!sender.hasPermission(getPermission())) {
            if (feedback) {
                Components.sendErrorMessage(sender, "You don't have permission the correct permission.");
            }
            return false;
        }
        return true;
    }
}
