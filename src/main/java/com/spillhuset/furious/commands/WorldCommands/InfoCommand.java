package com.spillhuset.furious.commands.WorldCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InfoCommand implements SubCommandInterface {
    private final Furious plugin;

    public InfoCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        // /world info <world>
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (World w : Bukkit.getWorlds()) {
                String name = w.getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(name);
            }
        }
        return out;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /world info <world>");
            return true;
        }
        String worldName = args[1];
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            Components.sendErrorMessage(sender, "World not found or not loaded: " + worldName);
            return true;
        }

        // Collect basic info
        String env = w.getEnvironment().name();
        long seed = w.getSeed();
        String type;
        // WorldType is deprecated; approximate using Environment and known characteristics
        World.Environment envEnum = w.getEnvironment();
        switch (envEnum) {
            case NETHER -> type = "NETHER";
            case THE_END -> type = "THE_END";
            default -> type = "NORMAL";
        }
        int spawnX = w.getSpawnLocation().getBlockX();
        int spawnY = w.getSpawnLocation().getBlockY();
        int spawnZ = w.getSpawnLocation().getBlockZ();
        int players = w.getPlayers().size();

        Components.sendInfo(sender,
                Components.t("World: "), Components.valueComp(w.getName()), Components.t(" | Env: "), Components.valueComp(env),
                Components.t(" | Type: "), Components.valueComp(type), Components.t(" | Seed: "), Components.valueComp(String.valueOf(seed)),
                Components.t(" | Spawn: "), Components.valueComp(spawnX+","+spawnY+","+spawnZ),
                Components.t(" | Players: "), Components.valueComp(String.valueOf(players))
        );
        return true;
    }

    @Override
    public String getName() { return "info"; }

    @Override
    public String getPermission() { return "furious.world.info"; }
}
