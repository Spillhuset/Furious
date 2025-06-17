package com.spillhuset.furious.commands.minigame;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to enable a minigame
 */
public class EnableSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for EnableSubCommand
     *
     * @param plugin The plugin instance
     */
    public EnableSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "enable";
    }

    @Override
    public String getDescription() {
        return "Enables a minigame from DISABLED state";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /minigame enable <name>", NamedTextColor.YELLOW));
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
        boolean success = plugin.getMinigameManager().enableMinigame(gameName);

        if (success) {
            sender.sendMessage(Component.text("Minigame " + gameName + " has been enabled.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to enable minigame " + gameName + ". Make sure it exists and is in DISABLED state.", NamedTextColor.RED));
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
        return "furious.minigame.enable";
    }
}