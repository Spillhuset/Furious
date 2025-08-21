package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.QueueData;
import com.spillhuset.furious.utils.TpRequest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Handles queued teleports with a 10-second countdown shown via a BossBar.
 * Cancels on movement, interaction, or damage. Applies nausea and darkness while queued.
 */
public class TeleportsService {
    private final Furious plugin;

    // Teleport request handling moved to utils.TpRequest
    private final Map<UUID, TpRequest> incomingByTarget = new HashMap<>(); // target -> request
    private final Map<UUID, TpRequest> outgoingBySender = new HashMap<>(); // sender -> request
    private final Map<UUID, Long> cooldownBySender = new HashMap<>(); // sender -> nextAllowedMs
    private final Set<UUID> denyToggles = new HashSet<>();

    public TeleportsService(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    // Configuration getters with defaults
    private int reqTimeoutSec() {
        return Math.max(5, plugin.getConfig().getInt("teleport.request-timeout-seconds", 120));
    }

    private int cooldownSec() {
        return Math.max(0, plugin.getConfig().getInt("teleport.cooldown-seconds", 300));
    }

    private double requestCost() {
        return Math.max(0d, plugin.getConfig().getDouble("teleport.cost", 50d));
    }

    private int queueSeconds() {
        // Countdown seconds before a queued teleport completes
        return Math.max(1, plugin.getConfig().getInt("teleport.queue-seconds", 10));
    }

    public boolean isDenyToggled(UUID player) {
        return denyToggles.contains(player);
    }

    public boolean toggleDeny(UUID player) {
        if (denyToggles.contains(player)) {
            denyToggles.remove(player);
            return false;
        }
        denyToggles.add(player);
        return true;
    }

    public void cancelOutgoing(UUID sender, String reason) {
        TpRequest out = outgoingBySender.remove(sender);
        if (out != null) {
            incomingByTarget.remove(out.target);
            Player s = Bukkit.getPlayer(sender);
            if (s != null) Components.sendInfoMessage(s, "Teleport request cancelled: " + reason);
            Player t = Bukkit.getPlayer(out.target);
            if (t != null)
                Components.sendGreyMessage(t, "Request from " + (s != null ? s.getName() : sender) + " was cancelled.");
        }
    }

    public boolean hasIncomingFor(UUID target) {
        return incomingByTarget.containsKey(target);
    }

    public boolean requestTeleport(@NotNull Player sender, @NotNull Player target) {
        UUID sId = sender.getUniqueId();
        UUID tId = target.getUniqueId();
        if (sId.equals(tId)) {
            Components.sendErrorMessage(sender, "You cannot request to yourself.");
            return false;
        }
        if (isQueued(sender)) {
            Components.sendErrorMessage(sender, "You are already teleporting.");
            return false;
        }
        Long nextAllowed = cooldownBySender.get(sId);
        long now = System.currentTimeMillis();
        if (nextAllowed != null && now < nextAllowed) {
            long sec = (nextAllowed - now + 999) / 1000;
            Components.sendErrorMessage(sender, "You must wait " + sec + "s before sending another request.");
            return false;
        }
        if (isDenyToggled(tId)) {
            Components.sendErrorMessage(sender, target.getName() + " is not accepting teleport requests.");
            return false;
        }
        // Charge cost
        double cost = requestCost();
        if (cost > 0) {
            if (plugin.walletService == null || !plugin.walletService.subBalance(sId, cost, "Teleport request to " + tId)) {
                if (plugin.walletService != null) {
                    Components.sendErrorMessage(sender, "You need " + plugin.walletService.formatAmount(cost) + " to send a request.");
                }
                return false;
            } else {
                Components.sendGreyMessage(sender, "Charged " + plugin.walletService.formatAmount(cost) + " for teleport request.");
            }
        }
        // Cancel prior pending from this sender
        cancelOutgoing(sId, "new request");
        int timeout = reqTimeoutSec();
        TpRequest req = new TpRequest(sId, tId, now + timeout * 1000L);
        outgoingBySender.put(sId, req);
        incomingByTarget.put(tId, req);
        cooldownBySender.put(sId, now + cooldownSec() * 1000L);
        // Schedule expiry
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TpRequest curIn = incomingByTarget.get(tId);
            if (curIn != null && curIn == req && System.currentTimeMillis() >= req.expireAtMs) {
                incomingByTarget.remove(tId);
                outgoingBySender.remove(sId);
                Player s = Bukkit.getPlayer(sId);
                Player tt = Bukkit.getPlayer(tId);
                if (s != null)
                    Components.sendErrorMessage(s, "Your teleport request to " + (tt != null ? tt.getName() : "player") + " expired.");
                if (tt != null)
                    Components.sendGreyMessage(tt, "Request from " + (s != null ? s.getName() : "player") + " expired.");
            }
        }, timeout * 20L + 5);

