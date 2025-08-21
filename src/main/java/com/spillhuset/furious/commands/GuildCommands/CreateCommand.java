package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.GuildType;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class CreateCommand implements SubCommandInterface {
    private final Furious plugin;
    public CreateCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // /guild create <name> [SAFE|WAR|FREE] (admin only type)
        if (args.length == 2) {
            return List.of();
        } else if (args.length == 3) {
            if (sender.hasPermission("furious.guild.create.admin")) {
                String partial = args[2].toUpperCase();
                return Arrays.stream(new GuildType[]{GuildType.SAFE, GuildType.WAR, GuildType.FREE})
                        .map(Enum::name)
                        .filter(n -> n.startsWith(partial))
                        .toList();
            }
        }
        return List.of();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can create a guild.");
            return true;
        }

        // Admin unmanned creation with explicit type
        if (sender.hasPermission("furious.guild.create.admin") && args.length >= 3) {
            String name = args[1];
            GuildType type;
            try {
                type = GuildType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException ex) {
                Components.sendErrorMessage(sender, "Unknown guild type. Use SAFE, WAR, or FREE (admin only).");
                return true;
            }
            if (type == GuildType.SAFE || type == GuildType.WAR || type == GuildType.FREE) {
                boolean created = plugin.guildService.createGuild(null, name, type);
                if (created) {
                    Components.sendSuccessMessage(sender, "Unmanned guild created: " + name + " (" + type.name() + ")");
                } else {
                    Components.sendErrorMessage(sender, "Failed to create guild. Name may be taken.");
                }
                return true;
            } else {
                // For any other type requested by admin, fall back to FREE unmanned
                boolean created = plugin.guildService.createGuild(null, args[1], GuildType.FREE);
                if (created) {
                    Components.sendSuccessMessage(sender, "Unmanned guild created as FREE: " + args[1]);
                } else {
                    Components.sendErrorMessage(sender, "Failed to create guild. Name may be taken.");
                }
                return true;
            }
        }

        // Regular user or admin without specifying type: always OWNED
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /guild create <name>" + (sender.hasPermission("furious.guild.create.admin") ? " [SAFE|WAR|FREE]" : ""));
            return true;
        }
        String name = args[1];
        boolean created = plugin.guildService.createGuild(player.getUniqueId(), name, GuildType.OWNED);
        if (created) {
            Components.sendSuccessMessage(sender, "Guild created: " + name + " (OWNED)");
        } else {
            Components.sendErrorMessage(sender, "Failed to create guild. Name may be taken or you may already be in a guild.");
        }
        return true;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getPermission() {
        return "furious.guild.create";
    }
}
