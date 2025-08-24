package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Simple audit logger that writes command usages and responses to a dedicated audit.log file.
 * Minimal, static utility for easy use across the plugin.
 */
public final class AuditLog {
    private static Logger auditLogger;
    private static Furious plugin;
    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private AuditLog() {}

    public static void init(Furious pl) {
        plugin = pl.getInstance();
        try {
            Path dataDir = plugin.getDataFolder().toPath();
            if (Files.notExists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            if (auditLogger == null) {
                auditLogger = Logger.getLogger("Furious-Audit");
                auditLogger.setUseParentHandlers(false);
                // Rotate at ~1MB with 3 backups for simplicity
                FileHandler fh = new FileHandler(dataDir.resolve("audit.log").toString(), 1_000_000, 3, true);
                fh.setFormatter(new SimpleFormatter());
                auditLogger.addHandler(fh);
            }
            auditLogger.info("=== AuditLog initialized ===");
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize AuditLog", ex);
        }
    }

    public static void logCommand(CommandSender sender, String rawCommand) {
        if (auditLogger == null) {
            fallback("CMD", sender, rawCommand);
            return;
        }
        String who = safeName(sender);
        String ts = TS.format(new Date());
        auditLogger.info("[" + ts + "] CMD by " + who + ": /" + rawCommand);
    }

    public static void logResponse(CommandSender sender, String message) {
        if (message == null) message = "";
        if (auditLogger == null) {
            fallback("RSP", sender, message);
            return;
        }
        String who = safeName(sender);
        String ts = TS.format(new Date());
        auditLogger.info("[" + ts + "] RSP to " + who + ": " + message);
    }

    public static void logResponse(CommandSender sender, Component component) {
        String plain = component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
        logResponse(sender, plain);
    }

    private static void fallback(String tag, CommandSender sender, String msg) {
        if (plugin != null) {
            plugin.getLogger().info("[AUDIT " + tag + "] " + safeName(sender) + ": " + msg);
        }
    }

    private static String safeName(CommandSender sender) {
        try {
            return sender == null ? "<null>" : sender.getName();
        } catch (Throwable t) {
            return "<unknown>";
        }
    }
}
