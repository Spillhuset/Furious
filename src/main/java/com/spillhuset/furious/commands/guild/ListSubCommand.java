package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.GuildSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Subcommand for listing all guilds.
 */
public class ListSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new ListSubCommand.
     *
     * @param plugin The plugin instance
     */
    public ListSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Lists all guilds.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild list", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Lists all guilds.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkGuildPermission(sender)) {
            return true;
        }

        if (args.length != 1) {
            getUsage(sender);
            return true;
        }

        Collection<Guild> guilds = plugin.getGuildManager().getAllGuilds();

        if (guilds.isEmpty()) {
            sender.sendMessage(Component.text("There are no guilds yet!", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("=== Guilds (" + plugin.getGuildManager().getGuildCount() + ") ===", NamedTextColor.GOLD));

        for (Guild guild : guilds) {
            Component guildInfo = Component.text("- ", NamedTextColor.YELLOW)
                    .append(Component.text(guild.getName(), NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/guild info " + guild.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to view guild info"))))
                    .append(Component.text(" (" + guild.getMemberCount() + " members)", NamedTextColor.GRAY));

            if (!guild.getDescription().isEmpty()) {
                guildInfo = guildInfo.append(Component.text(" - " + guild.getDescription(), NamedTextColor.WHITE));
            }

            sender.sendMessage(guildInfo);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completion for listing guilds
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.list";
    }

    @Override
    public GuildRole getRequiredRole() {
        // This command doesn't require a guild role
        return null;
    }

    @Override
    public boolean checkGuildPermission(@NotNull CommandSender sender, boolean feedback) {
        // Only check regular permissions
        return checkPermission(sender, feedback);
    }
}
