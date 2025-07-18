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
 * Command to exit minigame edit mode without saving changes
 */
public class ExitSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for ExitSubCommand
     *
     * @param plugin The plugin instance
     */
    public ExitSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public String getDescription() {
        return "Exits minigame edit mode without saving changes";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /minigame exit", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Exits edit mode without saving any changes", NamedTextColor.GRAY));
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

        boolean success = plugin.getMinigameManager().exitEditMode(player);

        if (!success) {
            player.sendMessage(Component.text("Failed to exit edit mode. Make sure you are in edit mode.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions for this command
    }

    @Override
    public String getPermission() {
        return "furious.minigame.edit";
    }
}