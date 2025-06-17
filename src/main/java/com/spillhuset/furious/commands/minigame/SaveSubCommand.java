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
 * Command to save minigame changes and exit edit mode
 */
public class SaveSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for SaveSubCommand
     *
     * @param plugin The plugin instance
     */
    public SaveSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "save";
    }

    @Override
    public String getDescription() {
        return "Saves minigame changes and exits edit mode";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /minigame save", NamedTextColor.YELLOW));
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

        boolean success = plugin.getMinigameManager().saveMinigameChanges(player);

        if (!success) {
            player.sendMessage(Component.text("Failed to save changes. Make sure you are in edit mode.", NamedTextColor.RED));
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