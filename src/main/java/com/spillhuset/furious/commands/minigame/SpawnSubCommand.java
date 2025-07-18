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
 * Command to set a spawn point for a minigame
 */
public class SpawnSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for SpawnSubCommand
     *
     * @param plugin The plugin instance
     */
    public SpawnSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "Sets a spawn point for a minigame (alternative to using carpet blocks)";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /minigame spawn <num>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Example: /minigame spawn 1", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Note: You can also place carpet blocks where you want spawn points to be.", NamedTextColor.AQUA));
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can use this command
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        int spawnNumber;
        try {
            spawnNumber = Integer.parseInt(args[1]);
            if (spawnNumber < 1) {
                player.sendMessage(Component.text("Spawn number must be at least 1!", NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid spawn number!", NamedTextColor.RED));
            return true;
        }

        boolean success = plugin.getMinigameManager().setSpawnPoint(player, spawnNumber);

        if (!success) {
            player.sendMessage(Component.text("Failed to set spawn point. Make sure you are in edit mode.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Suggest spawn numbers
            completions.add("1");
            completions.add("2");
            completions.add("3");
            completions.add("4");
            completions.add("5");
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.minigame.edit";
    }
}