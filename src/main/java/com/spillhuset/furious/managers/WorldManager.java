package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages world operations for minigames and maps
 */
public class WorldManager {
    private final Furious plugin;
    private final String GAME_WORLD_NAME = "GameWorld";
    private final String GAME_BACKUP_NAME = "GameBackup";
    private final int WORLD_BORDER_SIZE = 2000;

    // Map system fields
    private final Map<String, Set<UUID>> mapEditors = new ConcurrentHashMap<>(); // Map name -> Set of editors
    private final Map<UUID, String> playerEditingMap = new ConcurrentHashMap<>(); // Player UUID -> Map being edited

    /**
     * Constructor for WorldManager
     *
     * @param plugin The plugin instance
     */
    public WorldManager(Furious plugin) {
        this.plugin = plugin;
        initializeWorlds();
    }

    /**
     * Initializes the game worlds
     */
    private void initializeWorlds() {
        plugin.getLogger().info("Initializing game worlds...");

        // Check if GameWorld exists, if not create it
        World gameWorld = Bukkit.getWorld(GAME_WORLD_NAME);
        if (gameWorld == null) {
            plugin.getLogger().info("Creating GameWorld...");
            WorldCreator creator = new WorldCreator(GAME_WORLD_NAME);
            gameWorld = creator.createWorld();

            if (gameWorld != null) {
                // Set world border
                setWorldBorder(gameWorld);
                plugin.getLogger().info("GameWorld created successfully!");
            } else {
                plugin.getLogger().severe("Failed to create GameWorld!");
            }
        } else {
            // Ensure world border is set
            setWorldBorder(gameWorld);
            plugin.getLogger().info("GameWorld already exists.");
        }

        // Check if GameBackup exists, if not create it by copying GameWorld
        World gameBackup = Bukkit.getWorld(GAME_BACKUP_NAME);
        if (gameBackup == null) {
            plugin.getLogger().info("Creating GameBackup from GameWorld...");
            if (gameWorld != null) {
                // Save GameWorld to ensure all changes are written to disk
                gameWorld.save();

                // Create GameBackup by copying GameWorld
                if (copyWorld(GAME_WORLD_NAME, GAME_BACKUP_NAME)) {
                    // Load the backup world
                    WorldCreator backupCreator = new WorldCreator(GAME_BACKUP_NAME);
                    gameBackup = backupCreator.createWorld();

                    if (gameBackup != null) {
                        // Set world border
                        setWorldBorder(gameBackup);
                        plugin.getLogger().info("GameBackup created successfully!");
                    } else {
                        plugin.getLogger().severe("Failed to load GameBackup!");
                    }
                } else {
                    plugin.getLogger().severe("Failed to create GameBackup!");
                }
            }
        } else {
            // Ensure world border is set
            setWorldBorder(gameBackup);
            plugin.getLogger().info("GameBackup already exists.");
        }
    }

