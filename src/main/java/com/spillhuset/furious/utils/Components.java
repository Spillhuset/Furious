package com.spillhuset.furious.utils;

import com.spillhuset.furious.services.WalletService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class Components {
    public static void sendColored(CommandSender s, NamedTextColor base, Component... parts) {
        Component c = compose(base, parts);
        s.sendMessage(c);
        // Audit the response
        try { AuditLog.logResponse(s, c); } catch (Throwable ignored) {}
    }

    public static Component compose(NamedTextColor base, Component... parts) {
        Component c = Component.text("", base);
        for (Component p : parts) {
            c = c.append(p.colorIfAbsent(base));
        }
        return c;
    }

    public static Component t(String s) {
        return Component.text(s);
    }

    public static Component t(String s, NamedTextColor color) {
        return Component.text(s, color);
    }

    public static Component playerComp(String name) {
        return Component.text(String.valueOf(name), NamedTextColor.GOLD);
    }

    public static Component amountComp(double amount, WalletService wallet) {
        return Component.text(wallet.formatAmount(amount), NamedTextColor.GRAY);
    }
    public static Component valueComp(String value) {
        return Component.text(value, NamedTextColor.GRAY);
    }

    public static void sendErrorMessage(CommandSender s, String msg) {
        sendColored(s, NamedTextColor.RED, t(msg));
    }

    public static void sendErrorMessage(CommandSender s, Component msg) {
        sendColored(s, NamedTextColor.RED, msg);
    }

    public static void sendError(CommandSender s, Component... parts) {
        sendColored(s, NamedTextColor.RED, parts);
    }

    public static void sendInfoMessage(CommandSender s, String msg) {
        sendColored(s, NamedTextColor.YELLOW, t(msg));
    }

    public static void sendInfo(CommandSender s, Component... parts) {
        sendColored(s, NamedTextColor.YELLOW, parts);
    }

    public static void sendSuccessMessage(CommandSender s, String msg) {
        sendColored(s, NamedTextColor.GREEN, t(msg));
    }

    public static void sendSuccess(CommandSender s, Component... parts) {
        sendColored(s, NamedTextColor.GREEN, parts);
    }

    public static void sendGreyMessage(CommandSender s, String msg) {
        sendColored(s, NamedTextColor.GRAY, t(msg));
    }
}
