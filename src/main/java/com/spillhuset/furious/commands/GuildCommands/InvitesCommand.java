package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class InvitesCommand implements SubCommandInterface {
    private final Furious plugin;

    public InvitesCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // No arguments expected
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can list their invites.");
            return true;
        }
        if (!can(sender, true)) return true;

        UUID self = player.getUniqueId();
        java.util.List<String> names = plugin.guildService.getInvitingGuildNamesFor(self);
        if (names.isEmpty()) {
            Components.sendInfoMessage(sender, "You have no pending guild invites.");
        } else {
            Components.sendSuccess(sender,
                    Components.t("Pending invites from: "),
                    Components.t(String.join(", ", names))
            );
        }
        return true;
    }

    @Override
    public String getName() {
        return "invites";
    }

    @Override
    public String getPermission() {
        return "furious.guild.invites";
    }
}
