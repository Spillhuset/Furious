package com.spillhuset.furious;

import com.spillhuset.furious.commands.EnderseeCommand;
import com.spillhuset.furious.commands.InvseeCommand;
import com.spillhuset.furious.commands.teleport.TeleportCommand;
import com.spillhuset.furious.listeners.TeleportListener;
import com.spillhuset.furious.managers.TeleportManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Furious extends JavaPlugin {
    private TeleportManager teleportManager;

    @Override
    public void onEnable() {
        // Plugin startup logic

        saveDefaultConfig();

        teleportManager = new TeleportManager(this);

        getCommand("invsee").setExecutor(new InvseeCommand(this));
        getCommand("endersee").setExecutor(new EnderseeCommand(this));
        getCommand("teleport").setExecutor(new TeleportCommand(this));

        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);


        getLogger().info("Furious is enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (teleportManager != null) {
            teleportManager.shutdown();
        }

    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }


}
