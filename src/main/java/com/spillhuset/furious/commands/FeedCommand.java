package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.StandaloneCommand;
import com.spillhuset.furious.utils.HelpMenuFormatter;
import com.spillhuset.furious.utils.InputSanitizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FeedCommand extends StandaloneCommand {

    public FeedCommand(Furious furious) {
        super(furious);
    }

    @Override
    public String getName() {
        return "feed";
    }

    @Override
    public String getDescription() {
        return "Feed yourself or other players";
    }

    @Override
    public void getUsage(CommandSender sender) {
        HelpMenuFormatter.showPlayerCommandsHeader(sender, "Feed");
        HelpMenuFormatter.formatPlayerCommand(sender, "/feed", "Feed yourself to full hunger");

        if (sender.hasPermission("furious.feed.others") || sender.isOp()) {
            HelpMenuFormatter.showAdminCommandsHeader(sender, "Feed");
            HelpMenuFormatter.formatAdminSubCommandWithParams(sender, "/feed", "", "<player>", "", "Feed another player to full hunger");
            HelpMenuFormatter.formatAdminSubCommandWithParams(sender, "/feed", "", "<selector>", "", "Feed multiple players using a selector (@a, @p, @r, @s)");
        }
    }

    @Override
    public String getPermission() {
        return "furious.feed.self";
    }

    /**
     * Checks if the sender has permission to feed other players.
     *
     * @param sender The command sender
     * @return true if the sender has permission, false otherwise
     */
    private boolean checkFeedOthersPermission(CommandSender sender) {
        if (sender.hasPermission("furious.feed.others")) {
            return true;
        } else {
            sender.sendMessage(Component.text("You don't have permission to feed other players!", NamedTextColor.RED));
            return false;
        }
    }

    @Override
    protected boolean executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        int maxFood = 20;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }

            player.setFoodLevel(maxFood);
            player.sendMessage(Component.text("You have been fed to full stomach!", NamedTextColor.GREEN));
            return true;
        }

        if (args.length >= 1 && checkFeedOthersPermission(sender)) {
            StringBuilder found = new StringBuilder();
            StringBuilder notFound = new StringBuilder();
            StringBuilder invalid = new StringBuilder();

            for (String arg : args) {
                // Check if the input is safe
                if (!InputSanitizer.isSafeInput(arg)) {
                    invalid.append(arg).append(", ");
                    continue;
                }

                List<Player> targets = new ArrayList<>();

                // Handle selectors
                if (InputSanitizer.isValidSelector(arg)) {
                    if (arg.equals("@a")) {
                        // Get all online players
                        targets.addAll(plugin.getServer().getOnlinePlayers());
                    } else if (arg.equals("@p") && sender instanceof Player senderPlayer) {
                        // Get nearest player to sender
                        Player nearestPlayer = getNearestPlayer(senderPlayer);
                        if (nearestPlayer != null) {
                            targets.add(nearestPlayer);
                        }
                    } else if (arg.equals("@r")) {
                        // Get a random player
                        List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
                        if (!onlinePlayers.isEmpty()) {
                            targets.add(onlinePlayers.get(new Random().nextInt(onlinePlayers.size())));
                        }
                    } else if (arg.equals("@s") && sender instanceof Player senderPlayer) {
                        // Get the sender
                        targets.add(senderPlayer);
                    }
                } else {
                    // Regular player name - sanitize it
                    String sanitizedName = InputSanitizer.sanitizePlayerName(arg);
                    if (sanitizedName == null) {
                        invalid.append(arg).append(", ");
                        continue;
                    }

                    // Try to get the player
                    Player target = plugin.getServer().getPlayer(sanitizedName);
                    if (target != null) {
                        targets.add(target);
                    } else {
                        notFound.append(sanitizedName).append(", ");
                    }
                }

                // Feed all found targets
                for (Player target : targets) {
                    found.append(target.getName()).append(", ");
                    target.setFoodLevel(maxFood);
                    target.sendMessage(Component.text("You have been fed to full stomach!", NamedTextColor.GREEN));
                }
            }
            // Handle invalid inputs
            if (!invalid.isEmpty()) {
                invalid.delete(invalid.lastIndexOf(", "), invalid.length());
                sender.sendMessage(Component.text("Invalid player name(s) or selector(s): " + invalid, NamedTextColor.RED));
                return true;
            }

            // Handle not found players
            if (!notFound.isEmpty()) {
                notFound.delete(notFound.lastIndexOf(", "), notFound.length());
                sender.sendMessage(Component.text("Could not find player(s): " + notFound, NamedTextColor.RED));
                return true;
            }

            // Handle found players
            if (!found.isEmpty()) {
                found.delete(found.lastIndexOf(", "), found.length());
                sender.sendMessage(Component.text("Feed player(s): " + found, NamedTextColor.GREEN));
            }

            // Handle no players found
            if (found.isEmpty() && notFound.isEmpty() && invalid.isEmpty()) {
                sender.sendMessage(Component.text("No players found!", NamedTextColor.RED));
            }
        }
        return true;
    }

    /**
     * Gets the nearest player to the given player
     * @param player The player to find the nearest player to
     * @return The nearest player, or null if no other players are online
     */
    private Player getNearestPlayer(Player player) {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            // Skip the player themselves
            if (onlinePlayer.equals(player)) {
                continue;
            }

            // Skip players in different worlds
            if (!onlinePlayer.getWorld().equals(player.getWorld())) {
                continue;
            }

            double distance = player.getLocation().distance(onlinePlayer.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = onlinePlayer;
            }
        }

        return nearestPlayer;
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length >= 1) {
            String partial = args[args.length - 1].toLowerCase();
            List<String> alreadyAdded = new ArrayList<>();

            // Add all arguments except the current one to the already added list
            if (args.length > 1) {
                for (int i = 0; i < args.length - 1; i++) {
                    alreadyAdded.add(args[i].toLowerCase());
                }
            }

            // Add valid selectors if they match the partial input
            for (String selector : InputSanitizer.VALID_SELECTORS) {
                if (selector.startsWith(partial) && !alreadyAdded.contains(selector)) {
                    completions.add(selector);
                }
            }

            // Add matching player names that haven't been added yet
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();
                if (playerName.toLowerCase().startsWith(partial) &&
                    !alreadyAdded.contains(playerName.toLowerCase())) {
                    completions.add(playerName);
                }
            }
        }

        return completions.isEmpty() ? Collections.emptyList() : completions;
    }
}
