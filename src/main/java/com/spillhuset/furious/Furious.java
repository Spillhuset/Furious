package com.spillhuset.furious;

import com.spillhuset.furious.commands.*;
import com.spillhuset.furious.listeners.*;
import com.spillhuset.furious.managers.ArmorStandManager;
import com.spillhuset.furious.services.*;
import com.spillhuset.furious.services.Checklist.BiomesService;
import com.spillhuset.furious.services.Checklist.MonstersService;
import com.spillhuset.furious.services.Checklist.TamingService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class Furious extends JavaPlugin {
    private static Furious instance;

    public WalletService walletService;
    public HomesService homesService;
    public GuildService guildService;
    public GuildHomesService guildHomesService;
    public WarpsService warpsService;
    public BanksService banksService;
    public TeleportsService teleportsService;
    public ShopsService shopsService;
    public ArmorStandManager armorStandManager;
    public TombstoneService tombstoneService;
    public LocksService locksService;
    public BiomesService biomesService;
    public MonstersService monstersService;
    public TamingService tamingService;
    public com.spillhuset.furious.utils.RegistryCache registryCache;
    public com.spillhuset.furious.utils.MessageThrottle messageThrottle;

    @Override
    public void onEnable() {
        instance = this;
        // Ensure default config.yml is available for rewards and wallet settings
        saveDefaultConfig();

        // Build registry cache once to avoid repeated traversal during gameplay
        registryCache = new com.spillhuset.furious.utils.RegistryCache(instance);
        registryCache.init();

        // Initialize message throttle for anti-spam of action bars/broadcasts
        messageThrottle = new com.spillhuset.furious.utils.MessageThrottle(instance);

        guildService = new GuildService(instance);
        guildService.load();

        guildHomesService = new GuildHomesService(instance);
        guildHomesService.load();

        walletService = new WalletService(instance);
        walletService.load();

        homesService = new HomesService(instance);
        homesService.load();

        warpsService = new WarpsService(instance);
        warpsService.load();

        banksService = new BanksService(instance);
        banksService.load();

        shopsService = new ShopsService(instance);
        shopsService.load();

        teleportsService = new TeleportsService(instance);
        armorStandManager = new ArmorStandManager(instance);
        tombstoneService = new TombstoneService(instance);
        locksService = new LocksService(instance);
        locksService.load();

        biomesService = new BiomesService(instance);
        biomesService.load();

        monstersService = new MonstersService(instance);
        monstersService.load();

        tamingService = new TamingService(instance);
        tamingService.load();

        PluginCommand cmd;

        cmd = getCommand("wallet");
        if (cmd != null) {
            WalletCommand wc = new WalletCommand(instance);
            cmd.setExecutor(wc);
            cmd.setTabCompleter(wc);
        }

        cmd = getCommand("homes");
        if (cmd != null) {
            HomesCommand hc = new HomesCommand(instance);
            cmd.setExecutor(hc);
            cmd.setTabCompleter(hc);
        }

        cmd = getCommand("guild");
        if (cmd != null) {
            GuildCommand gc = new GuildCommand(instance);
            cmd.setExecutor(gc);
            cmd.setTabCompleter(gc);
        }

        // Register warps command
        cmd = getCommand("warps");
        if (cmd != null) {
            WarpsCommand wpc = new WarpsCommand(instance);
            cmd.setExecutor(wpc);
            cmd.setTabCompleter(wpc);
        }

        cmd = getCommand("banks");
        if (cmd != null) {
            BanksCommand bc = new BanksCommand(instance);
            cmd.setExecutor(bc);
            cmd.setTabCompleter(bc);
        }

        cmd = getCommand("shops");
        if (cmd != null) {
            ShopsCommand sc = new ShopsCommand(instance);
            cmd.setExecutor(sc);
            cmd.setTabCompleter(sc);
        }

        cmd = getCommand("teleport");
        if (cmd != null) {
            TeleportCommand tc = new TeleportCommand(instance);
            cmd.setExecutor(tc);
            cmd.setTabCompleter(tc);
        }

        cmd = getCommand("tombstones");
        if (cmd != null) {
            TombstoneCommand tsc = new TombstoneCommand(instance);
            cmd.setExecutor(tsc);
            cmd.setTabCompleter(tsc);
        }

        cmd = getCommand("locks");
        if (cmd != null) {
            LocksCommand lc = new LocksCommand(instance);
            cmd.setExecutor(lc);
            cmd.setTabCompleter(lc);
        }

        cmd = getCommand("checklist");
        if (cmd != null) {
            ChecklistCommand clc = new ChecklistCommand(instance);
            cmd.setExecutor(clc);
            cmd.setTabCompleter(clc);
        }

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(instance), instance);
        // Ensure ops are hidden from non-ops on join and when op/deop changes
        getServer().getPluginManager().registerEvents(new OpVisibilityListener(instance), instance);
        getServer().getPluginManager().registerEvents(new SelectionListener(instance), instance);
        getServer().getPluginManager().registerEvents(new TeleportsListener(instance), instance);

        getServer().getPluginManager().registerEvents(new ArmorStandListener(instance), instance);
        getServer().getPluginManager().registerEvents(new TombstoneListener(instance), instance);
        // Show territory info on chunk changes
        getServer().getPluginManager().registerEvents(new ChunkChangeListener(instance), instance);
        // Enforce SAFE-type protections in guild territories
        getServer().getPluginManager().registerEvents(new ProtectionListener(instance), instance);
        // Enforce simple block locks
        getServer().getPluginManager().registerEvents(new LocksListener(instance), instance);
        // Cleanup orphans and recreate missing ArmorStands on chunk load
        getServer().getPluginManager().registerEvents(new ChunkArmorStandSanitizer(instance), instance);
        // Track visited biomes
        getServer().getPluginManager().registerEvents(new BiomeTrackListener(instance), instance);
        // Track removed monsters
        getServer().getPluginManager().registerEvents(new MonsterTrackListener(instance), instance);
        // Track tamed animals
        getServer().getPluginManager().registerEvents(new TamingTrackListener(instance), instance);

        // Ensure all marker ArmorStands are present after startup (centralized)
        try {
            if (armorStandManager != null) armorStandManager.ensureArmorStands();
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Failed to ensure ArmorStands", ex);
        }

        getLogger().info("Furious enabled!");
    }

    @Override
    public void onDisable() {
        if (walletService != null) walletService.save();
        if (homesService != null) homesService.save();
        if (guildService != null) guildService.save();
        if (guildHomesService != null) guildHomesService.save();
        if (warpsService != null) warpsService.save();
        if (banksService != null) banksService.save();
        if (shopsService != null) shopsService.save();
        if (locksService != null) locksService.save();
        if (biomesService != null) biomesService.save();
        if (monstersService != null) monstersService.save();
        if (tamingService != null) tamingService.save();
        getLogger().info("Furious disabled!");
    }

    public Furious getInstance() {
        return instance;
    }
}
