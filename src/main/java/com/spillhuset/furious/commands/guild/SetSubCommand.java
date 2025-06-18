package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.GuildSubCommand;
import com.spillhuset.furious.utils.InputSanitizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Subcommand for setting guild properties.
 */
public class SetSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new SetSubCommand.
     *
     * @param plugin The plugin instance
     */
    public SetSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getDescription() {
        return "Sets guild properties.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild set open <true|false>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Sets whether the guild is open for anyone to join without invitation.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild set mobs <allow|deny>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Sets whether mobs can spawn in guild claimed chunks.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkGuildPermission(sender)) {
            return true;
        }

        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        Player player = (Player) sender;
        Guild guild = isInGuild(player);

        String property = args[1].toLowerCase();
        String value = args[2].toLowerCase();

        switch (property) {
            case "open":
                return handleOpenSetting(player, guild, value);
            case "mobs":
                return handleMobsSetting(player, guild, value);
            default:
                sender.sendMessage(Component.text("Unknown property: " + property, NamedTextColor.RED));
                getUsage(sender);
                return true;
        }
    }

    /**
     * Handles the "open" property setting.
     *
     * @param player The player executing the command
     * @param guild The player's guild
     * @param value The value to set (true/false)
     * @return true if the command was handled, false otherwise
     */
    private boolean handleOpenSetting(Player player, Guild guild, String value) {
        boolean openValue;

        if (value.equals("true")) {
            openValue = true;
        } else if (value.equals("false")) {
            openValue = false;
        } else {
            player.sendMessage(Component.text("Invalid value for open: " + value + ". Use 'true' or 'false'.", NamedTextColor.RED));
            return true;
        }

        plugin.getGuildManager().setGuildOpen(guild, openValue, player);
        return true;
    }

    /**
     * Handles the "mobs" property setting.
     *
     * @param player The player executing the command
     * @param guild The player's guild
     * @param value The value to set (allow/deny)
     * @return true if the command was handled, false otherwise
     */
    private boolean handleMobsSetting(Player player, Guild guild, String value) {
        boolean mobsValue;

        if (value.equals("allow")) {
            mobsValue = true;
        } else if (value.equals("deny")) {
            mobsValue = false;
        } else {
            player.sendMessage(Component.text("Invalid value for mobs: " + value + ". Use 'allow' or 'deny'.", NamedTextColor.RED));
            return true;
        }

        plugin.getGuildManager().setGuildMobSpawning(guild, mobsValue, player);
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            return Arrays.asList("open", "mobs");
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("open")) {
                return Arrays.asList("true", "false");
            } else if (args[1].equalsIgnoreCase("mobs")) {
                return Arrays.asList("allow", "deny");
            }
        }

        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.set";
    }

    @Override
    public GuildRole getRequiredRole() {
        // This command requires the player to be the guild owner
        return GuildRole.OWNER;
    }
}
