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
 * Command to start a minigame
 */
public class StartSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for StartSubCommand
     *
     * @param plugin The plugin instance
     */
    public StartSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "Starts a game from the COUNTDOWN state";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /minigame start <name>", NamedTextColor.YELLOW));
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
        boolean success = plugin.getMinigameManager().startMinigame(gameName);

        if (success) {
            sender.sendMessage(Component.text("Minigame " + gameName + " has been started.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to start minigame " + gameName + ". Make sure it exists and is in COUNTDOWN state.", NamedTextColor.RED));
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
        return "furious.minigame.start";
    }
}