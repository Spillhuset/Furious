package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ListCommand implements SubCommandInterface {
    private final Furious plugin;

    public ListCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // /guild list has no further arguments
        return List.of();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!can(sender, true)) return true;
        // Expected: /guild list
        if (args.length != 1) {
            Components.sendInfoMessage(sender, "Usage: /guild list");
            return true;
        }
        List<String> names = new ArrayList<>(plugin.guildService.getAllGuildNames());
        if (names.isEmpty()) {
            Components.sendInfoMessage(sender, "No guilds found.");
            return true;
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        // Header with total count
        Components.sendInfo(sender, Components.t("Guilds total: "), Components.valueComp(String.valueOf(names.size())));
        for (String name : names) {
            Guild g = plugin.guildService.getGuildByName(name);
            if (g == null) continue;
            int members = g.getMembers().size();
            boolean open = g.isOpen();
            Components.sendInfo(
                    sender,
                    Components.t("Guild "), Components.valueComp(name),
                    Components.t(" | members: "), Components.valueComp(String.valueOf(members)),
                    Components.t(" | open: "), Components.t(open ? "YES" : "NO", open ? NamedTextColor.GREEN : NamedTextColor.RED)
            );
        }
        return true;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getPermission() {
        return "furious.guild.list";
    }
}
