package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildRole;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final Furious instance;

    public PlayerJoinListener(Furious instance) {
        this.instance = instance.getInstance();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("furious.hidden")) {
            event.quitMessage(null);
        } else {
            event.quitMessage(
                    Component.text("[")
                            .append(Component.text("-", NamedTextColor.RED))
                            .append(Component.text("] "))
                            .append(player.displayName()
                            )
            );
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("furious.hidden")) {
            event.joinMessage(null);
        } else {
            event.joinMessage(
                    Component.text("[")
                            .append(Component.text("+", NamedTextColor.GREEN))
                            .append(Component.text("] "))
                            .append(player.displayName()
                            )
            );
        }

        UUID uuid = player.getUniqueId();

        // Show wallet balance
        try {
            if (instance.walletService != null) {
                double bal = instance.walletService.getBalance(uuid);
                Components.sendInfo(player,
                        Components.t("Wallet: "),
                        Components.amountComp(bal, instance.walletService)
                );
            }
        } catch (Throwable ignored) {
        }

        // Show homes info (set and available)
        try {
            if (instance.homesService != null) {
                int set = instance.homesService.getOwnedCount(uuid);
                int max = instance.homesService.getMaximumCount(uuid);
                int available = instance.homesService.getAvailableHomes(uuid);
                Components.sendInfo(player,
                        Components.t("Homes: "),
                        Components.t("set "), Components.valueComp(String.valueOf(set)),
                        Components.t("  available "), Components.valueComp(String.valueOf(available)),
                        Components.t("  max "), Components.valueComp(String.valueOf(max))
                );
            }
        } catch (Throwable ignored) {
        }

        // Show guild info and rank
        try {
            if (instance.guildService != null) {
                UUID gid = instance.guildService.getGuildIdForMember(uuid);
                if (gid != null) {
                    Guild g = instance.guildService.getGuildById(gid);
                    GuildRole role = (g != null) ? g.getMembers().get(uuid) : null;
                    String gName = (g != null && g.getName() != null) ? g.getName() : "<unnamed>";
                    String gRole = (role != null) ? role.name() : "MEMBER";
                    Components.sendInfo(player,
                            Components.t("Guild: "),
                            Components.valueComp(gName),
                            Components.t("  Rank: "),
                            Components.valueComp(gRole)
                    );
                } else {
                    Components.sendInfo(player,
                            Components.t("Guild: "),
                            Components.valueComp("None")
                    );
                }
            }
        } catch (Throwable ignored) {
        }

        // Show bank account details
        try {
            if (instance.banksService != null) {
                Map<String, Double> accounts = instance.banksService.getAccountsBalances(uuid);
                if (accounts.isEmpty()) {
                    Components.sendInfo(player,
                            Components.t("Bank accounts: "),
                            Components.valueComp("None")
                    );
                } else {
                    Components.sendInfo(player, Components.t("Bank accounts:"));
                    for (Map.Entry<String, Double> e : accounts.entrySet()) {
                        Components.sendInfo(player,
                                Components.t(" - "),
                                Components.valueComp(e.getKey()),
                                Components.t(": "),
                                Components.valueComp(instance.walletService.formatAmount(e.getValue()))
                        );
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Ensure warp armor stands are visible only to ops for this joining player
        try {
            if (instance.warpsService != null) {
                for (String name : instance.warpsService.getWarpNames()) {
                    com.spillhuset.furious.utils.Warp w = instance.warpsService.getWarp(name);
                    if (w == null || w.getArmorStandUuid() == null) continue;
                    org.bukkit.entity.Entity ent = instance.getServer().getEntity(w.getArmorStandUuid());
                    if (ent instanceof org.bukkit.entity.ArmorStand stand) {
                        if (player.isOp()) {
                            player.showEntity(instance, stand);
                        } else {
                            player.hideEntity(instance, stand);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Ensure home armor stands are visible only to ops for this joining player
        try {
            if (instance.homesService != null) {
                instance.homesService.applyHomeArmorStandVisibility(player);
            }
        } catch (Throwable ignored) {
        }

        // Ensure guild home armor stands are visible only to ops for this joining player
        try {
            if (instance.guildHomesService != null) {
                instance.guildHomesService.applyGuildHomeArmorStandVisibility(player);
            }
        } catch (Throwable ignored) {
        }

        // Ensure shop armor stands are visible only to ops for this joining player
        try {
            if (instance.shopsService != null) {
                instance.shopsService.applyShopArmorStandVisibilityForViewer(player);
            }
        } catch (Throwable ignored) {
        }

        // Ensure bank armor stands are visible only to ops for this joining player
        try {
            if (instance.banksService != null) {
                instance.banksService.applyBankArmorStandVisibilityForViewer(player);
            }
        } catch (Throwable ignored) {
        }
    }
}
