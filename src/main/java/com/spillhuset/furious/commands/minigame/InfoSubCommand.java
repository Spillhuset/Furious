package com.spillhuset.furious.commands.minigame;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import com.spillhuset.furious.minigames.Minigame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for getting information about a minigame
 */
public class InfoSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for InfoSubCommand
     *
     * @param plugin The plugin instance
     */
    public InfoSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Get information about a minigame";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /minigame info <game>", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String gameName = args[1].toLowerCase();

        // Check if the game exists
        if (!plugin.getMinigameManager().getGameNames().contains(gameName)) {
            sender.sendMessage(Component.text("That minigame doesn't exist! Available games: " +
                    String.join(", ", plugin.getMinigameManager().getGameNames()), NamedTextColor.RED));
            return true;
        }

        // Get game information
        Minigame game = plugin.getMinigameManager().getGame(gameName);
        int queueSize = plugin.getMinigameManager().getQueueSize(gameName);
        int minPlayers = plugin.getMinigameManager().getMinPlayers(gameName);
        int maxPlayers = plugin.getMinigameManager().getMaxPlayers(gameName);

        // Display game information
        sender.sendMessage(Component.text("=== " + gameName + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Status: ", NamedTextColor.YELLOW)
                .append(Component.text(game.isRunning() ? "Running" : "Waiting for players",
                        game.isRunning() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("Players in queue: ", NamedTextColor.YELLOW)
                .append(Component.text(queueSize + "/" + maxPlayers, NamedTextColor.AQUA)));
        sender.sendMessage(Component.text("Minimum players: ", NamedTextColor.YELLOW)
                .append(Component.text(minPlayers, NamedTextColor.AQUA)));

        // If the game is running, show players in the game
        if (game.isRunning()) {
            sender.sendMessage(Component.text("Players in game:", NamedTextColor.YELLOW));

            List<UUID> players = game.getPlayers();
            if (players.isEmpty()) {
                sender.sendMessage(Component.text("  None", NamedTextColor.GRAY));
            } else {
                for (UUID playerId : players) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        sender.sendMessage(Component.text("  - " + player.getName(), NamedTextColor.AQUA));
                    }
                }
            }
        }

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
        return "furious.minigame.info";
    }
}