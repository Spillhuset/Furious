package com.spillhuset.furious.commands.ProfessionCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.ProfessionService;
import com.spillhuset.furious.services.ProfessionService.Profession;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class ShowCommand implements SubCommandInterface {
    private final Furious plugin;
    public ShowCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() != null && op.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    names.add(op.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length < 2) {
            Components.sendErrorMessage(sender, "Console must specify a player: /profession show <player>");
            return true;
        }
        UUID target;
        String name;
        if (args.length >= 2) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            if (op == null || (op.getName() == null && !op.hasPlayedBefore())) {
                Components.sendErrorMessage(sender, "Unknown player: "+args[1]);
                return true;
            }
            target = op.getUniqueId();
            name = op.getName() != null ? op.getName() : target.toString();
        } else {
            target = ((Player)sender).getUniqueId();
            name = ((Player)sender).getName();
        }
        ProfessionService svc = plugin.professionService;
        if (svc == null) {
            Components.sendErrorMessage(sender, "Professions not available.");
            return true;
        }
        Profession prim = svc.getPrimary(target);
        Profession sec = svc.getSecondary(target);
        Component msg = Component.text("Professions for ", NamedTextColor.YELLOW)
                .append(Component.text(name, NamedTextColor.GOLD))
                .append(Component.text(": ", NamedTextColor.YELLOW))
                .append(Component.text("Primary=", NamedTextColor.GRAY))
                .append(Component.text(prim != null ? prim.name() : "None", NamedTextColor.AQUA))
                .append(Component.text(", Secondary=", NamedTextColor.GRAY))
                .append(Component.text(sec != null ? sec.name() : "None", NamedTextColor.AQUA));
        sender.sendMessage(msg);

        // Show simple points summary
        StringBuilder sb = new StringBuilder("Points — ");
        for (Profession p : Profession.values()) {
            sb.append(p.name()).append(": ").append(svc.getPoints(target, p)).append("  ");
        }
        sender.sendMessage(Component.text(sb.toString(), NamedTextColor.DARK_AQUA));
        return true;
    }

    @Override
    public String getName() { return "show"; }

    @Override
    public String getPermission() { return "furious.profession"; }
}
