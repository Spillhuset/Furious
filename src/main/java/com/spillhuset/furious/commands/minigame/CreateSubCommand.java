package com.spillhuset.furious.commands.minigame;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.enums.MinigameType;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to create a new minigame
 */
public class CreateSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for CreateSubCommand
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
        return "Create a new minigame";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /minigame create <name> <type> <min> [map]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Example: /minigame create mygame hungergame 2 mygameworld", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Available types: ", NamedTextColor.YELLOW)
                .append(Component.text(String.join(", ", getAvailableTypes()), NamedTextColor.GRAY)));
    }

    @Override
    public boolean denyNonPlayer() {
        return true;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (args.length < 4) {
            getUsage(sender);
            return true;
        }

        String name = args[1];
        String typeStr = args[2].toLowerCase();
        int minPlayers;
        String mapName = args.length > 4 ? args[4] : name;

        // Validate name
        if (name.length() < 3 || name.length() > 16) {
            player.sendMessage(Component.text("Name must be between 3 and 16 characters!", NamedTextColor.RED));
            return true;
        }

        // Validate type
        MinigameType type = MinigameType.getById(typeStr);
        if (type == null) {
            player.sendMessage(Component.text("Invalid minigame type! Available types: ", NamedTextColor.RED)
                    .append(Component.text(String.join(", ", getAvailableTypes()), NamedTextColor.GRAY)));
            return true;
        }

        // Validate min players
        try {
            minPlayers = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid number for minimum players!", NamedTextColor.RED));
            return true;
        }

        // Create the minigame
        boolean success = plugin.getMinigameManager().createMinigame(name, type, minPlayers, mapName);
        if (success) {
            player.sendMessage(Component.text("Minigame " + name + " created successfully!", NamedTextColor.GREEN));
            player.sendMessage(Component.text("Use /minigame edit " + name + " to set up spawn points.", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("A minigame with that name already exists!", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Suggest a default name
            completions.add("mygame");
        } else if (args.length == 3) {
            // Suggest available types
            for (String type : getAvailableTypes()) {
                if (type.startsWith(args[2].toLowerCase())) {
                    completions.add(type);
                }
            }
        } else if (args.length == 4) {
            // Suggest minimum players
            completions.add("2");
            completions.add("4");
            completions.add("8");
        } else if (args.length == 5) {
            // Suggest map name (same as game name by default)
            completions.add(args[1]);
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.minigame.create";
    }

    /**
     * Gets a list of available minigame types
     *
     * @return A list of available minigame types
     */
    private List<String> getAvailableTypes() {
        List<String> types = new ArrayList<>();
        for (MinigameType type : MinigameType.values()) {
            types.add(type.getId());
        }
        return types;
    }
}