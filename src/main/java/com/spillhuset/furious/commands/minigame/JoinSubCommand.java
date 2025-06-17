package com.spillhuset.furious.commands.minigame;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for joining a minigame queue
 */
public class JoinSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for JoinSubCommand
     *
     * @param plugin The plugin instance
     */
    public JoinSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Join a minigame queue";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /minigame join <game>", NamedTextColor.YELLOW));
    }

    @Override
    public boolean denyNonPlayer() {
        return true;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String gameName = args[1].toLowerCase();

        // Check if the game exists
        if (!plugin.getMinigameManager().getGameNames().contains(gameName)) {
            player.sendMessage(Component.text("That minigame doesn't exist! Available games: " +
                    String.join(", ", plugin.getMinigameManager().getGameNames()), NamedTextColor.RED));
            return true;
        }

        // Join the queue
        plugin.getMinigameManager().joinQueue(player, gameName);

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (String gameName : plugin.getMinigameManager().getGameNames()) {
                if (gameName.startsWith(partial)) {
                    completions.add(gameName);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.minigame.join";
    }
}