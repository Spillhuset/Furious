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
 * Command to edit a minigame
 */
public class EditSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for EditSubCommand
     *
     * @param plugin The plugin instance
     */
    public EditSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "edit";
    }

    @Override
    public String getDescription() {
        return "Enters edit mode for a minigame";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /minigame edit <name>", NamedTextColor.YELLOW));
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

        String gameName = args[1];
        boolean success = plugin.getMinigameManager().editMinigame(player, gameName);

        if (!success) {
            player.sendMessage(Component.text("Failed to enter edit mode for minigame " + gameName + ".", NamedTextColor.RED));
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
        return "furious.minigame.edit";
    }
}