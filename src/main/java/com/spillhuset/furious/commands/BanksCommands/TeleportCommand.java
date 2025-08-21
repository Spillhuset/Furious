package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Teleport to a bank's claimed location.
 * Usage: /banks teleport <bankName> [player]
 */
public class TeleportCommand implements SubCommandInterface {
    private final Furious plugin;

    public TeleportCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            list.addAll(plugin.banksService.suggestBankNames(args[1]));
        } else if (args.length == 3) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) list.add(p.getName());
            }
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Components.sendErrorMessage(sender, "Usage: /banks teleport <bankName> [player]");
            return true;
        }
        String bankName = args[1];
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                Components.sendErrorMessage(sender, "Player not found.");
                return true;
            }
        } else {
            if (!(sender instanceof Player p)) {
                Components.sendErrorMessage(sender, "Only players can use this without specifying a target.");
                return true;
            }
            target = p;
        }
        plugin.banksService.teleportToBank(target, bankName);
        return true;
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String getPermission() {
        return "furious.banks.teleport";
    }
}
