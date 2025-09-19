package com.spillhuset.furious.commands.WorldCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SetSpawnCommand implements SubCommandInterface {
    private final Furious plugin;

    public SetSpawnCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // /world setspawn has no additional args
        return new ArrayList<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            Components.sendErrorMessage(sender, "Only players can use this command.");
            return true;
        }
        World world = p.getWorld();
        Location loc = p.getLocation();
        try {
            world.setSpawnLocation(loc);
        } catch (NoSuchMethodError err) {
            world.setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
        Components.sendSuccess(sender,
                Components.t("World spawn set for "), Components.valueComp(world.getName()), Components.t(" at "),
                Components.valueComp(loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ()), Components.t("."));
        return true;
    }

    @Override
    public String getName() { return "setspawn"; }

    @Override
    public String getPermission() { return "furious.world.setspawn"; }
}
