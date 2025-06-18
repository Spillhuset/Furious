package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Tombstone;
import com.spillhuset.furious.managers.TombstoneManager;
import com.spillhuset.furious.misc.StandaloneCommand;
import com.spillhuset.furious.utils.AuditLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Command handler for tombstone-related commands.
 */
public class TombstonesCommand extends StandaloneCommand {
    private final TombstoneManager tombstoneManager;
    private final AuditLogger auditLogger;

    /**
     * Creates a new TombstonesCommand.
     *
     * @param plugin The plugin instance
     */
    public TombstonesCommand(Furious plugin) {
        super(plugin);
        this.tombstoneManager = plugin.getTombstoneManager();
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "tombstones";
    }

    @Override
    public String getDescription() {
        return "Manage tombstones";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/tombstones purge - Remove all tombstones", NamedTextColor.YELLOW));
    }

    @Override
    public String getPermission() {
        return "furious.tombstones.admin";
    }

    @Override
    protected boolean executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            getUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if ("purge".equals(subCommand)) {
            return executePurgeCommand(sender);
        } else {
            getUsage(sender);
            return true;
        }
    }

    /**
     * Executes the purge subcommand.
     *
     * @param sender The command sender
     * @return true if the command was handled, false otherwise
     */
    private boolean executePurgeCommand(CommandSender sender) {
        // Get all tombstones
        Collection<Tombstone> tombstones = tombstoneManager.getAllTombstones();
        int count = tombstones.size();

        if (count == 0) {
            sender.sendMessage(Component.text("There are no tombstones to purge.", NamedTextColor.YELLOW));
            return true;
        }

        // Remove all tombstones
        for (Tombstone tombstone : new ArrayList<>(tombstones)) {
            tombstoneManager.removeTombstone(tombstone.getId());
        }

        // Log the action
        auditLogger.logSensitiveOperation(sender, "tombstone purge", "Purged " + count + " tombstones");

        // Notify the sender
        sender.sendMessage(Component.text("Successfully purged " + count + " tombstones.", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("purge".startsWith(partial)) {
                completions.add("purge");
            }
        }

        return completions;
    }
}
