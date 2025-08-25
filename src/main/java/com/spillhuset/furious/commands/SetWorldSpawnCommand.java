package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.Components;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SetWorldSpawnCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;

    public SetWorldSpawnCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;
        if (!(sender instanceof Player p)) {
            Components.sendErrorMessage(sender, "Only players can use this command.");
            return true;
        }
        World world = p.getWorld();
        Location loc = p.getLocation();
        // Use modern API if available
        try {
            world.setSpawnLocation(loc);
        } catch (NoSuchMethodError err) {
            // Fallback to integer block coords
            world.setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
        Components.sendSuccess(sender,
                Components.t("World spawn set for "), Components.valueComp(world.getName()), Components.t(" at "),
                Components.valueComp(loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ()), Components.t("."));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getName() { return "setworldspawn"; }

    @Override
    public String getPermission() { return "furious.worldspawn.set"; }
}
