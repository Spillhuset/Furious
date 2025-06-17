package com.spillhuset.furious.commands.minigame;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.enums.MinigameState;
import com.spillhuset.furious.minigames.ConfigurableMinigame;
import com.spillhuset.furious.minigames.Minigame;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for listing all available minigames
 */
public class ListSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for ListSubCommand
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
        return "List all available minigames";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /minigame list", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        sender.sendMessage(Component.text("Available Minigames:", NamedTextColor.GOLD));

        if (plugin.getMinigameManager().getGameNames().isEmpty()) {
            sender.sendMessage(Component.text("No minigames available!", NamedTextColor.RED));
            return true;
        }

        for (String gameName : plugin.getMinigameManager().getGameNames()) {
            Minigame game = plugin.getMinigameManager().getGame(gameName);

            if (game instanceof ConfigurableMinigame configGame) {
                int queueSize = plugin.getMinigameManager().getQueueSize(gameName);
                int minPlayers = configGame.getMinPlayers();
                int maxPlayers = configGame.getMaxPlayers();
                MinigameState state = configGame.getState();

                // Format: <name> - <type> - <min>/<inqueue>/<max> - <state>
                Component gameInfo = Component.text(configGame.getName(), NamedTextColor.GREEN)
                        .append(Component.text(" - ", NamedTextColor.GRAY))
                        .append(Component.text(configGame.getType().getDisplayName(), NamedTextColor.YELLOW))
                        .append(Component.text(" - ", NamedTextColor.GRAY))
                        .append(Component.text(minPlayers + "/" + queueSize + "/" + maxPlayers, NamedTextColor.AQUA))
                        .append(Component.text(" - ", NamedTextColor.GRAY))
                        .append(Component.text(state.getName(), getStateColor(state)));

                sender.sendMessage(gameInfo);
            } else {
                // Legacy minigames
                int queueSize = plugin.getMinigameManager().getQueueSize(gameName);
                int minPlayers = plugin.getMinigameManager().getMinPlayers(gameName);
                int maxPlayers = plugin.getMinigameManager().getMaxPlayers(gameName);

                // Try to get the minigame type from the name
                String typeName = "Legacy";
                com.spillhuset.furious.enums.MinigameType type = com.spillhuset.furious.enums.MinigameType.getById(gameName);
                if (type != null) {
                    typeName = type.getDisplayName();
                }

                Component gameInfo = Component.text(gameName, NamedTextColor.GREEN)
                        .append(Component.text(" - ", NamedTextColor.GRAY))
                        .append(Component.text(typeName, NamedTextColor.YELLOW))
                        .append(Component.text(" - ", NamedTextColor.GRAY))
                        .append(Component.text(minPlayers + "/" + queueSize + "/" + maxPlayers, NamedTextColor.AQUA))
                        .append(Component.text(" - ", NamedTextColor.GRAY))
                        .append(Component.text(game.isRunning() ? "STARTED" : "READY",
                                game.isRunning() ? NamedTextColor.RED : NamedTextColor.GREEN));

                sender.sendMessage(gameInfo);
            }
        }

        return true;
    }

    /**
     * Gets the color for a minigame state
     *
     * @param state The state
     * @return The color
     */
    private NamedTextColor getStateColor(MinigameState state) {
        return switch (state) {
            case DISABLED, STARTED -> NamedTextColor.RED;
            case READY -> NamedTextColor.GREEN;
            case QUEUE -> NamedTextColor.YELLOW;
            case COUNTDOWN -> NamedTextColor.GOLD;
            case FINAL -> NamedTextColor.DARK_RED;
        };
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions for this command
    }

    @Override
    public String getPermission() {
        return "furious.minigame.list";
    }
}
