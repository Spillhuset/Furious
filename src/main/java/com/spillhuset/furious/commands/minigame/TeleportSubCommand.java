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
 * Subcommand for teleporting to the GameWorld
 */
public class TeleportSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Constructor for TeleportSubCommand
     *
     * @param plugin The plugin instance
     */
    public TeleportSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public String getDescription() {
        return "Teleport to the GameWorld";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /gameworld tp", NamedTextColor.YELLOW));
    }

    @Override
    public boolean denyNonPlayer() {
        return true;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Teleport the player to the GameWorld
        plugin.getWorldManager().teleportToGameWorld(player);
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions for this command
    }

    @Override
    public String getPermission() {
        return "furious.minigame.tp";
    }
}
