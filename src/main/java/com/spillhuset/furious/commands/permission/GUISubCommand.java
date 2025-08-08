package com.spillhuset.furious.commands.permission;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.gui.PermissionManagerGUI;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for opening the permission management GUI.
 */
public class GUISubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new GUISubCommand.
     *
     * @param plugin The plugin instance
     */
    public GUISubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public String getDescription() {
        return "Open the permission management GUI";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/permissions gui", NamedTextColor.YELLOW)
                .append(Component.text(" - Open the permission management GUI", NamedTextColor.WHITE)));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return false;
        }

        // Get the PermissionManagerGUI instance from the plugin
        PermissionManagerGUI gui = plugin.getPermissionManagerGUI();

        if (gui == null) {
            sender.sendMessage(Component.text("The permission management GUI is not available.", NamedTextColor.RED));
            return false;
        }

        // Open the permission manager GUI for the player
        gui.openPermissionManager(player);
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completions for this command
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.permission.*";
    }
}