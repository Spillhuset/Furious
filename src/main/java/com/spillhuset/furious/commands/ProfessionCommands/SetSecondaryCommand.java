package com.spillhuset.furious.commands.ProfessionCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.ProfessionService;
import com.spillhuset.furious.services.ProfessionService.Profession;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SetSecondaryCommand implements SubCommandInterface {
    private final Furious plugin;
    public SetSecondaryCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            String start = args[1].toLowerCase(Locale.ROOT);
            Arrays.stream(Profession.values()).map(Enum::name).forEach(n -> { if (n.toLowerCase(Locale.ROOT).startsWith(start)) list.add(n); });
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Components.sendErrorMessage(sender, "Only players can set professions.");
            return true;
        }
        if (args.length < 2) {
            Components.sendErrorMessage(sender, "Usage: /profession setsecondary <Miner|Lumberjack|Farmer|Fisher|Butcher>");
            return true;
        }
        Profession prof;
        try { prof = Profession.valueOf(args[1].toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ex) {
            Components.sendErrorMessage(sender, "Unknown profession.");
            return true;
        }
        UUID id = ((Player)sender).getUniqueId();
        ProfessionService svc = plugin.professionService;
        if (svc == null) {
            Components.sendErrorMessage(sender, "Professions not available.");
            return true;
        }
        svc.setSecondary(id, prof);
        sender.sendMessage(Component.text("Secondary profession set to "+prof.name(), NamedTextColor.GREEN));
        return true;
    }

    @Override
    public String getName() { return "setsecondary"; }

    @Override
    public String getPermission() { return "furious.profession"; }
}
