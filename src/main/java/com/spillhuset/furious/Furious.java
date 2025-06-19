package com.spillhuset.furious;

import com.spillhuset.furious.commands.BankCommand;
import com.spillhuset.furious.commands.EnderseeCommand;
import com.spillhuset.furious.commands.FeedCommand;
import com.spillhuset.furious.commands.HealCommand;
import com.spillhuset.furious.commands.InvseeCommand;
import com.spillhuset.furious.commands.PermissionCommand;
import com.spillhuset.furious.commands.SecurityCommand;
import com.spillhuset.furious.commands.TombstonesCommand;
import com.spillhuset.furious.commands.guild.GuildCommand;
import com.spillhuset.furious.commands.homes.HomesCommand;
import com.spillhuset.furious.commands.locks.LocksCommand;
import com.spillhuset.furious.commands.minigame.MinigameCommand;
import com.spillhuset.furious.commands.teleport.TeleportCommand;
import com.spillhuset.furious.commands.teleport.TpaCommand;
import com.spillhuset.furious.commands.teleport.TpacceptCommand;
import com.spillhuset.furious.commands.teleport.TpdeclineCommand;
import com.spillhuset.furious.commands.warps.WarpsCommand;
import com.spillhuset.furious.listeners.*;
import com.spillhuset.furious.managers.*;
import com.spillhuset.furious.minigames.hungergames.ContainerRegistry;
import com.spillhuset.furious.managers.PermissionManager;
import com.spillhuset.furious.utils.AuditLogger;
import com.spillhuset.furious.utils.EncryptionUtil;
import com.spillhuset.furious.utils.RateLimiter;
import com.spillhuset.furious.utils.SecurityReviewManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class Furious extends JavaPlugin {
    private TeleportManager teleportManager;
    private WalletManager walletManager;
    private GuildManager guildManager;
    private LocksManager locksManager;
    private MinigameManager minigameManager;
    private WorldManager worldManager;
    private HomesManager homesManager;
    private WarpsManager warpsManager;
    private ContainerRegistry containerRegistry;
    private PlayerDataManager playerDataManager;
    private AuditLogger auditLogger;
    private RateLimiter rateLimiter;
    private TombstoneManager tombstoneManager;
    private SecurityReviewManager securityReviewManager;
    private CombatManager combatManager;
    private EncryptionUtil encryptionUtil;
    private BankManager bankManager;
    private BankInterestListener bankInterestListener;
    private PermissionManager permissionManager;
    private static Furious instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        saveDefaultConfig();

        // Save default configuration files
        saveDefaultConfigFiles();

        teleportManager = new TeleportManager(this);
        walletManager = new WalletManager(this);
        guildManager = new GuildManager(this);
        locksManager = new LocksManager(this);
        worldManager = new WorldManager(this);
        minigameManager = new MinigameManager(this);
        homesManager = new HomesManager(this);
        warpsManager = new WarpsManager(this);
        playerDataManager = new PlayerDataManager(this);
        auditLogger = new AuditLogger(this);
        rateLimiter = new RateLimiter(this);
        tombstoneManager = new TombstoneManager(this);
        securityReviewManager = new SecurityReviewManager(this);
        combatManager = new CombatManager(this);
        encryptionUtil = new EncryptionUtil(getLogger(), getDataFolder());
        bankManager = new BankManager(this);
        permissionManager = new PermissionManager(this);

        getCommand("invsee").setExecutor(new InvseeCommand(this));
        getCommand("endersee").setExecutor(new EnderseeCommand(this));
        getCommand("teleport").setExecutor(new TeleportCommand(this));
        getCommand("tpa").setExecutor(new TpaCommand(this));
        getCommand("tpaccept").setExecutor(new TpacceptCommand(this));
        getCommand("tpdecline").setExecutor(new TpdeclineCommand(this));
        getCommand("guild").setExecutor(new GuildCommand(this));
        getCommand("heal").setExecutor(new HealCommand(this));
        getCommand("feed").setExecutor(new FeedCommand(this));
        getCommand("locks").setExecutor(new LocksCommand(this));
        getCommand("minigame").setExecutor(new MinigameCommand(this));
        getCommand("homes").setExecutor(new HomesCommand(this));
        getCommand("warps").setExecutor(new WarpsCommand(this));
        getCommand("tombstones").setExecutor(new TombstonesCommand(this));
        getCommand("security").setExecutor(new SecurityCommand(this, securityReviewManager));
        getCommand("bank").setExecutor(new BankCommand(this));
        getCommand("perm").setExecutor(new PermissionCommand(this));

        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new WalletListener(this), this);
        getServer().getPluginManager().registerEvents(new GuildListener(this), this);
        getServer().getPluginManager().registerEvents(new MinigameListener(this), this);
        getServer().getPluginManager().registerEvents(new WarpsListener(this), this);
        getServer().getPluginManager().registerEvents(new LocksListener(this), this);
        getServer().getPluginManager().registerEvents(new TombstoneListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new MessageListener(this), this);

        // Register bank interest listener for tracking day cycles and applying interest
        bankInterestListener = new BankInterestListener(this);
        getServer().getPluginManager().registerEvents(bankInterestListener, this);

        // Register container listener for hunger games
        containerRegistry = new ContainerRegistry(this);
        getServer().getPluginManager().registerEvents(new ContainerListener(this, containerRegistry), this);


        getLogger().info("Furious is enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (teleportManager != null) {
            teleportManager.shutdown();
        }
        if (walletManager != null) {
            walletManager.shutdown();
        }
        if (guildManager != null) {
            guildManager.shutdown();
        }
        if (locksManager != null) {
            locksManager.shutdown();
        }
        if (worldManager != null) {
            worldManager.shutdown();
        }
        if (minigameManager != null) {
            minigameManager.shutdown();
        }
        if (homesManager != null) {
            homesManager.shutdown();
        }
        if (warpsManager != null) {
            warpsManager.shutdown();
        }
        if (containerRegistry != null) {
            containerRegistry.shutdown();
        }
        if (tombstoneManager != null) {
            tombstoneManager.shutdown();
        }
        if (combatManager != null) {
            combatManager.shutdown();
        }
        if (bankInterestListener != null) {
            bankInterestListener.shutdown();
        }
        if (bankManager != null) {
            bankManager.shutdown();
        }
        if (permissionManager != null) {
            permissionManager.shutdown();
        }
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public WalletManager getWalletManager() {
        return walletManager;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    public LocksManager getLocksManager() {
        return locksManager;
    }

    public MinigameManager getMinigameManager() {
        return minigameManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public HomesManager getHomesManager() {
        return homesManager;
    }

    public WarpsManager getWarpsManager() {
        return warpsManager;
    }

    public ContainerRegistry getContainerRegistry() {
        return containerRegistry;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public AuditLogger getAuditLogger() {
        return auditLogger;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public TombstoneManager getTombstoneManager() {
        return tombstoneManager;
    }

    public SecurityReviewManager getSecurityReviewManager() {
        return securityReviewManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public EncryptionUtil getEncryptionUtil() {
        return encryptionUtil;
    }

    public BankManager getBankManager() {
        return bankManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public ItemStack createScrapItem(double amount) {
        // Use the cached currency configuration from the WalletManager
        String material = walletManager.getCurrencyMaterial();
        Material scrapMaterial = Material.getMaterial(material);
        if (scrapMaterial == null) {
            scrapMaterial = Material.IRON_INGOT;
        }
        ItemStack scrapItem = new ItemStack(scrapMaterial, 1);
        ItemMeta scrapMeta = scrapItem.getItemMeta();

        // Use the cached currency symbol and name
        String symbol = walletManager.getCurrencySymbol();
        String name = amount == 1 ? walletManager.getCurrencyName() : walletManager.getCurrencyPlural();

        scrapMeta.displayName(Component.text(symbol + " ").append(Component.text(amount, NamedTextColor.GOLD)).append(Component.text(" " + name)));
        scrapItem.setItemMeta(scrapMeta);
        return scrapItem;
    }

    public static Furious getInstance() {
        return instance;
    }

    /**
     * Saves default configuration files listed in the config.yml file.
     */
    private void saveDefaultConfigFiles() {
        List<String> configFiles = getConfig().getStringList("config-files");
        for (String configFile : configFiles) {
            if (!new File(getDataFolder(), configFile).exists()) {
                saveResource(configFile, false);
            }
        }
    }
}
