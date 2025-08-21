package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildRole;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class InfoCommand implements SubCommandInterface {
    private final Furious plugin;

    public InfoCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (!can(sender, false)) return out;
        // /guild info [guildName]
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (String name : plugin.guildService.getAllGuildNames()) {
                if (name != null && name.toLowerCase().startsWith(prefix)) out.add(name);
            }
        }
        return out;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!can(sender, true)) return true;

        Guild targetGuild = null;

        if (args.length == 1) {
            // No extra args: show own guild (only for players)
            if (!(sender instanceof Player player)) {
                Components.sendInfoMessage(sender, "Usage: /guild info <guildName>");
                return true;
            }
            UUID gid = plugin.guildService.getGuildIdForMember(player.getUniqueId());
            if (gid == null) {
                Components.sendErrorMessage(sender, "You are not in a guild.");
                return true;
            }
            targetGuild = plugin.guildService.getGuildById(gid);
        } else {
            // args.length >= 2: guild by name
            String guildName = args[1];
            targetGuild = plugin.guildService.getGuildByName(guildName);
            if (targetGuild == null) {
                Components.sendErrorMessage(sender, "Guild not found: " + guildName);
                return true;
            }
        }

        showGuildInfo(sender, targetGuild);
        return true;
    }

    private void showGuildInfo(CommandSender sender, Guild g) {
        if (g == null) {
            Components.sendErrorMessage(sender, "No guild information available.");
            return;
        }
        String name = g.getName() != null ? g.getName() : "<unnamed>";
        String type = g.getType() != null ? g.getType().name() : "UNKNOWN";
        boolean open = g.isOpen();
        UUID ownerId = g.getOwner();
        String ownerName = ownerId != null ? Optional.ofNullable(plugin.getServer().getOfflinePlayer(ownerId).getName()).orElse(ownerId.toString()) : "None";

        // Header line
        Components.sendInfo(sender, Components.t("Guild: "), Components.valueComp(name));
        // Basic info lines
        Components.sendInfo(sender,
                Components.t("Type: "), Components.valueComp(type),
                Components.t("  Open: "), Components.t(open ? "YES" : "NO", open ? NamedTextColor.GREEN : NamedTextColor.RED)
        );
        Components.sendInfo(sender, Components.t("Owner: "), Components.valueComp(ownerName));

        // Members summary
        Map<UUID, GuildRole> members = g.getMembers();
        int total = members != null ? members.size() : 0;
        Components.sendInfo(sender, Components.t("Members: "), Components.valueComp(String.valueOf(total)));

        if (members != null && !members.isEmpty()) {
            // Group by role and list names for a quick view
            Map<GuildRole, List<String>> byRole = new EnumMap<>(GuildRole.class);
            for (Map.Entry<UUID, GuildRole> e : members.entrySet()) {
                GuildRole role = e.getValue();
                byRole.computeIfAbsent(role, r -> new ArrayList<>())
                        .add(Optional.ofNullable(plugin.getServer().getOfflinePlayer(e.getKey()).getName()).orElse(e.getKey().toString()));
            }
            // Sort names for stable output
            for (List<String> list : byRole.values()) list.sort(String.CASE_INSENSITIVE_ORDER);

            for (GuildRole role : GuildRole.values()) {
                List<String> list = byRole.get(role);
                if (list == null || list.isEmpty()) continue;
                Components.sendInfo(sender,
                        Components.t(" - " + role.name() + ": "),
                        Components.valueComp(String.join(", ", list))
                );
            }
        }
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getPermission() {
        return "furious.guild.info";
    }
}
