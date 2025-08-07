package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.TeleportTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeleportManager {
    private final Furious plugin;
    private final Map<UUID, Set<UUID>> incomingRequests; // target -> set of requesters
    private final Map<UUID, UUID> outgoingRequests; // requester -> target
    private final Set<UUID> denyAll; // players who deny all requests
    private final File configFile;
    private FileConfiguration config;
    private final Map<UUID, TeleportTask> teleportTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> preTeleportTasks = new HashMap<>(); // players in pre-teleport state
    private final int TELEPORT_DELAY = 10; // seconds
    private final int PRE_TELEPORT_DELAY = 5; // seconds

    public TeleportManager(Furious plugin) {
        this.plugin = plugin;
        this.incomingRequests = new HashMap<>();
        this.outgoingRequests = new HashMap<>();
        this.denyAll = new HashSet<>();
        this.configFile = new File(plugin.getDataFolder(), "teleport.yml");
        loadConfiguration();
    }

    public void teleportQueue(Player player, Location destination) {
        // Cancel any existing teleport task without showing a message
        cancelTeleportTask(player, false);

        // Create a new teleport task
        TeleportTask task = new TeleportTask(player, destination, TELEPORT_DELAY, plugin);
        teleportTasks.put(player.getUniqueId(), task);
        task.start();
    }

    public void cancelTeleportTask(Player player) {
        cancelTeleportTask(player, true);
    }

    public void cancelTeleportTask(Player player, boolean showMessage) {
        TeleportTask task = teleportTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            if (showMessage) {
                player.sendMessage(Component.text("Teleport cancelled!", NamedTextColor.RED));
            }
        }
    }

    /**
     * Cancels a pre-teleport task for a player.
     *
     * @param player The player whose pre-teleport task should be cancelled
     */
    public void cancelPreTeleportTask(Player player) {
        BukkitTask task = preTeleportTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            player.sendMessage(Component.text("Teleport preparation cancelled!", NamedTextColor.RED));
        }
    }

    public boolean isPlayerTeleporting(Player player) {
        return teleportTasks.containsKey(player.getUniqueId());
    }

    /**
     * Checks if a player is in the pre-teleport state (waiting for the teleport countdown to begin).
     *
     * @param player The player to check
     * @return True if the player is in the pre-teleport state, false otherwise
     */
    public boolean isPlayerInPreTeleport(Player player) {
        return preTeleportTasks.containsKey(player.getUniqueId());
    }


    /**
     * Teleport a player to a specific location.
     *
     * @param target The player to teleport.
     * @param sender The sender of the command. Can be null.
     * @param args   The command arguments.
     * @return True if the teleport was successful, false otherwise.
     */
    public boolean teleportCoords(Player target, CommandSender sender, String[] args) {
        try {
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);
            World world = args.length > 4 && args[4] != null ? Bukkit.getWorld(args[4]) : target.getWorld();
            if (world == null) {
                (sender != null ? sender : target).sendMessage(Component.text("World not found!", NamedTextColor.RED));
                return true;
            }

            Location location = new Location(world, x, y, z);
            if (plugin.getTeleportManager().forceTeleport(target, location)) {
                Component message = Component.text("Teleported " + (sender == target ? "to" : target.getName() + " to") + String.format(" %.1f, %.1f, %.1f in %s", x, y, z, world.getName()), NamedTextColor.GREEN);

                Objects.requireNonNullElse(sender, target).sendMessage(message);
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            (sender != null ? sender : target).sendMessage(Component.text("Invalid coordinates!", NamedTextColor.RED));
            return true;
        }
    }

    /**
     * Forcefully teleport a player to a location.
     *
     * @param target   The player to teleport. This player will be teleported even if they have a pending request.
     * @param location The location to teleport the player to. This location will be used even if the player has a pending request.
     * @return True if the teleport was successful, false otherwise.
     */
    public boolean forceTeleport(Player target, Location location) {
        return target.teleport(location);
    }

    /**
     * Teleport a player to another player.
     */
    private void loadConfiguration() {
        if (!configFile.exists()) {
            plugin.saveResource("teleport.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load the deny-all list from config
        List<String> denyList = config.getStringList("deny-all");
        for (String uuidStr : denyList) {
            try {
                denyAll.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in teleport.yml: " + uuidStr);
            }
        }
    }

    /**
     * Save the current configuration to disk.
     */
    private void saveConfiguration() {
        // Save the deny-all list to config
        List<String> denyList = new ArrayList<>();
        for (UUID uuid : denyAll) {
            denyList.add(uuid.toString());
        }
        config.set("deny-all", denyList);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save teleport preferences: " + e.getMessage());
        }
    }

    /**
     * Check if the world is disabled for teleportation.
     *
     * @param worldUid The UUID of the world to check.
     * @return True if the world is disabled, false otherwise.
     */
    public boolean isWorldDisabled(UUID worldUid) {
        List<String> disabledWorlds = config.getStringList("disabled-worlds");
        return disabledWorlds.contains(worldUid.toString());
    }


    /**
     * Add a world to the list of disabled worlds.
     *
     * @param world The world to add.
     */
    public void addDisabledWorld(World world) {
        List<String> disabledWorlds = new ArrayList<>(config.getStringList("disabled-worlds"));
        String worldUid = world.getUID().toString();

        if (!disabledWorlds.contains(worldUid)) {
            disabledWorlds.add(worldUid);
            config.set("disabled-worlds", disabledWorlds);
            saveConfiguration();
        }
    }

    /**
     * Remove a world from the list of disabled worlds.
     *
     * @param world The world to remove.
     */
    public void removeDisabledWorld(World world) {
        List<String> disabledWorlds = new ArrayList<>(config.getStringList("disabled-worlds"));
        String worldUid = world.getUID().toString();

        if (disabledWorlds.remove(worldUid)) {
            config.set("disabled-worlds", disabledWorlds);
            saveConfiguration();
        }
    }

    /**
     * Get a map of world names to their UUIDs.
     *
     * @return A map of world names with their UUIDs.
     */
    public Map<String, UUID> getWorldUUIDs() {
        Map<String, UUID> worldUUIDs = new HashMap<>();
        for (World world : plugin.getServer().getWorlds()) {
            worldUUIDs.put(world.getName(), world.getUID());
        }
        return worldUUIDs;
    }

    /**
     * Get a message to display when a player tries to teleport to a player in a different world.
     *
     * @return A message to display when a player tries to teleport to a player in a different world.
     */
    private Component getCrossWorldMessage() {
        return Component.text(config.getString("cross-world-message", "You can only teleport to players in the same world!").replace("&", "§"), NamedTextColor.RED);
    }

    /**
     * Get a message to display when a player tries to teleport in a disabled world.
     *
     * @return A message to display when a player tries to teleport in a disabled world.
     */
    private Component getDisabledWorldMessage() {
        return Component.text(config.getString("disabled-world-message", "Teleportation is disabled in this world!").replace("&", "§"), NamedTextColor.RED);
    }

    /**
     * Check if the same-world-only config option is enabled.
     *
     * @return True if the same-world-only config option is enabled, false otherwise.
     */
    private boolean isSameWorldOnly() {
        return config.getBoolean("same-world-only", true);
    }

    /**
     * Send a teleport request to another player.
     *
     * @param requester The player who is sending the request.
     * @param target    The player who is being requested to teleport to.
     * @return True if the request was sent successfully, false otherwise.
     */
    public boolean sendRequest(Player requester, Player target) {
        // Check if the target is an op (ops cannot be teleported to)
        if (target.isOp()) {
            requester.sendMessage(Component.text("You cannot send teleport requests to ops!", NamedTextColor.RED));
            return false;
        }

        // Check if the requester is in a disabled world
        if (isWorldDisabled(requester.getWorld().getUID())) {
            requester.sendMessage(getDisabledWorldMessage());
            return false;
        }

        // Check if the target is in a disabled world
        if (isWorldDisabled(target.getWorld().getUID())) {
            requester.sendMessage(Component.text("You cannot teleport to players in that world!", NamedTextColor.RED));
            return false;
        }

        // Check if they're in different worlds and same-world-only is enabled
        if (isSameWorldOnly() && !requester.getWorld().getUID().equals(target.getWorld().getUID())) {
            requester.sendMessage(getCrossWorldMessage());
            return false;
        }

        // Check if the requester has denied all requests
        if (denyAll.contains(target.getUniqueId())) {
            return false;
        }

        // Check if the requester already has an outgoing request
        if (outgoingRequests.containsKey(requester.getUniqueId())) {
            requester.sendMessage(Component.text("You already have an outgoing teleport request. Cancel it first.", NamedTextColor.RED));
            return false;
        }

        // Check if the requester is in the pre-teleport state
        if (isPlayerInPreTeleport(requester)) {
            requester.sendMessage(Component.text("You are preparing for teleport. Please wait.", NamedTextColor.RED));
            return false;
        }

        // Check if the requester is already teleporting
        if (isPlayerTeleporting(requester)) {
            requester.sendMessage(Component.text("You are already teleporting. Please wait.", NamedTextColor.RED));
            return false;
        }

        // Check if the target has already received a request from the requester
        incomingRequests.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>()).add(requester.getUniqueId());
        // Add outgoing request (e.g., 60 seconds)
        outgoingRequests.put(requester.getUniqueId(), target.getUniqueId());

        // Add request expiry (e.g., 60 seconds)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (hasOutgoingRequest(requester)) {
                // Store target UUID before canceling the request
                UUID targetUUID = outgoingRequests.get(requester.getUniqueId());
                Player targetPlayer = plugin.getServer().getPlayer(targetUUID);

                // Cancel the request
                cancelRequest(requester);

                // Notify requester
                requester.sendMessage(Component.text("Your teleport request has expired.", NamedTextColor.YELLOW));

                // Notify target if they're online
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    targetPlayer.sendMessage(Component.text("Teleport request from " + requester.getName() + " has expired.", NamedTextColor.YELLOW));
                }
            }
        }, 20 * 60);

        return true;
    }

    /**
     * Accept a teleport request from another player.
     *
     * @param target    The player who is being teleported to.
     * @param requester The player who is sending the request.
     */
    public void acceptRequest(Player target, Player requester) {
        if (hasIncomingRequest(target, requester)) {
            // Remove request from incoming map
            removeRequest(requester, target);

            // Notify the requester that they have time to prepare (acceptance message is handled in AcceptSubCommand)
            requester.sendMessage(Component.text("You have " + PRE_TELEPORT_DELAY + " seconds to prepare. Don't move to avoid cancellation.", NamedTextColor.YELLOW));

            // Notify the target that the requester will be teleported to them
            target.sendMessage(Component.text(requester.getName() + " will be teleported to you in " + PRE_TELEPORT_DELAY + " seconds.", NamedTextColor.YELLOW));

            // Schedule the teleport after the pre-teleport delay
            BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // Remove from pre-teleport state
                preTeleportTasks.remove(requester.getUniqueId());

                // Start the teleportation process with countdown
                teleportQueue(requester, target.getLocation());

                // Notify the requester that the teleport process is starting
                requester.sendMessage(Component.text("Teleport sequence initiated. Don't move!", NamedTextColor.YELLOW));
            }, PRE_TELEPORT_DELAY * 20); // 20 ticks = 1 second

            // Store the task to be able to cancel it if needed
            preTeleportTasks.put(requester.getUniqueId(), task);
        }
    }

    /**
     * Decline a teleport request from another player.
     *
     * @param target    The player who is being teleported to.
     * @param requester The player who is sending the request.
     */
    public void declineRequest(Player target, Player requester) {
        removeRequest(requester, target);

        // Also cancel any pre-teleport task
        cancelPreTeleportTask(requester);

        // Feedback messages are now handled in DeclineSubCommand
    }

    /**
     * Cancel a teleport request from another player.
     *
     * @param requester The player who is sending the request.
     */
    public void cancelRequest(Player requester) {
        UUID targetUUID = outgoingRequests.get(requester.getUniqueId());
        if (targetUUID != null) {
            removeRequest(requester, plugin.getServer().getPlayer(targetUUID));
        }

        // Also cancel any pre-teleport task
        cancelPreTeleportTask(requester);
    }

    /**
     * Toggle the deny-all option for a player.
     *
     * @param player The player to toggle the deny-all option for.
     */
    public void toggleDenyAll(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (denyAll.contains(playerUUID)) {
            denyAll.remove(playerUUID);
        } else {
            denyAll.add(playerUUID);
        }
        saveConfiguration();
    }

    /**
     * Check if the deny-all option is enabled for a player.
     *
     * @param player The player to check the deny-all option for.
     * @return True if the deny-all option is enabled for the player, false otherwise.
     */
    public boolean isDenyingAll(Player player) {
        return denyAll.contains(player.getUniqueId());
    }

    /**
     * Get a set of all players who have sent a request to the specified player.
     *
     * @param player The player to get the incoming requests for.
     * @return A set of all players who have sent a request to the specified player.
     */
    public Set<UUID> getIncomingRequests(Player player) {
        return incomingRequests.getOrDefault(player.getUniqueId(), new HashSet<>());
    }

    /**
     * Get the target player for a request sent by a player.
     *
     * @param player The player to get the target player for.
     * @return The target player for a request sent by a player, or null if the player has no outgoing requests.
     */
    public UUID getOutgoingRequest(Player player) {
        return outgoingRequests.get(player.getUniqueId());
    }

    /**
     * Check if a player has an incoming request from another player.
     *
     * @param target    The player to check for incoming requests.
     * @param requester The player who sent the request.
     * @return True if the player has an incoming request from another player, false otherwise.
     */
    public boolean hasIncomingRequest(Player target, Player requester) {
        Set<UUID> requests = incomingRequests.get(target.getUniqueId());
        return requests != null && requests.contains(requester.getUniqueId());
    }

    /**
     * Check if a player has an outgoing request to another player.
     *
     * @param requester The player to check for outgoing requests.
     * @return True if the player has an outgoing request to another player, false otherwise.
     */
    public boolean hasOutgoingRequest(Player requester) {
        return outgoingRequests.containsKey(requester.getUniqueId());
    }

    /**
     * Remove a request from the request map.
     *
     * @param requester The player who sent the request.
     * @param target    The player who is being requested to teleport to.
     */
    private void removeRequest(Player requester, Player target) {
        if (target != null) {
            Set<UUID> requests = incomingRequests.get(target.getUniqueId());
            if (requests != null) {
                requests.remove(requester.getUniqueId());
                if (requests.isEmpty()) {
                    incomingRequests.remove(target.getUniqueId());
                }
            }
        }
        outgoingRequests.remove(requester.getUniqueId());
    }

    public void shutdown() {
        saveConfiguration();

        // Cancel all pending teleport tasks
        for (TeleportTask task : teleportTasks.values()) {
            task.cancel();
        }
        teleportTasks.clear();

        // Cancel all pending pre-teleport tasks
        for (BukkitTask task : preTeleportTasks.values()) {
            task.cancel();
        }
        preTeleportTasks.clear();

        incomingRequests.clear();
        outgoingRequests.clear();
        denyAll.clear();
    }

    /**
     * Remove all player data from the manager.
     *
     * @param playerUUID The UUID of the player to remove.
     */
    public void removePlayerData(UUID playerUUID) {
        // Remove from a deny-all list
        denyAll.remove(playerUUID);

        // Clean up outgoing request
        UUID targetId = outgoingRequests.remove(playerUUID);
        if (targetId != null) {
            Set<UUID> targetIncoming = incomingRequests.get(targetId);
            if (targetIncoming != null) {
                targetIncoming.remove(playerUUID);
                if (targetIncoming.isEmpty()) {
                    incomingRequests.remove(targetId);
                }
            }
        }

        // Clean up incoming requests
        Set<UUID> requests = incomingRequests.remove(playerUUID);
        if (requests != null) {
            for (UUID requesterId : requests) {
                outgoingRequests.remove(requesterId);
            }
        }

        // Cancel and remove any pre-teleport task
        BukkitTask preTeleportTask = preTeleportTasks.remove(playerUUID);
        if (preTeleportTask != null) {
            preTeleportTask.cancel();
        }

        saveConfiguration();
    }

    /**
     * Remove all player data from the manager.
     *
     * @param player The player to remove.
     */
    public void removePlayerData(Player player) {
        removePlayerData(player.getUniqueId());
    }

}
