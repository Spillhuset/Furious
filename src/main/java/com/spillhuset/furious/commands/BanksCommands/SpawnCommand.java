package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Bank;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * OP-only: Spawns an ArmorStand marker for a bank at the caller's location.
 */
public class SpawnCommand implements SubCommandInterface {
    private final Furious plugin;
    public SpawnCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) list.addAll(plugin.banksService.suggestBankNames(args[1]));
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use this.");
            return true;
        }
        if (args.length < 2) {
            Components.sendErrorMessage(sender, "Usage: /banks spawn <bankName>");
            return true;
        }
        String bankName = args[1];
        Bank bank = plugin.banksService.getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(sender, "Bank not found.");
            return true;
        }
        // Must be within the bank's claimed chunk
        if (!bank.isClaimed()) {
            Components.sendErrorMessage(sender, "This bank is not claimed; cannot set spawn.");
            return true;
        }
        if (player.getWorld() == null || !player.getWorld().getUID().equals(bank.getWorldId())) {
            Components.sendErrorMessage(sender, "You must stand inside the bank's claimed chunk (wrong world).");
            return true;
        }
        org.bukkit.Chunk pc = player.getLocation().getChunk();
        if (pc.getX() != bank.getChunkX() || pc.getZ() != bank.getChunkZ()) {
            Components.sendErrorMessage(sender, "You must stand inside the bank's claimed chunk to set spawn.");
            return true;
        }
        // Remove any previous armor stand to prevent duplicates
        plugin.banksService.removeArmorStandForBank(bank);
        boolean ok = plugin.banksService.spawnArmorStandForBank(bank, player.getLocation());
        if (ok) {
            Components.sendSuccess(sender, Components.t("Spawned ArmorStand for bank "), Components.valueComp(bank.getName()));
        }
        return true;
    }

    @Override
    public String getName() { return "spawn"; }

    @Override
    public String getPermission() { return "furious.banks.spawn"; }
}
