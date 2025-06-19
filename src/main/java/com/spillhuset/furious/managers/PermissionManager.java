package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Permission;
import com.spillhuset.furious.entities.PlayerRoles;
import com.spillhuset.furious.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages permissions, roles, and player-role associations.
 */
public class PermissionManager {
    private final Furious plugin;
    private final Map<UUID, Role> roles;
    private final Map<UUID, PlayerRoles> playerRoles;
    private final File rolesFile;
    private final File playerRolesFile;

    /**
     * Creates a new PermissionManager.
     *
     * @param plugin The plugin instance
     */
    public PermissionManager(Furious plugin) {
        this.plugin = plugin;
        this.roles = new HashMap<>();
        this.playerRoles = new HashMap<>();
        this.rolesFile = new File(plugin.getDataFolder(), "roles.yml");
        this.playerRolesFile = new File(plugin.getDataFolder(), "player_roles.yml");

        // Load roles and player roles
        loadRoles();
        loadPlayerRoles();

        // Create default roles if they don't exist
        createDefaultRoles();
    }

    /**
     * Creates default roles if they don't exist.
     */
    private void createDefaultRoles() {
        // Create default roles if they don't exist
        if (getRoleByName("default") == null) {
            Role defaultRole = new Role("default", "Default role for all players");
            defaultRole.addPermission("furious.teleport.request");
            defaultRole.addPermission("furious.teleport.accept");
            defaultRole.addPermission("furious.teleport.decline");
            defaultRole.addPermission("furious.teleport.list");
            defaultRole.addPermission("furious.teleport.abort");
            defaultRole.addPermission("furious.teleport.deny");
            defaultRole.addPermission("furious.guild.create");
            defaultRole.addPermission("furious.guild.invite");
            defaultRole.addPermission("furious.guild.join");
            defaultRole.addPermission("furious.guild.leave");
            defaultRole.addPermission("furious.guild.info");
            defaultRole.addPermission("furious.guild.list");
            defaultRole.addPermission("furious.guild.kick");
            defaultRole.addPermission("furious.guild.disband");
            defaultRole.addPermission("furious.guild.transfer");
            defaultRole.addPermission("furious.guild.description");
            defaultRole.addPermission("furious.guild.claim");
            defaultRole.addPermission("furious.guild.unclaim");
            defaultRole.addPermission("furious.guild.claims");
            defaultRole.addPermission("furious.guild.mobs");
            defaultRole.addPermission("furious.guild.homes");
            defaultRole.addPermission("furious.guild.homes.set");
            defaultRole.addPermission("furious.guild.homes.teleport");
            addRole(defaultRole);
        }

        if (getRoleByName("admin") == null) {
            Role adminRole = new Role("admin", "Administrator role with all permissions");
            adminRole.addPermission("furious.*");
            addRole(adminRole);
        }
    }