        sendClickablePrompt(sender, target, timeout);
        return true;
    }

    private void sendClickablePrompt(Player sender, Player target, int timeout) {
        try {
            net.kyori.adventure.text.Component base = net.kyori.adventure.text.Component.text(sender.getName() + " wants to teleport to you. ");
            net.kyori.adventure.text.Component accept = net.kyori.adventure.text.Component.text("[ACCEPT]")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/teleport accept " + sender.getName()));
            net.kyori.adventure.text.Component space = net.kyori.adventure.text.Component.text(" ");
            net.kyori.adventure.text.Component decline = net.kyori.adventure.text.Component.text("[DECLINE]")
                    .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/teleport decline " + sender.getName()));
            target.sendMessage(base.append(accept).append(space).append(decline)
                    .append(net.kyori.adventure.text.Component.text(" (" + timeout + "s)")));
        } catch (Throwable e) {
            Components.sendInfoMessage(target, sender.getName() + " wants to teleport to you. Use /teleport accept " + sender.getName() + " or /teleport decline " + sender.getName());
        }
        Components.sendInfoMessage(sender, "Teleport request sent to " + target.getName() + " (expires in " + timeout + "s)");
    }

    public boolean accept(@NotNull Player target, @NotNull String senderName) {
        Player sender = Bukkit.getPlayerExact(senderName);
        if (sender == null) {
            Components.sendErrorMessage(target, "Sender is not online.");
            return false;
        }
        TpRequest req = incomingByTarget.get(target.getUniqueId());
        if (req == null || !req.sender.equals(sender.getUniqueId())) {
            Components.sendErrorMessage(target, "No pending request from " + senderName + ".");
            return false;
        }
        if (System.currentTimeMillis() > req.expireAtMs) {
            incomingByTarget.remove(target.getUniqueId());
            outgoingBySender.remove(req.sender);
            Components.sendErrorMessage(target, "That request has expired.");
            return false;
        }
        // Clean up
        incomingByTarget.remove(target.getUniqueId());
        outgoingBySender.remove(req.sender);
        // Teleport sender to target via queued teleport
        plugin.teleportsService.queueTeleport(sender, target.getLocation(), "to " + target.getName());
        Components.sendSuccessMessage(target, "Accepted request from " + sender.getName() + ".");
        Components.sendSuccessMessage(sender, target.getName() + " accepted your request.");
        return true;
    }

    public boolean decline(@NotNull Player target, @NotNull String senderName) {
        Player sender = Bukkit.getPlayerExact(senderName);
        TpRequest req = incomingByTarget.get(target.getUniqueId());
        if (req == null || (sender != null && !req.sender.equals(sender.getUniqueId()))) {
            Components.sendErrorMessage(target, "No pending request" + " from " + senderName + ".");
            return false;
        }
        incomingByTarget.remove(target.getUniqueId());
        outgoingBySender.remove(req.sender);
        Components.sendInfoMessage(target, "Declined teleport request.");
        Player s = sender != null ? sender : Bukkit.getPlayer(req.sender);
        if (s != null) Components.sendErrorMessage(s, target.getName() + " declined your teleport request.");
        return true;
    }

    public boolean cancel(@NotNull Player sender) {
        TpRequest req = outgoingBySender.get(sender.getUniqueId());
        if (req == null) {
            Components.sendErrorMessage(sender, "You have no outgoing request.");
            return false;
        }
        cancelOutgoing(sender.getUniqueId(), "by sender");
        return true;
    }


    private final Map<UUID, QueueData> queued = new HashMap<>();

    public boolean isQueued(@NotNull Player player) {
        return queued.containsKey(player.getUniqueId());
    }

    public void cancelQueue(@NotNull Player player, @NotNull String reason) {
        QueueData data = queued.remove(player.getUniqueId());
        if (data != null) {
            try {
                Bukkit.getScheduler().cancelTask(data.taskId);
            } catch (Throwable ignored) {
            }
            try {
                data.bossBar.removeAll();
            } catch (Throwable ignored) {
            }
            clearEffects(player);
            Components.sendErrorMessage(player, "Teleport cancelled: " + reason);
        }
    }

    private void applyEffects(@NotNull Player player, int seconds) {
        try {
            // Nausea and Darkness effects (handle API differences)
            PotionEffectType nausea = resolveEffect("NAUSEA", "CONFUSION");
            if (nausea != null) {
                player.addPotionEffect(new PotionEffect(nausea, seconds * 20 + 40, 0, true, false, true));
            }
        } catch (Throwable ignored) {
        }
        try {
            PotionEffectType darkness = resolveEffect("DARKNESS");
            if (darkness != null) {
                player.addPotionEffect(new PotionEffect(darkness, seconds * 20 + 40, 0, true, false, true));
            }
        } catch (Throwable ignored) {
        }
    }

    private void clearEffects(@NotNull Player player) {
        try {
            PotionEffectType nausea = resolveEffect("NAUSEA", "CONFUSION");
            if (nausea != null) player.removePotionEffect(nausea);
        } catch (Throwable ignored) {
        }
        try {
            PotionEffectType darkness = resolveEffect("DARKNESS");
            if (darkness != null) player.removePotionEffect(darkness);
        } catch (Throwable ignored) {
        }
    }

    private PotionEffectType resolveEffect(String... names) {
        for (String n : names) {
            // Try static constant field (e.g., CONFUSION, DARKNESS, NAUSEA) to avoid deprecated name lookup
            try {
                java.lang.reflect.Field f = PotionEffectType.class.getField(n);
                Object val = f.get(null);
                if (val instanceof PotionEffectType t) return t;
            } catch (Throwable ignored) {
            }
            // Fallback to namespaced key lookup (modern API)
            try {
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(n.toLowerCase(java.util.Locale.ROOT));
                PotionEffectType t = null;
                // Preferred modern API via Registry (avoids deprecated lookups)
                try {
                    t = org.bukkit.Registry.POTION_EFFECT_TYPE.get(key);
                } catch (Throwable ignoredInner) {
                }
                if (t == null) {
                    // Older API: attempt static getByKey (may be deprecated) reflectively to avoid compile-time deprecation
                    try {
                        java.lang.reflect.Method m = PotionEffectType.class.getMethod("getByKey", org.bukkit.NamespacedKey.class);
                        t = (PotionEffectType) m.invoke(null, key);
                    } catch (Throwable ignored) {
                    }
                }
                if (t != null) return t;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * Queue a teleport with 10-second countdown.
     *
     * @param player the player to teleport
     * @param target target location
     * @param label  label to show on the bar (e.g., Home: name)
     */
    public void queueTeleport(@NotNull Player player, @NotNull Location target, @NotNull String label) {
        if (target.getWorld() == null) {
            Components.sendErrorMessage(player, "Invalid target location.");
            return;
        }
        // Cancel any existing queue first
        if (isQueued(player)) {
            cancelQueue(player, "new teleport started");
        }
        final int seconds = queueSeconds();
        BossBar bar = Bukkit.createBossBar("Teleporting in " + seconds + "s: " + label, BarColor.BLUE, BarStyle.SOLID);
        bar.setProgress(1.0);
        bar.addPlayer(player);
        applyEffects(player, seconds);

        long startTick = Bukkit.getCurrentTick();
        Location startBlockLoc = player.getLocation().getBlock().getLocation();
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            int remaining = seconds;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(player.getUniqueId());
                if (p == null) {
                    // Player left; cleanup
                    cancelQueue(player, "player left");
                    return;
                }
                // Update UI
                try {
                    bar.setTitle("Teleporting in " + remaining + "s: " + label);
                    bar.setProgress(Math.max(0.0, Math.min(1.0, remaining / (double) seconds)));
                } catch (Throwable ignored) {
                }

                if (remaining <= 0) {
                    // Complete
                    try {
                        Bukkit.getScheduler().cancelTask(thisTaskId());
                    } catch (Throwable ignored) {
                    }
                    queued.remove(player.getUniqueId());
                    try {
                        bar.removeAll();
                    } catch (Throwable ignored) {
                    }
                    clearEffects(p);
                    p.teleportAsync(target);
                    Components.sendSuccess(p, Components.t("Teleported."));
                }
                remaining--;
            }

            private int thisTaskId() {
                return queued.getOrDefault(player.getUniqueId(), new QueueData(null, null, null, 0, 0, bar, -1, null)).taskId;
            }
        }, 0L, 20L);

        QueueData data = new QueueData(player.getUniqueId(), target, label, startTick, seconds, bar, taskId, startBlockLoc);
        queued.put(player.getUniqueId(), data);
        Components.sendInfo(player, Components.t("Teleport queued. Do not move, take damage, or interact."));
    }

}
