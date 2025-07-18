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
 * Command to disable the queue for a minigame
 */
public class DisableSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for DisableSubCommand
     *
     * @param plugin The plugin instance
     */
    public DisableSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "disable";
    }

    @Override
    public String getDescription() {
        return "Disables the queue for a minigame";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /minigame disable <game>", NamedTextColor.YELLOW));
    }

    @Override
    public boolean denyNonPlayer() {
        return false; // Allow console to use this command
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String gameName = args[1];
        boolean success = plugin.getMinigameManager().disableQueue(gameName);

        if (success) {
            sender.sendMessage(Component.text("Queue for minigame " + gameName + " has been disabled.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to disable queue for minigame " + gameName + ". Make sure it exists.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (String gameName : plugin.getMinigameManager().getGameNames()) {
                if (gameName.toLowerCase().startsWith(partial)) {
                    completions.add(gameName);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.minigame.disable";
    }
}