    /**
     * Sets the world border for a world
     *
     * @param world The world to set the border for
     */
    private void setWorldBorder(World world) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(WORLD_BORDER_SIZE);
    }

    /**
     * Creates a playground world for a minigame by copying GameBackup
     *
     * @param gameName The name of the minigame
     * @return The created world, or null if creation failed
     */
    public World createPlayground(String gameName) {
        String playgroundName = gameName + "_playground";
        plugin.getLogger().info("Creating playground " + playgroundName + " from GameBackup...");

        // Check if the playground already exists
        World existingWorld = Bukkit.getWorld(playgroundName);
        if (existingWorld != null) {
            // Unload and delete the existing world
            if (!deletePlayground(gameName)) {
                plugin.getLogger().severe("Failed to delete existing playground!");
                return null;
            }
        }

        // Get the backup world
        World backupWorld = Bukkit.getWorld(GAME_BACKUP_NAME);
        if (backupWorld == null) {
            plugin.getLogger().severe("GameBackup world not found!");
            return null;
        }

        // Save the backup world to ensure all changes are written to disk
        backupWorld.save();

        // Copy the backup world to create the playground
        if (copyWorld(GAME_BACKUP_NAME, playgroundName)) {
            // Load the playground world
            WorldCreator creator = new WorldCreator(playgroundName);
            World playground = creator.createWorld();

            if (playground != null) {
                // Set world border
                setWorldBorder(playground);
                plugin.getLogger().info("Playground " + playgroundName + " created successfully!");
                return playground;
            } else {
                plugin.getLogger().severe("Failed to load playground " + playgroundName + "!");
            }
        } else {
            plugin.getLogger().severe("Failed to create playground " + playgroundName + "!");
        }

        return null;
    }

    /**
     * Deletes a playground world
     *
     * @param gameName The name of the minigame
     * @return True if deletion was successful, false otherwise
     */
    public boolean deletePlayground(String gameName) {
        String playgroundName = gameName + "_playground";
        plugin.getLogger().info("Deleting playground " + playgroundName + "...");

        // Get the playground world
        World playground = Bukkit.getWorld(playgroundName);
        if (playground != null) {
            // Teleport all players out of the world
            for (Player player : playground.getPlayers()) {
                player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            }

            // Unload the world
            if (Bukkit.unloadWorld(playground, false)) {
                // Delete the world directory
                File worldFolder = new File(Bukkit.getWorldContainer(), playgroundName);
                if (deleteDirectory(worldFolder)) {
                    plugin.getLogger().info("Playground " + playgroundName + " deleted successfully!");
                    return true;
                } else {
                    plugin.getLogger().severe("Failed to delete playground directory!");
                }
            } else {
                plugin.getLogger().severe("Failed to unload playground world!");
            }
        } else {
            // World doesn't exist, consider it deleted
            return true;
        }

        return false;
    }

    /**
     * Saves the GameWorld to GameBackup
     *
     * @param player The player who initiated the save (for feedback)
     * @return True if save was successful, false otherwise
     */
    public boolean saveGameWorldToBackup(Player player) {
        plugin.getLogger().info("Saving GameWorld to GameBackup...");

        // Get the game world
        World gameWorld = Bukkit.getWorld(GAME_WORLD_NAME);
        if (gameWorld == null) {
            if (player != null) {
                player.sendMessage(Component.text("GameWorld not found!", NamedTextColor.RED));
            }
            return false;
        }

        // Save the game world to ensure all changes are written to disk
        gameWorld.save();

        // Get the backup world
        World backupWorld = Bukkit.getWorld(GAME_BACKUP_NAME);
        if (backupWorld != null) {
            // Teleport all players out of the backup world
            for (Player p : backupWorld.getPlayers()) {
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }

            // Unload the backup world
            if (!Bukkit.unloadWorld(backupWorld, false)) {
                if (player != null) {
                    player.sendMessage(Component.text("Failed to unload GameBackup world!", NamedTextColor.RED));
                }
                return false;
            }
        }

        // Delete the backup world directory
        File backupFolder = new File(Bukkit.getWorldContainer(), GAME_BACKUP_NAME);
        if (backupFolder.exists() && !deleteDirectory(backupFolder)) {
            if (player != null) {
                player.sendMessage(Component.text("Failed to delete GameBackup directory!", NamedTextColor.RED));
            }
            return false;
        }

        // Copy the game world to create the backup
        if (copyWorld(GAME_WORLD_NAME, GAME_BACKUP_NAME)) {
            // Load the backup world
            WorldCreator creator = new WorldCreator(GAME_BACKUP_NAME);
            backupWorld = creator.createWorld();

            if (backupWorld != null) {
                // Set world border
                setWorldBorder(backupWorld);
                if (player != null) {
                    player.sendMessage(Component.text("GameWorld saved to GameBackup successfully!", NamedTextColor.GREEN));
                }
                plugin.getLogger().info("GameWorld saved to GameBackup successfully!");
                return true;
            } else {
                if (player != null) {
                    player.sendMessage(Component.text("Failed to load GameBackup world!", NamedTextColor.RED));
                }
                plugin.getLogger().severe("Failed to load GameBackup world!");
            }
        } else {
            if (player != null) {
                player.sendMessage(Component.text("Failed to copy GameWorld to GameBackup!", NamedTextColor.RED));
            }
            plugin.getLogger().severe("Failed to copy GameWorld to GameBackup!");
        }

        return false;
    }

    /**
     * Teleports a player to the GameWorld
     *
     * @param player The player to teleport
     * @return True if teleport was successful, false otherwise
     */
    public boolean teleportToGameWorld(Player player) {
        // Get the game world
        World gameWorld = Bukkit.getWorld(GAME_WORLD_NAME);
        if (gameWorld == null) {
            player.sendMessage(Component.text("GameWorld not found!", NamedTextColor.RED));
            return false;
        }

        // Teleport the player to the game world
        player.teleport(gameWorld.getSpawnLocation());
        player.sendMessage(Component.text("Teleported to GameWorld!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Copies a world directory
     *
     * @param sourceName The name of the source world
     * @param targetName The name of the target world
     * @return True if copy was successful, false otherwise
     */
    private boolean copyWorld(String sourceName, String targetName) {
        File sourceFolder = new File(Bukkit.getWorldContainer(), sourceName);
        File targetFolder = new File(Bukkit.getWorldContainer(), targetName);

        if (!sourceFolder.exists()) {
            plugin.getLogger().severe("Source world folder does not exist: " + sourceFolder.getAbsolutePath());
            return false;
        }

        // Create target folder if it doesn't exist
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
            plugin.getLogger().severe("Failed to create target world folder: " + targetFolder.getAbsolutePath());
            return false;
        }

        try {
            // Copy all files and directories
            copyDirectory(sourceFolder, targetFolder);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error copying world", e);
            return false;
        }
    }

    /**
     * Copies a directory recursively
     *
     * @param source The source directory
     * @param target The target directory
     * @throws IOException If an I/O error occurs
     */
    private void copyDirectory(File source, File target) throws IOException {
        // Skip session.lock file to avoid world corruption
        if (source.getName().equals("session.lock")) {
            return;
        }

        if (source.isDirectory()) {
            // Create the target directory if it doesn't exist
            if (!target.exists() && !target.mkdirs()) {
                throw new IOException("Failed to create directory: " + target.getAbsolutePath());
            }

            // Copy each file/directory in the source directory
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File sourceFile = new File(source, file);
                    File targetFile = new File(target, file);

                    // Skip session.lock and uid.dat files
                    if (file.equals("session.lock") || file.equals("uid.dat")) {
                        continue;
                    }

                    copyDirectory(sourceFile, targetFile);
                }
            }
        } else {
            // Copy the file
            try (FileInputStream in = new FileInputStream(source);
                 FileOutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        }
    }

    /**
     * Deletes a directory recursively
     *
     * @param directory The directory to delete
     * @return True if deletion was successful, false otherwise
     */
    private boolean deleteDirectory(File directory) {
        if (!directory.exists()) {
            return true;
        }

        try {
            // Walk through the directory and delete all files and subdirectories
            Files.walk(directory.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error deleting directory", e);
            return false;
        }
    }

    /**
     * Shuts down the WorldManager
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down WorldManager...");

        // Clean up any remaining edit copies
        for (String mapName : new HashSet<>(mapEditors.keySet())) {
            plugin.getLogger().info("Cleaning up edit copy for map " + mapName + "...");
            deleteMapEditCopy(mapName);
        }

        // Clear the editor maps
        mapEditors.clear();
        playerEditingMap.clear();
    }

    /**
     * Gets the GameWorld name
     *
     * @return The GameWorld name
     */
    public String getGameWorldName() {
        return GAME_WORLD_NAME;
    }

    /**
     * Gets the GameBackup name
     *
     * @return The GameBackup name
     */
    public String getGameBackupName() {
        return GAME_BACKUP_NAME;
    }

    /**
     * Gets the backup name for a map
     *
     * @param mapName The name of the map
     * @return The backup name for the map
     */
    public String getMapBackupName(String mapName) {
        return mapName + "Backup";
    }

    /**
     * Gets the edit name for a map
     *
     * @param mapName The name of the map
     * @return The edit name for the map
     */
    public String getMapEditName(String mapName) {
        return mapName + "Map";
    }

    /**
     * Gets the minigame instance name for a map
     *
     * @param mapName  The name of the map
     * @param uniqueId A unique identifier for the instance
     * @return The minigame instance name for the map
     */
    public String getMapInstanceName(String mapName, String uniqueId) {
        return mapName + "-" + uniqueId;
    }

    /**
     * Checks if a backup exists for a map
     *
     * @param mapName The name of the map
     * @return True if a backup exists, false otherwise
     */
    public boolean hasMapBackup(String mapName) {
        String backupName = getMapBackupName(mapName);
        World backupWorld = Bukkit.getWorld(backupName);
        if (backupWorld != null) {
            return true;
        }

        File backupFolder = new File(Bukkit.getWorldContainer(), backupName);
        return backupFolder.exists();
    }

    /**
     * Creates a backup of a map if it doesn't exist
     *
     * @param mapName The name of the map
     * @return True if the backup was created or already exists, false otherwise
     */
    public boolean createMapBackupIfNeeded(String mapName) {
        if (hasMapBackup(mapName)) {
            return true;
        }

        // Check if the map exists
        World mapWorld = Bukkit.getWorld(mapName);
        if (mapWorld == null) {
            File mapFolder = new File(Bukkit.getWorldContainer(), mapName);
            if (!mapFolder.exists()) {
                plugin.getLogger().severe("Map " + mapName + " does not exist!");
                return false;
            }

            // Load the map world
            WorldCreator creator = new WorldCreator(mapName);
            mapWorld = creator.createWorld();
            if (mapWorld == null) {
                plugin.getLogger().severe("Failed to load map " + mapName + "!");
                return false;
            }
        }

        // Save the map world to ensure all changes are written to disk
        mapWorld.save();

        // Create the backup by copying the map
        String backupName = getMapBackupName(mapName);
        if (copyWorld(mapName, backupName)) {
            // Load the backup world
            WorldCreator creator = new WorldCreator(backupName);
            World backupWorld = creator.createWorld();

            if (backupWorld != null) {
                // Set world border
                setWorldBorder(backupWorld);
                plugin.getLogger().info("Map backup " + backupName + " created successfully!");
                return true;
            } else {
                plugin.getLogger().severe("Failed to load map backup " + backupName + "!");
            }
        } else {
            plugin.getLogger().severe("Failed to create map backup " + backupName + "!");
        }

        return false;
    }

    /**
     * Checks if an editable copy exists for a map
     *
     * @param mapName The name of the map
     * @return True if an editable copy exists, false otherwise
     */
    public boolean hasMapEditCopy(String mapName) {
        String editName = getMapEditName(mapName);
        World editWorld = Bukkit.getWorld(editName);
        if (editWorld != null) {
            return true;
        }

        File editFolder = new File(Bukkit.getWorldContainer(), editName);
        return editFolder.exists();
    }

    /**
     * Creates an editable copy of a map from its backup
     *
     * @param mapName The name of the map
     * @return True if the editable copy was created successfully, false otherwise
     */
    public boolean createMapEditCopy(String mapName) {
        // Ensure the backup exists
        if (!createMapBackupIfNeeded(mapName)) {
            return false;
        }

        // Check if the edit copy already exists
        if (hasMapEditCopy(mapName)) {
            return true;
        }

        // Create the edit copy by copying the backup
        String backupName = getMapBackupName(mapName);
        String editName = getMapEditName(mapName);

        if (copyWorld(backupName, editName)) {
            // Load the edit world
            WorldCreator creator = new WorldCreator(editName);
            World editWorld = creator.createWorld();

            if (editWorld != null) {
                // Set world border
                setWorldBorder(editWorld);
                plugin.getLogger().info("Map edit copy " + editName + " created successfully!");
                return true;
            } else {
                plugin.getLogger().severe("Failed to load map edit copy " + editName + "!");
            }
        } else {
            plugin.getLogger().severe("Failed to create map edit copy " + editName + "!");
        }

        return false;
    }

    /**
     * Adds a player as an editor of a map
     *
     * @param player  The player
     * @param mapName The name of the map
     * @return True if the player was added as an editor, false otherwise
     */
    public boolean addMapEditor(Player player, String mapName) {
        // Check if the player is already editing a map
        if (playerEditingMap.containsKey(player.getUniqueId())) {
            String currentMap = playerEditingMap.get(player.getUniqueId());
            if (currentMap.equals(mapName)) {
                // Player is already editing this map
                return true;
            } else {
                // Player is editing a different map
                player.sendMessage(Component.text("You are already editing map " + currentMap + "!", NamedTextColor.RED));
                return false;
            }
        }

        // Create the edit copy if it doesn't exist
        if (!hasMapEditCopy(mapName)) {
            if (!createMapEditCopy(mapName)) {
                player.sendMessage(Component.text("Failed to create edit copy of map " + mapName + "!", NamedTextColor.RED));
                return false;
            }
        }

        // Add the player to the editors set
        mapEditors.computeIfAbsent(mapName, k -> ConcurrentHashMap.newKeySet()).add(player.getUniqueId());
        playerEditingMap.put(player.getUniqueId(), mapName);

        // Teleport the player to the edit world
        String editName = getMapEditName(mapName);
        World editWorld = Bukkit.getWorld(editName);
        if (editWorld != null) {
            player.teleport(editWorld.getSpawnLocation());
            player.sendMessage(Component.text("You are now editing map " + mapName + "!", NamedTextColor.GREEN));
            return true;
        } else {
            player.sendMessage(Component.text("Failed to teleport to edit world for map " + mapName + "!", NamedTextColor.RED));
            return false;
        }
    }

    /**
     * Removes a player as an editor of a map
     *
     * @param player      The player
     * @param saveChanges Whether to save changes to the backup
     * @return True if the player was removed as an editor, false otherwise
     */
    public boolean removeMapEditor(Player player, boolean saveChanges) {
        // Check if the player is editing a map
        if (!playerEditingMap.containsKey(player.getUniqueId())) {
            return false;
        }

        String mapName = playerEditingMap.get(player.getUniqueId());
        Set<UUID> editors = mapEditors.get(mapName);

        if (editors != null) {
            // Remove the player from the editors set
            editors.remove(player.getUniqueId());

            // If this was the last editor and saveChanges is true, save the changes
            if (editors.isEmpty() && saveChanges) {
                saveMapChanges(mapName, player);
            }

            // If there are no more editors, remove the map from the editors map
            if (editors.isEmpty()) {
                mapEditors.remove(mapName);
            }
        }

        // Remove the player from the playerEditingMap
        playerEditingMap.remove(player.getUniqueId());

        // Teleport the player to the main world
        player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());

        if (saveChanges) {
            player.sendMessage(Component.text("You have saved your changes to map " + mapName + "!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("You have stopped editing map " + mapName + " without saving changes.", NamedTextColor.YELLOW));
        }

        return true;
    }

    /**
     * Gets the set of editors for a map
     *
     * @param mapName The name of the map
     * @return The set of editors for the map, or an empty set if none
     */
    public Set<UUID> getMapEditors(String mapName) {
        return mapEditors.getOrDefault(mapName, Collections.emptySet());
    }

    /**
     * Checks if a player is editing a map
     *
     * @param player The player
     * @return True if the player is editing a map, false otherwise
     */
    public boolean isPlayerEditingMap(Player player) {
        return playerEditingMap.containsKey(player.getUniqueId());
    }

    /**
     * Gets the name of the map a player is editing
     *
     * @param player The player
     * @return The name of the map the player is editing, or null if none
     */
    public String getPlayerEditingMap(Player player) {
        return playerEditingMap.get(player.getUniqueId());
    }

    /**
     * Saves changes from an editable copy of a map to its backup
     *
     * @param mapName The name of the map
     * @param player  The player who initiated the save (for feedback)
     * @return True if the changes were saved successfully, false otherwise
     */
    public boolean saveMapChanges(String mapName, Player player) {
        String editName = getMapEditName(mapName);
        String backupName = getMapBackupName(mapName);

        // Check if the edit copy exists
        World editWorld = Bukkit.getWorld(editName);
        if (editWorld == null) {
            if (player != null) {
                player.sendMessage(Component.text("Edit copy of map " + mapName + " not found!", NamedTextColor.RED));
            }
            plugin.getLogger().severe("Edit copy of map " + mapName + " not found!");
            return false;
        }

        // Save the edit world to ensure all changes are written to disk
        editWorld.save();

        // Get the backup world
        World backupWorld = Bukkit.getWorld(backupName);
        if (backupWorld != null) {
            // Teleport all players out of the backup world
            for (Player p : backupWorld.getPlayers()) {
                p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            }

            // Unload the backup world
            if (!Bukkit.unloadWorld(backupWorld, false)) {
                if (player != null) {
                    player.sendMessage(Component.text("Failed to unload backup world for map " + mapName + "!", NamedTextColor.RED));
                }
                plugin.getLogger().severe("Failed to unload backup world for map " + mapName + "!");
                return false;
            }
        }

        // Delete the backup world directory
        File backupFolder = new File(Bukkit.getWorldContainer(), backupName);
        if (backupFolder.exists() && !deleteDirectory(backupFolder)) {
            if (player != null) {
                player.sendMessage(Component.text("Failed to delete backup directory for map " + mapName + "!", NamedTextColor.RED));
            }
            plugin.getLogger().severe("Failed to delete backup directory for map " + mapName + "!");
            return false;
        }

        // Copy the edit world to create the backup
        if (copyWorld(editName, backupName)) {
            // Load the backup world
            WorldCreator creator = new WorldCreator(backupName);
            backupWorld = creator.createWorld();

            if (backupWorld != null) {
                // Set world border
                setWorldBorder(backupWorld);

                // Check if there are no more editors
                if (!mapEditors.containsKey(mapName) || mapEditors.get(mapName).isEmpty()) {
                    // Delete the edit world
                    deleteMapEditCopy(mapName);
                }

                if (player != null) {
                    player.sendMessage(Component.text("Changes to map " + mapName + " saved successfully!", NamedTextColor.GREEN));
                }
                plugin.getLogger().info("Changes to map " + mapName + " saved successfully!");
                return true;
            } else {
                if (player != null) {
                    player.sendMessage(Component.text("Failed to load backup world for map " + mapName + "!", NamedTextColor.RED));
                }
                plugin.getLogger().severe("Failed to load backup world for map " + mapName + "!");
            }
        } else {
            if (player != null) {
                player.sendMessage(Component.text("Failed to copy edit world to backup for map " + mapName + "!", NamedTextColor.RED));
            }
            plugin.getLogger().severe("Failed to copy edit world to backup for map " + mapName + "!");
        }

        return false;
    }

    /**
     * Deletes the editable copy of a map
     *
     * @param mapName The name of the map
     * @return True if the editable copy was deleted successfully, false otherwise
     */
    public boolean deleteMapEditCopy(String mapName) {
        String editName = getMapEditName(mapName);

        // Get the edit world
        World editWorld = Bukkit.getWorld(editName);
        if (editWorld != null) {
            // Teleport all players out of the edit world
            for (Player player : editWorld.getPlayers()) {
                player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
                player.sendMessage(Component.text("The edit world for map " + mapName + " is being deleted.", NamedTextColor.YELLOW));
            }

            // Unload the edit world
            if (!Bukkit.unloadWorld(editWorld, false)) {
                plugin.getLogger().severe("Failed to unload edit world for map " + mapName + "!");
                return false;
            }
        }

        // Delete the edit world directory
        File editFolder = new File(Bukkit.getWorldContainer(), editName);
        if (editFolder.exists() && !deleteDirectory(editFolder)) {
            plugin.getLogger().severe("Failed to delete edit directory for map " + mapName + "!");
            return false;
        }

        plugin.getLogger().info("Edit copy of map " + mapName + " deleted successfully!");
        return true;
    }

    /**
     * Creates a temporary copy of a map for a minigame
     *
     * @param mapName  The name of the map
     * @param uniqueId A unique identifier for the instance
     * @return The created world, or null if creation failed
     */
    public World createMapInstance(String mapName, String uniqueId) {
        // Ensure the backup exists
        if (!createMapBackupIfNeeded(mapName)) {
            plugin.getLogger().severe("Failed to create backup for map " + mapName + "!");
            return null;
        }

        String backupName = getMapBackupName(mapName);
        String instanceName = getMapInstanceName(mapName, uniqueId);

        plugin.getLogger().info("Creating instance " + instanceName + " from " + backupName + "...");

        // Check if the instance already exists
        World existingWorld = Bukkit.getWorld(instanceName);
        if (existingWorld != null) {
            // Unload and delete the existing world
            for (Player player : existingWorld.getPlayers()) {
                player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            }

            if (!Bukkit.unloadWorld(existingWorld, false)) {
                plugin.getLogger().severe("Failed to unload existing instance " + instanceName + "!");
                return null;
            }

            File instanceFolder = new File(Bukkit.getWorldContainer(), instanceName);
            if (instanceFolder.exists() && !deleteDirectory(instanceFolder)) {
                plugin.getLogger().severe("Failed to delete existing instance directory " + instanceName + "!");
                return null;
            }
        }

        // Get the backup world
        World backupWorld = Bukkit.getWorld(backupName);
        if (backupWorld == null) {
            plugin.getLogger().severe("Backup world " + backupName + " not found!");
            return null;
        }

        // Save the backup world to ensure all changes are written to disk
        backupWorld.save();

        // Copy the backup world to create the instance
        if (copyWorld(backupName, instanceName)) {
            // Load the instance world
            WorldCreator creator = new WorldCreator(instanceName);
            World instanceWorld = creator.createWorld();

            if (instanceWorld != null) {
                // Set world border
                setWorldBorder(instanceWorld);
                plugin.getLogger().info("Instance " + instanceName + " created successfully!");
                return instanceWorld;
            } else {
                plugin.getLogger().severe("Failed to load instance " + instanceName + "!");
            }
        } else {
            plugin.getLogger().severe("Failed to create instance " + instanceName + "!");
        }

        return null;
    }

    /**
     * Deletes a temporary copy of a map
     *
     * @param mapName  The name of the map
     * @param uniqueId The unique identifier for the instance
     * @return True if the instance was deleted successfully, false otherwise
     */
    public boolean deleteMapInstance(String mapName, String uniqueId) {
        String instanceName = getMapInstanceName(mapName, uniqueId);

        plugin.getLogger().info("Deleting instance " + instanceName + "...");

        // Get the instance world
        World instanceWorld = Bukkit.getWorld(instanceName);
        if (instanceWorld != null) {
            // Teleport all players out of the instance world
            for (Player player : instanceWorld.getPlayers()) {
                player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            }

            // Unload the instance world
            if (!Bukkit.unloadWorld(instanceWorld, false)) {
                plugin.getLogger().severe("Failed to unload instance " + instanceName + "!");
                return false;
            }
        } else {
            // World doesn't exist, consider it deleted
            return true;
        }

        // Delete the instance world directory
        File instanceFolder = new File(Bukkit.getWorldContainer(), instanceName);
        if (instanceFolder.exists() && !deleteDirectory(instanceFolder)) {
            plugin.getLogger().severe("Failed to delete instance directory " + instanceName + "!");
            return false;
        }

        plugin.getLogger().info("Instance " + instanceName + " deleted successfully!");
        return true;
    }

    /**
     * Sets a new spawn location for a world and updates the worldborder center
     *
     * @param world  The world to set the spawn for
     * @param player The player whose location will be used as the new spawn and worldborder center
     * @return True if setting the spawn was successful, false otherwise
     */
    public boolean setWorldSpawn(World world, Player player) {
        if (world == null || player == null) {
            return false;
        }

        try {
            // Set the world's spawn location to the player's current location
            world.setSpawnLocation(player.getLocation());

            // Update the worldborder center to match the new spawn location
            WorldBorder border = world.getWorldBorder();
            border.setCenter(player.getLocation());
            border.setSize(WORLD_BORDER_SIZE);

            // Inform the player
            player.sendMessage(Component.text("Set " + world.getName() + "'s spawn location and worldborder center to your current position.", NamedTextColor.GREEN));
            plugin.getLogger().info("Set " + world.getName() + "'s spawn location and worldborder center to " + player.getLocation());

            return true;
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to set spawn location for " + world.getName() + "!", NamedTextColor.RED));
            plugin.getLogger().log(Level.SEVERE, "Error setting spawn location", e);
            return false;
        }
    }
}
