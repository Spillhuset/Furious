package com.spillhuset.furious.commands.WalletCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReloadCommand implements SubCommandInterface {
    private final Furious instance;

    public ReloadCommand(Furious plugin) {
        this.instance = plugin.getInstance();
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "furious.wallet.reload";
    }


    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length == 1 && can(sender, true)) {
            instance.walletService.flushNow();
            instance.walletService.load();

            // Feedback
            Components.sendSuccessMessage(sender, "Reloaded wallet configuration.");
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /wallet reload");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {

        return List.of();

    }
}
