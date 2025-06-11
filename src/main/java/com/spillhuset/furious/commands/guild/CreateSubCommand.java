package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for creating a new guild.
 */
public class CreateSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new CreateSubCommand.
     *
     * @param plugin The plugin instance
     */
    public CreateSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Creates a new guild.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild create <name>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Creates a new guild with the specified name.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 2) {
            getUsage(sender);
            return true;
        }

        String guildName = args[1];

        // Create the guild
        Guild guild = plugin.getGuildManager().createGuild(guildName, player);

        if (guild != null) {
            player.sendMessage(Component.text("Guild " + guild.getName() + " created successfully!", NamedTextColor.GREEN));
            return true;
        }

        // If we get here, guild creation failed (error message already sent by GuildManager)
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completion for guild creation
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.create";
    }
}