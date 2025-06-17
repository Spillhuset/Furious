package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
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

    public CoordsSubCommand(Furious plugin) {
        this.plugin = plugin;
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
        if (isNumeric(args[1])) {
            // No player specified, use sender
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Console must specify a player!", NamedTextColor.RED));
                return true;
            }
            target = (Player) sender;
            coordsArgs = args;
        } else {
            // Player name specified
            target = Bukkit.getPlayer(args[1]);
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

        // Execute teleport
        return plugin.getTeleportManager().teleportCoords(target, sender, coordsArgs);
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
        if (args.length == 2 && !isNumeric(args[1])) {
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
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
