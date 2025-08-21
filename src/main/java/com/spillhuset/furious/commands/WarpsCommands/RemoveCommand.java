package com.spillhuset.furious.commands.WarpsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.*;

public class RemoveCommand implements SubCommandInterface {
    private final Furious plugin;
    private final Map<UUID,String> pending = new HashMap<>();

    public RemoveCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (String name : plugin.warpsService.getWarpNames()) {
                if (name.startsWith(prefix)) out.add(name);
            }
        } else if (args.length == 3) {
            if ("confirm".startsWith(args[2].toLowerCase())) out.add("confirm");
        }
        return out;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            Components.sendErrorMessage(sender, "Only operators can remove warps.");
            return true;
        }
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /warps remove <name> [confirm]");
            return true;
        }
        String name = args[1];
        boolean confirmed = args.length >= 3 && args[2].equalsIgnoreCase("confirm");
        UUID token = UUID.randomUUID();
        String key = (sender.getName() == null ? "console" : sender.getName()) + ":" + name;
        if (!confirmed) {
            pending.put(token, key);
            Components.sendInfo(sender, Components.t("Are you sure you want to remove warp "), Components.valueComp(name), Components.t("? Type /warps remove "), Components.valueComp(name), Components.t(" confirm"));
            return true;
        }
        // Look up any matching token for this sender+name pair
        boolean valid = pending.values().stream().anyMatch(s -> s.equals(key));
        if (!valid) {
            Components.sendErrorMessage(sender, "Invalid or expired confirmation. Run the command again to confirm.");
            return true;
        }
        plugin.warpsService.removeWarp(sender, name);
        // Clean matching entries
        pending.entrySet().removeIf(e -> Objects.equals(e.getValue(), key));
        return true;
    }

    @Override
    public String getName() { return "remove"; }

    @Override
    public String getPermission() { return "furious.warps.remove"; }
}
