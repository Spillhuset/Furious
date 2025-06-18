package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.StandaloneCommand;
import com.spillhuset.furious.utils.SecurityReviewManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Command for managing security reviews and other security-related tasks.
 */
public class SecurityCommand extends StandaloneCommand {
    private final SecurityReviewManager securityReviewManager;

    /**
     * Creates a new SecurityCommand.
     *
     * @param plugin The Furious plugin instance
     * @param securityReviewManager The security review manager
     */
    public SecurityCommand(Furious plugin, SecurityReviewManager securityReviewManager) {
        super(plugin);
        this.securityReviewManager = securityReviewManager;
    }

    @Override
    protected boolean executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            // Display help message
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "status":
                return handleStatusCommand(sender);
            case "review":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /security review <complete|interval> [args]");
                    return true;
                }
                String reviewAction = args[1].toLowerCase();
                if (reviewAction.equals("complete")) {
                    return handleReviewCompleteCommand(sender, args);
                } else if (reviewAction.equals("interval")) {
                    return handleReviewIntervalCommand(sender, args);
                } else {
                    sender.sendMessage("§cUnknown review action: " + reviewAction);
                    sender.sendMessage("§cUsage: /security review <complete|interval> [args]");
                    return true;
                }
            case "help":
                showHelp(sender);
                return true;
            default:
                sender.sendMessage("§cUnknown subcommand: " + subCommand);
                showHelp(sender);
                return true;
        }
    }

    /**
     * Handles the /security status command.
     *
     * @param sender The command sender
     * @return true if the command was handled successfully
     */
    private boolean handleStatusCommand(CommandSender sender) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date lastReview = securityReviewManager.getLastReviewDate();
        Date nextReview = securityReviewManager.getNextReviewDate();
        int intervalDays = securityReviewManager.getReviewIntervalDays();

        sender.sendMessage("§e===== Security Review Status =====");
        sender.sendMessage("§fLast review: §e" + sdf.format(lastReview));
        sender.sendMessage("§fNext review: §e" + sdf.format(nextReview));
        sender.sendMessage("§fReview interval: §e" + intervalDays + " days");

        // Check if a review is due
        Date now = new Date();
        if (now.after(nextReview)) {
            sender.sendMessage("§c§lA security review is due!");
            sender.sendMessage("§fUse §e/security review complete§f to mark it as completed.");
        } else {
            // Calculate days until next review
            long diffInMillies = nextReview.getTime() - now.getTime();
            long diffInDays = diffInMillies / (1000 * 60 * 60 * 24);
            sender.sendMessage("§fDays until next review: §e" + diffInDays);
        }

        return true;
    }

    /**
     * Handles the /security review complete command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled successfully
     */
    private boolean handleReviewCompleteCommand(CommandSender sender, String[] args) {
        String reviewer = sender instanceof Player ? sender.getName() : "Console";

        // Get notes if provided
        String notes = "";
        if (args.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            notes = sb.toString().trim();
        }

        boolean success = securityReviewManager.completeSecurityReview(reviewer, notes);

        if (success) {
            sender.sendMessage("§aThe security review has been marked as completed.");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date nextReview = securityReviewManager.getNextReviewDate();
            sender.sendMessage("§fNext review is scheduled for: §e" + sdf.format(nextReview));
        } else {
            sender.sendMessage("§cAn error occurred while marking the security review as completed.");
        }

        return true;
    }

    /**
     * Handles the /security review interval command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled successfully
     */
    private boolean handleReviewIntervalCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /security review interval <days>");
            return true;
        }

        try {
            int days = Integer.parseInt(args[2]);
            if (days < 1) {
                sender.sendMessage("§cThe review interval must be at least 1 day.");
                return true;
            }

            securityReviewManager.setReviewIntervalDays(days);
            sender.sendMessage("§aThe security review interval has been set to §e" + days + " days§a.");

            // Show the next review date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date nextReview = securityReviewManager.getNextReviewDate();
            sender.sendMessage("§fNext review is scheduled for: §e" + sdf.format(nextReview));
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number format: " + args[2]);
            sender.sendMessage("§cUsage: /security review interval <days>");
        }

        return true;
    }

    /**
     * Shows the help message for the security command.
     *
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage("§e===== Security Command Help =====");
        sender.sendMessage("§f/security status §7- §fShow the status of security reviews");
        sender.sendMessage("§f/security review complete [notes] §7- §fMark a security review as completed");
        sender.sendMessage("§f/security review interval <days> §7- §fSet the interval between security reviews");
        sender.sendMessage("§f/security help §7- §fShow this help message");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return tabComplete(sender, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("furious.security.admin")) {
            return completions;
        }

        if (args.length == 1) {
            // First argument - subcommands
            String[] subCommands = {"status", "review", "help"};
            return getMatchingCompletions(args[0], Arrays.asList(subCommands));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("review")) {
            // Second argument for "review" subcommand
            String[] reviewActions = {"complete", "interval"};
            return getMatchingCompletions(args[1], Arrays.asList(reviewActions));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("review") && args[1].equalsIgnoreCase("interval")) {
            // Third argument for "review interval" subcommand - suggest current interval
            completions.add(String.valueOf(securityReviewManager.getReviewIntervalDays()));
            return completions;
        }

        return completions;
    }

    /**
     * Gets a list of tab completions that match the current argument.
     *
     * @param arg The current argument
     * @param options The list of available options
     * @return A list of matching completions
     */
    private List<String> getMatchingCompletions(String arg, List<String> options) {
        List<String> completions = new ArrayList<>();

        for (String option : options) {
            if (option.toLowerCase().startsWith(arg.toLowerCase())) {
                completions.add(option);
            }
        }

        return completions;
    }

    @Override
    public void getUsage(CommandSender sender) {
        showHelp(sender);
    }

    @Override
    public String getName() {
        return "security";
    }

    @Override
    public String getDescription() {
        return "Manage security reviews and other security-related tasks";
    }

    @Override
    public String getPermission() {
        return "furious.security.admin";
    }
}
