package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import com.spillhuset.furious.utils.AuditLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CoordsSubCommand implements SubCommand {
    private final Furious plugin;
    private final AuditLogger auditLogger;

    public CoordsSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "coords";
    }

    @Override
    public String getDescription() {
        return "Teleport to a set of coordinates.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        if (!(sender instanceof ConsoleCommandSender)) sender.sendMessage(Component.text("/teleport coords <x> <y> <z> [world]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/teleport coords <player> <x> <y> <z> [world]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If no world is specified, the player's current world will be used.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If the player is not online, the teleport will be attempted when they log in.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If the player is not online and no world is specified, the teleport will be attempted when they log in in the player's current world.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If the player is not online and a world is specified, the teleport will be attempted when they log in in that world.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        // /teleport coords [player] <x> <y> <z> [world]

        // Check minimum args (coords + x y z)
        if (args.length < 4) {
            getUsage(sender);
            return true;
        }

        Player target;
        String[] coordsArgs;

        // Check if a player name is specified
        if (com.spillhuset.furious.utils.InputSanitizer.sanitizeCoordinate(args[1]) != null) {
            // No player specified, use sender
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Console must specify a player!", NamedTextColor.RED));
                return true;
            }
            target = (Player) sender;
            coordsArgs = args;
        } else {
            // Player name specified - sanitize it
            String targetName = com.spillhuset.furious.utils.InputSanitizer.sanitizePlayerName(args[1]);

            // Check if the player name is valid
            if (targetName == null) {
                sender.sendMessage(Component.text("Invalid player name! Please use a valid Minecraft username.", NamedTextColor.RED));
                return true;
            }

            // Check if the input is safe
            if (!com.spillhuset.furious.utils.InputSanitizer.isSafeInput(args[1])) {
                sender.sendMessage(Component.text("Invalid input detected! Please use only alphanumeric characters and underscores.", NamedTextColor.RED));
                return true;
            }

            target = Bukkit.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }

            // Check if we have enough args for coordinates
            if (args.length < 5) {
                getUsage(sender);
                return true;
            }

            // Shift args to align coordinates
            coordsArgs = new String[args.length - 1];
            coordsArgs[0] = args[0];  // "coords"
            System.arraycopy(args, 2, coordsArgs, 1, args.length - 2);
        }

        // Sanitize coordinates
        int coordIndex = (coordsArgs == args) ? 1 : 1; // Index of x coordinate

        // Validate x coordinate
        Double x = com.spillhuset.furious.utils.InputSanitizer.sanitizeCoordinate(coordsArgs[coordIndex]);
        if (x == null) {
            sender.sendMessage(Component.text("Invalid x coordinate! Please use a valid number.", NamedTextColor.RED));
            return true;
        }

        // Validate y coordinate
        Double y = com.spillhuset.furious.utils.InputSanitizer.sanitizeCoordinate(coordsArgs[coordIndex + 1]);
        if (y == null) {
            sender.sendMessage(Component.text("Invalid y coordinate! Please use a valid number.", NamedTextColor.RED));
            return true;
        }

        // Validate z coordinate
        Double z = com.spillhuset.furious.utils.InputSanitizer.sanitizeCoordinate(coordsArgs[coordIndex + 2]);
        if (z == null) {
            sender.sendMessage(Component.text("Invalid z coordinate! Please use a valid number.", NamedTextColor.RED));
            return true;
        }

        // Check if world name is provided
        if (coordsArgs.length > coordIndex + 3) {
            // Sanitize world name
            String worldName = com.spillhuset.furious.utils.InputSanitizer.sanitizeWorldName(coordsArgs[coordIndex + 3]);
            if (worldName == null) {
                sender.sendMessage(Component.text("Invalid world name! Please use a valid world name.", NamedTextColor.RED));
                return true;
            }

            // Update the coordsArgs with sanitized values
            coordsArgs[coordIndex + 3] = worldName;
        }

        // Update the coordsArgs with sanitized values
        coordsArgs[coordIndex] = String.valueOf(x);
        coordsArgs[coordIndex + 1] = String.valueOf(y);
        coordsArgs[coordIndex + 2] = String.valueOf(z);

        // Build destination string for logging
        String destination = x + ", " + y + ", " + z;
        if (coordsArgs.length > coordIndex + 3) {
            destination += " in world " + coordsArgs[coordIndex + 3];
        } else {
            destination += " in current world";
        }

        // Execute teleport
        boolean success = plugin.getTeleportManager().teleportCoords(target, sender, coordsArgs);

        // Log the teleport operation
        if (success) {
            auditLogger.logTeleportOperation(
                sender,
                target.getName(),
                destination,
                "Coordinates teleport command executed"
            );
        } else {
            auditLogger.logFailedAccess(
                sender,
                target.getName(),
                "teleport to coordinates " + destination,
                "Teleport failed"
            );
        }

        return success;
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // The first argument after "coords" could be player or coordinate
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            // Suggest player names if input is not numeric
            if (!isNumeric(partial)) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            }

            // Also suggest coordinates if the sender is a player
            if (sender instanceof Player player) {
                // Round to nearest integer for cleaner suggestions
                int x = (int) Math.round(player.getLocation().getX());
                String xStr = String.valueOf(x);
                if (partial.isEmpty() || xStr.startsWith(partial)) {
                    completions.add(xStr);
                }
            }
        }
        // X coordinate has been entered, suggest Y coordinate
        else if ((args.length == 3 && isNumeric(args[1])) ||  // /tp coords x y
                (args.length == 4 && !isNumeric(args[1]) && isNumeric(args[2]))) {  // /tp coords player x y
            if (sender instanceof Player player) {
                String partial = args[args.length - 1].toLowerCase();
                // Round to nearest integer for cleaner suggestions
                int y = (int) Math.round(player.getLocation().getY());
                String yStr = String.valueOf(y);
                if (partial.isEmpty() || yStr.startsWith(partial)) {
                    completions.add(yStr);
                }
            }
        }
        // Y coordinate has been entered, suggest Z coordinate
        else if ((args.length == 4 && isNumeric(args[1])) ||  // /tp coords x y z
                (args.length == 5 && !isNumeric(args[1]) && isNumeric(args[2]))) {  // /tp coords player x y z
            if (sender instanceof Player player) {
                String partial = args[args.length - 1].toLowerCase();
                // Round to nearest integer for cleaner suggestions
                int z = (int) Math.round(player.getLocation().getZ());
                String zStr = String.valueOf(z);
                if (partial.isEmpty() || zStr.startsWith(partial)) {
                    completions.add(zStr);
                }
            }
        }
        // World name completion
        else if ((args.length == 5 && isNumeric(args[1])) ||   // /tp coords x y z [world]
                (args.length == 6 && !isNumeric(args[1]))) {   // /tp coords player x y z [world]
            String partial = args[args.length - 1].toLowerCase();
            String gameBackupName = plugin.getWorldManager().getGameBackupName();

            for (World world : Bukkit.getWorlds()) {
                // Skip GameBackup world
                if (!world.getName().equals(gameBackupName) && world.getName().toLowerCase().startsWith(partial)) {
                    completions.add(world.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.teleport.coords";
    }
}
