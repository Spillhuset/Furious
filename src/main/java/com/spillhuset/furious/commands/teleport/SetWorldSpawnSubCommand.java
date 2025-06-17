package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for setting a world's spawn location
 */
public class SetWorldSpawnSubCommand implements SubCommand {
    private final Furious plugin;

    public SetWorldSpawnSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "setworldspawn";
    }

    @Override
    public String getDescription() {
        return "Sets the spawn location of a world to your current position";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/teleport setworldspawn [world]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Sets the spawn location of the specified world to your current position.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If no world is specified, your current world will be used.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        World world;
        if (args.length > 1) {
            world = Bukkit.getWorld(args[1]);
            if (world == null) {
                player.sendMessage(Component.text("World not found!", NamedTextColor.RED));
                return true;
            }
        } else {
            world = player.getWorld();
        }

        return plugin.getWorldManager().setWorldSpawn(world, player);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
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
        return "furious.teleport.setworldspawn";
    }
}