    /**
     * Loads roles from the roles.yml file.
     */
    private void loadRoles() {
        if (!rolesFile.exists()) {
            try {
                rolesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create roles.yml", e);
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(rolesFile);
        ConfigurationSection rolesSection = config.getConfigurationSection("roles");

        if (rolesSection == null) {
            return;
        }

        for (String roleIdStr : rolesSection.getKeys(false)) {
            try {
                UUID roleId = UUID.fromString(roleIdStr);
                ConfigurationSection roleSection = rolesSection.getConfigurationSection(roleIdStr);

                if (roleSection == null) {
                    continue;
                }

                String name = roleSection.getString("name");
                String description = roleSection.getString("description", "");

                Role role = new Role(name, description);

                // Use reflection to set the ID field
                try {
                    java.lang.reflect.Field idField = Role.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(role, roleId);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not set role ID", e);
                    continue;
                }

                List<String> permissionNodes = roleSection.getStringList("permissions");
                for (String permissionNode : permissionNodes) {
                    role.addPermission(permissionNode);
                }

                roles.put(roleId, role);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Invalid role ID: " + roleIdStr, e);
            }
        }
    }

    /**
     * Saves roles to the roles.yml file.
     */
    private void saveRoles() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection rolesSection = config.createSection("roles");

        for (Map.Entry<UUID, Role> entry : roles.entrySet()) {
            UUID roleId = entry.getKey();
            Role role = entry.getValue();

            ConfigurationSection roleSection = rolesSection.createSection(roleId.toString());
            roleSection.set("name", role.getName());
            roleSection.set("description", role.getDescription());

            List<String> permissionNodes = new ArrayList<>();
            for (Permission permission : role.getPermissions()) {
                permissionNodes.add(permission.toString());
            }

            roleSection.set("permissions", permissionNodes);
        }

        try {
            config.save(rolesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save roles.yml", e);
        }
    }

    /**
     * Loads player roles from the player_roles.yml file.
     */
    private void loadPlayerRoles() {
        if (!playerRolesFile.exists()) {
            try {
                playerRolesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create player_roles.yml", e);
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerRolesFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");

        if (playersSection == null) {
            return;
        }

        for (String playerIdStr : playersSection.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(playerIdStr);
                ConfigurationSection playerSection = playersSection.getConfigurationSection(playerIdStr);

                if (playerSection == null) {
                    continue;
                }

                PlayerRoles playerRolesObj = new PlayerRoles(playerId);

                List<String> roleIdStrs = playerSection.getStringList("roles");
                for (String roleIdStr : roleIdStrs) {
                    try {
                        UUID roleId = UUID.fromString(roleIdStr);
                        playerRolesObj.addRole(roleId);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().log(Level.WARNING, "Invalid role ID: " + roleIdStr, e);
                    }
                }

                List<String> permissionNodes = playerSection.getStringList("permissions");
                for (String permissionNode : permissionNodes) {
                    playerRolesObj.addDirectPermission(permissionNode);
                }

                playerRoles.put(playerId, playerRolesObj);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Invalid player ID: " + playerIdStr, e);
            }
        }
    }

    /**
     * Saves player roles to the player_roles.yml file.
     */
    private void savePlayerRoles() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection playersSection = config.createSection("players");

        for (Map.Entry<UUID, PlayerRoles> entry : playerRoles.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerRoles playerRolesObj = entry.getValue();

            ConfigurationSection playerSection = playersSection.createSection(playerId.toString());

            List<String> roleIdStrs = new ArrayList<>();
            for (UUID roleId : playerRolesObj.getRoleIds()) {
                roleIdStrs.add(roleId.toString());
            }

            playerSection.set("roles", roleIdStrs);

            List<String> permissionNodes = new ArrayList<>();
            for (Permission permission : playerRolesObj.getDirectPermissions()) {
                permissionNodes.add(permission.toString());
            }

            playerSection.set("permissions", permissionNodes);
        }

        try {
            config.save(playerRolesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player_roles.yml", e);
        }
    }

    /**
     * Shuts down the PermissionManager and saves all data.
     */
    public void shutdown() {
        saveRoles();
        savePlayerRoles();
    }

    /**
     * Gets a role by its ID.
     *
     * @param roleId The ID of the role
     * @return The role, or null if not found
     */
    public Role getRole(UUID roleId) {
        return roles.get(roleId);
    }

    /**
     * Gets a role by its name.
     *
     * @param name The name of the role
     * @return The role, or null if not found
     */
    public Role getRoleByName(String name) {
        for (Role role : roles.values()) {
            if (role.getName().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return null;
    }

    /**
     * Gets all roles.
     *
     * @return A collection of all roles
     */
    public Collection<Role> getRoles() {
        return new ArrayList<>(roles.values());
    }

    /**
     * Adds a role.
     *
     * @param role The role to add
     * @return True if the role was added, false if a role with the same ID already exists
     */
    public boolean addRole(Role role) {
        if (roles.containsKey(role.getId())) {
            return false;
        }
        roles.put(role.getId(), role);
        saveRoles();
        return true;
    }

    /**
     * Removes a role.
     *
     * @param roleId The ID of the role to remove
     * @return True if the role was removed, false if it was not found
     */
    public boolean removeRole(UUID roleId) {
        if (!roles.containsKey(roleId)) {
            return false;
        }

        // Remove the role from all players
        for (PlayerRoles playerRolesObj : playerRoles.values()) {
            playerRolesObj.removeRole(roleId);
        }

        roles.remove(roleId);
        saveRoles();
        savePlayerRoles();
        return true;
    }

    /**
     * Gets the roles of a player.
     *
     * @param playerId The ID of the player
     * @return The player's roles, or an empty set if the player has no roles
     */
    public Set<Role> getPlayerRoles(UUID playerId) {
        PlayerRoles playerRolesObj = playerRoles.get(playerId);
        if (playerRolesObj == null) {
            return new HashSet<>();
        }

        Set<Role> playerRoleSet = new HashSet<>();
        for (UUID roleId : playerRolesObj.getRoleIds()) {
            Role role = roles.get(roleId);
            if (role != null) {
                playerRoleSet.add(role);
            }
        }

        return playerRoleSet;
    }

    /**
     * Gets the direct permissions of a player.
     *
     * @param playerId The ID of the player
     * @return The player's direct permissions, or an empty set if the player has no direct permissions
     */
    public Set<Permission> getPlayerDirectPermissions(UUID playerId) {
        PlayerRoles playerRolesObj = playerRoles.get(playerId);
        if (playerRolesObj == null) {
            return new HashSet<>();
        }

        return playerRolesObj.getDirectPermissions();
    }

    /**
     * Adds a role to a player.
     *
     * @param playerId The ID of the player
     * @param roleId The ID of the role to add
     * @return True if the role was added, false if the player already had the role or the role doesn't exist
     */
    public boolean addRoleToPlayer(UUID playerId, UUID roleId) {
        if (!roles.containsKey(roleId)) {
            return false;
        }

        PlayerRoles playerRolesObj = playerRoles.get(playerId);
        if (playerRolesObj == null) {
            playerRolesObj = new PlayerRoles(playerId);
            playerRoles.put(playerId, playerRolesObj);
        }

        boolean result = playerRolesObj.addRole(roleId);
        if (result) {
            savePlayerRoles();
        }
        return result;
    }

    /**
     * Removes a role from a player.
     *
     * @param playerId The ID of the player
     * @param roleId The ID of the role to remove
     * @return True if the role was removed, false if the player didn't have the role
     */
    public boolean removeRoleFromPlayer(UUID playerId, UUID roleId) {
        PlayerRoles playerRolesObj = playerRoles.get(playerId);
        if (playerRolesObj == null) {
            return false;
        }

        boolean result = playerRolesObj.removeRole(roleId);
        if (result) {
            savePlayerRoles();
        }
        return result;
    }

    /**
     * Adds a direct permission to a player.
     *
     * @param playerId The ID of the player
     * @param permission The permission to add
     * @return True if the permission was added, false if the player already had the permission
     */
    public boolean addDirectPermissionToPlayer(UUID playerId, Permission permission) {
        PlayerRoles playerRolesObj = playerRoles.get(playerId);
        if (playerRolesObj == null) {
            playerRolesObj = new PlayerRoles(playerId);
            playerRoles.put(playerId, playerRolesObj);
        }

        boolean result = playerRolesObj.addDirectPermission(permission);
        if (result) {
            savePlayerRoles();
        }
        return result;
    }

    /**
     * Adds a direct permission to a player.
     *
     * @param playerId The ID of the player
     * @param permissionNode The permission node to add
     * @return True if the permission was added, false if the player already had the permission
     */
    public boolean addDirectPermissionToPlayer(UUID playerId, String permissionNode) {
        return addDirectPermissionToPlayer(playerId, new Permission(permissionNode));
    }

    /**
     * Removes a direct permission from a player.
     *
     * @param playerId The ID of the player
     * @param permission The permission to remove
     * @return True if the permission was removed, false if the player didn't have the permission
     */
    public boolean removeDirectPermissionFromPlayer(UUID playerId, Permission permission) {
        PlayerRoles playerRolesObj = playerRoles.get(playerId);
        if (playerRolesObj == null) {
            return false;
        }

        boolean result = playerRolesObj.removeDirectPermission(permission);
        if (result) {
            savePlayerRoles();
        }
        return result;
    }

    /**
     * Removes a direct permission from a player.
     *
     * @param playerId The ID of the player
     * @param permissionNode The permission node to remove
     * @return True if the permission was removed, false if the player didn't have the permission
     */
    public boolean removeDirectPermissionFromPlayer(UUID playerId, String permissionNode) {
        PlayerRoles playerRolesObj = playerRoles.get(playerId);
        if (playerRolesObj == null) {
            return false;
        }

        boolean result = playerRolesObj.removeDirectPermission(permissionNode);
        if (result) {
            savePlayerRoles();
        }
        return result;
    }

    /**
     * Checks if a player has a permission.
     * Takes into account direct permissions, role permissions, wildcards, and negated permissions.
     *
     * @param playerId The ID of the player
     * @param permissionNode The permission node to check
     * @return True if the player has the permission, false otherwise
     */
    public boolean hasPermission(UUID playerId, String permissionNode) {
        // Check if the player is an operator
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        if (offlinePlayer.isOp()) {
            return true;
        }

        PlayerRoles playerRolesObj = playerRoles.get(playerId);
        if (playerRolesObj == null) {
            // If the player has no roles, check if there's a default role
            Role defaultRole = getRoleByName("default");
            if (defaultRole != null) {
                return defaultRole.hasPermission(permissionNode);
            }
            return false;
        }

        // Check direct permissions first
        for (Permission permission : playerRolesObj.getDirectPermissions()) {
            if (permission.matches(permissionNode)) {
                return !permission.isNegated();
            }
        }

        // Check role permissions
        boolean hasPermission = false;

        for (UUID roleId : playerRolesObj.getRoleIds()) {
            Role role = roles.get(roleId);
            if (role == null) {
                continue;
            }

            // Check if the role has the permission
            if (role.hasPermission(permissionNode)) {
                hasPermission = true;
                break;
            }
        }

        // If the player has no roles or no roles with the permission, check the default role
        if (!hasPermission && playerRolesObj.getRoleIds().isEmpty()) {
            Role defaultRole = getRoleByName("default");
            if (defaultRole != null) {
                return defaultRole.hasPermission(permissionNode);
            }
        }

        return hasPermission;
    }

    /**
     * Checks if a player has a permission.
     * Takes into account direct permissions, role permissions, wildcards, and negated permissions.
     *
     * @param player The player
     * @param permissionNode The permission node to check
     * @return True if the player has the permission, false otherwise
     */
    public boolean hasPermission(Player player, String permissionNode) {
        return hasPermission(player.getUniqueId(), permissionNode);
    }
}