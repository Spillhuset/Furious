# Admin Guide: PERMISSIONS

This guide provides information on how to administer and configure the permissions system.

## Table of Contents
- [Overview](#overview)
- [Permission Requirements](#permission-requirements)
- [Permission Inheritance System](#permission-inheritance-system)
- [Enhanced Wildcard Matching](#enhanced-wildcard-matching)
- [Permission Caching](#permission-caching)
- [GUI-Based Permission Management](#gui-based-permission-management)
- [Permission Presets](#permission-presets)
- [Command Structure](#command-structure)
- [Roles Management](#roles-management)
- [Player Management](#player-management)
- [Examples](#examples)
- [Best Practices](#best-practices)

## Overview

The `/permissions` command allows server administrators to manage roles and player permissions. It provides a flexible system for creating roles, assigning permissions to roles, and managing player-specific permissions. The system now features a structured permission inheritance model that makes permission management more intuitive and efficient.

## Permission Requirements

To use the `/permissions` command and its subcommands, you need the following permission:

- `furious.permission.*` - Gives access to all permission management commands (default: op)

## Permission Inheritance System

The plugin now features a structured permission inheritance system that makes permission management more efficient and intuitive. This system allows higher-level permissions to automatically grant related lower-level permissions.

### How Permission Inheritance Works

Permission inheritance follows these principles:

1. **Wildcard Permissions**: Permissions ending with `.*` grant all child permissions
   - Example: `furious.guild.*` grants all guild-related permissions
   - Example: `furious.bank.admin.*` grants all administrative bank permissions

2. **Hierarchical Structure**: Permissions are organized in a hierarchical structure
   - Top level: Feature category (e.g., `furious.guild`, `furious.bank`)
   - Second level: Operation type (e.g., `furious.guild.admin`, `furious.bank.add`)
   - Third level: Target specification (e.g., `furious.bank.add.others`)

3. **Automatic Inheritance**: Higher-level permissions automatically grant lower-level permissions
   - If a player has `furious.homes.*.others`, they automatically have all permissions like `furious.homes.set.others`, `furious.homes.delete.others`, etc.
   - If a player has `furious.teleport.admin.*`, they automatically have permissions like `furious.teleport.worldconfig`, `furious.teleport.coords`, etc.

4. **Granular Control**: The system allows for precise control over permissions
   - You can grant specific permissions without granting entire categories
   - You can create custom permission sets for different staff roles

## Enhanced Wildcard Matching

The permission system now features enhanced wildcard matching capabilities that provide more flexibility and power in permission management.

### How Enhanced Wildcard Matching Works

1. **Multi-Level Inheritance**: The enhanced system supports multi-level inheritance for wildcard permissions
   - A permission like `furious.*` now grants all permissions that start with "furious."
   - This works for any level of the permission hierarchy

2. **Middle Wildcards**: Wildcards can now appear anywhere in the permission node, not just at the end
   - Example: `furious.*.admin` matches permissions like `furious.bank.admin` and `furious.guild.admin`
   - Example: `furious.homes.*.others` matches all permissions for managing other players' homes

3. **Pattern Matching**: The system uses sophisticated pattern matching for permissions
   - Permissions are converted to regex patterns for flexible matching
   - The system handles complex permission structures efficiently

### Examples of Enhanced Wildcard Matching

1. **Feature-Wide Administrative Access**:
   - `furious.*.admin` grants admin permissions across all features
   - This matches `furious.bank.admin`, `furious.guild.admin`, `furious.homes.admin`, etc.

2. **Cross-Feature Operations**:
   - `furious.*.*.others` grants all permissions for operations on other players
   - This matches `furious.homes.set.others`, `furious.bank.balance.others`, etc.

3. **Specific Feature Wildcards**:
   - `furious.homes.*` grants all home-related permissions
   - `furious.bank.*` grants all bank-related permissions

## Permission Caching

To improve performance, especially on servers with many players and permissions, the system now includes a permission caching mechanism.

### How Permission Caching Works

1. **Result Caching**: The system caches the results of permission checks
   - When a permission is checked, the result is stored in memory
   - Subsequent checks for the same permission use the cached result
   - This significantly reduces CPU usage for permission checks

2. **Cache Invalidation**: The cache is automatically invalidated when permissions change
   - When permissions are added or removed, the cache is cleared
   - This ensures that permission changes take effect immediately

3. **Configurable Caching**: The caching system can be configured based on server needs
   - Cache size limits prevent memory issues
   - Cache expiration can be set to balance performance and freshness

### Benefits of Permission Caching

1. **Improved Performance**: Faster permission checks, especially for frequently checked permissions
2. **Reduced Server Load**: Less CPU usage for permission operations
3. **Better Scalability**: More efficient handling of large permission sets and many players

### Managing the Permission Cache

- The cache is automatically managed by the system
- To manually clear the cache (rarely needed):
  ```
  /permissions cache clear
  ```

## GUI-Based Permission Management

A new graphical user interface (GUI) has been added to make permission management more intuitive and user-friendly.

### Accessing the Permission GUI

To open the permission management GUI:
```
/permissions gui
```

Requirements:
- You must have the `furious.permission.*` permission

### GUI Features

The permission management GUI includes several sections:

1. **Browse Permissions**: Browse and edit all permissions in the system
   - View permissions by category
   - Toggle permissions on/off with a click
   - Search for specific permissions

2. **Search Permissions**: Search for specific permissions
   - Find permissions by name
   - Filter by category or feature

3. **Permission Presets**: Apply predefined permission sets
   - View available presets
   - Apply presets to players or roles

4. **Player Permissions**: Manage permissions for specific players
   - View a player's current permissions
   - Add or remove permissions
   - Assign roles to players

5. **Role Editor**: Create and manage permission roles
   - Create new roles
   - Edit role permissions
   - Assign roles to players

6. **Inheritance Visualization**: Visualize permission inheritance relationships
   - See how permissions are inherited
   - Understand the permission hierarchy

### Using the Permission GUI

1. **Browsing Permissions**:
   - Click on "Browse Permissions" in the main menu
   - Navigate through pages of permissions
   - Click on a permission to toggle it

2. **Applying Presets**:
   - Click on "Permission Presets" in the main menu
   - Select a preset to view its permissions
   - Click "Apply Preset" and enter a player or role name

3. **Managing Player Permissions**:
   - Click on "Player Permissions" in the main menu
   - Enter a player name to view their permissions
   - Add or remove permissions as needed

## Permission Presets

The system now includes predefined permission presets that can be applied to players or roles, making permission setup faster and more consistent.

### Available Permission Presets

1. **Admin Preset**: Full access to all server features and administrative tools
   - Includes permissions like `furious.*` and `furious.admin.*`
   - Typically assigned to server administrators only

2. **Moderator Preset**: Tools for helping manage the server and players
   - Includes permissions to view other players' information and manage basic server functions
   - Examples: `furious.teleport.admin`, `furious.homes.*.others`, `furious.guild.admin.*`

3. **Builder Preset**: Special permissions for world building and construction
   - Includes permissions for teleportation and world management
   - Examples: `furious.teleport.coords`, `furious.warps.create`

4. **VIP Preset**: Enhanced gameplay features for donators or special players
   - Includes bypass permissions for cooldowns and costs
   - Examples: `furious.teleport.bypass.cooldown`, `furious.homes.set.extra.5`

5. **Economy Preset**: Focused on economic activities and transactions
   - Includes permissions for bank and wallet management
   - Examples: `furious.bank.*`, `furious.wallet.*`

6. **Survival Preset**: Basic gameplay permissions for regular survival players
   - Includes essential teleportation, homes, and guild features
   - Examples: `furious.teleport.*`, `furious.homes.*`, `furious.guild.*`

### Using Permission Presets

#### Via GUI

1. Open the permission GUI:
   ```
   /permissions gui
   ```

2. Click on "Permission Presets"

3. Select a preset and click "Apply Preset"

4. Enter the player or role name to apply the preset

#### Via Commands

Apply a preset to a player:
```
/permissions preset apply <preset_name> player <player_name>
```

Apply a preset to a role:
```
/permissions preset apply <preset_name> role <role_name>
```

List available presets:
```
/permissions preset list
```

View preset details:
```
/permissions preset info <preset_name>
```

### Examples of Permission Inheritance

1. **Guild System Example**:
   - `furious.guild.*` grants all guild permissions
   - `furious.guild.admin.*` grants all administrative guild permissions
   - `furious.guild.officer.*` grants all officer-level permissions
   - If a player has `furious.guild.admin.transfer`, they can transfer guild ownership administratively

2. **Homes System Example**:
   - `furious.homes.*` grants all homes permissions and unlimited homes
   - `furious.homes.*.others` grants all administrative homes permissions
   - If a player has `furious.homes.set.others`, they can set homes for other players

3. **Bank System Example**:
   - `furious.bank.*` grants all bank permissions
   - `furious.bank.admin.*` grants all administrative bank permissions
   - If a player has `furious.bank.add.others`, they can add to other players' account balances

## Command Structure

The `/permissions` command has three main subcommands:

1. `roles` (alias: `r`) - Manage roles
2. `player` (alias: `p`) - Manage player permissions
3. `help` (alias: `h`) - Show help information

### Basic Usage

- `/permissions` - Shows help information
- `/permissions help` - Shows detailed help information
- `/permissions roles` - Shows roles management commands
- `/permissions player` - Shows player management commands

## Roles Management

The roles subcommand allows you to create, delete, and manage roles.

### Creating and Managing Roles

- `/permissions roles create <role_name> [description]` - Create a new role
  - Example: `/permissions roles create moderator "Moderator role with basic moderation permissions"`
  - Requirements:
    - You must have the `furious.permission.*` permission

- `/permissions roles delete <role_name>` - Delete a role
  - Example: `/permissions roles delete moderator`
  - Requirements:
    - You must have the `furious.permission.*` permission

- `/permissions roles list` - List all roles
  - Example: `/permissions roles list`
  - Requirements:
    - You must have the `furious.permission.*` permission

- `/permissions roles info <role_name>` - View detailed information about a role
  - Example: `/permissions roles info moderator`
  - Requirements:
    - You must have the `furious.permission.*` permission

- `/permissions roles set description <role_name> <description>` - Set or update a role's description
  - Example: `/permissions roles set description moderator "Staff role with moderation abilities"`
  - Requirements:
    - You must have the `furious.permission.*` permission

### Managing Role Permissions

- `/permissions roles add permission <role_name> <permission>` - Add a permission to a role
  - Example: `/permissions roles add permission moderator furious.teleport.force`
  - Example with wildcard: `/permissions roles add permission admin furious.teleport.*`
  - Requirements:
    - You must have the `furious.permission.*` permission

- `/permissions roles remove permission <role_name> <permission>` - Remove a permission from a role
  - Example: `/permissions roles remove permission moderator furious.teleport.force`
  - Example with wildcard: `/permissions roles remove permission admin furious.teleport.*`
  - Requirements:
    - You must have the `furious.permission.*` permission

### Using Permission Inheritance with Roles

The permission inheritance system is particularly powerful when combined with roles. Here are some strategies for effective role-based permission management:

1. **Tiered Role Structure**: Create roles with increasing levels of permissions
   - `helper` role: Basic moderation tools
   - `moderator` role: More advanced moderation capabilities
   - `admin` role: Full administrative access

2. **Efficient Permission Assignment**: Use wildcard permissions to grant entire categories
   - For admin role: `/permissions roles add permission admin furious.*`
   - For moderator role: `/permissions roles add permission moderator furious.teleport.*`
   - For helper role: `/permissions roles add permission helper furious.teleport.force`

3. **Granular Control**: Use specific permissions for fine-tuned access
   - Grant specific administrative capabilities:
     - `/permissions roles add permission moderator furious.homes.set.others`
     - `/permissions roles add permission moderator furious.bank.balance.others`
   - Restrict sensitive operations:
     - `/permissions roles remove permission moderator furious.bank.set.others`

### Assigning Roles to Players

- `/permissions roles add player <role_name> <player_name>` - Assign a role to a player
  - Example: `/permissions roles add player moderator Steve`
  - Requirements:
    - You must have the `furious.permission.*` permission

- `/permissions roles remove player <role_name> <player_name>` - Remove a role from a player
  - Example: `/permissions roles remove player moderator Steve`
  - Requirements:
    - You must have the `furious.permission.*` permission

## Player Management

The player subcommand allows you to manage player-specific permissions and view player roles.

### Managing Player Permissions

- `/permissions player add permission <player_name> <permission>` - Add a permission directly to a player
  - Example: `/permissions player add permission Steve furious.teleport.force`
  - Requirements:
    - You must have the `furious.permission.*` permission

- `/permissions player remove permission <player_name> <permission>` - Remove a permission from a player
  - Example: `/permissions player remove permission Steve furious.teleport.force`
  - Requirements:
    - You must have the `furious.permission.*` permission

- `/permissions player list permissions <player_name>` - List all permissions assigned to a player
  - Example: `/permissions player list permissions Steve`
  - Requirements:
    - You must have the `furious.permission.*` permission

### Viewing Player Roles

- `/permissions player list roles <player_name>` - List all roles assigned to a player
  - Example: `/permissions player list roles Steve`
  - Requirements:
    - You must have the `furious.permission.*` permission

## Examples

### Creating a Staff Role Hierarchy

1. Create the roles:
   ```
   /permissions roles create admin "Administrator with full access"
   /permissions roles create moderator "Moderator with moderation abilities"
   /permissions roles create helper "Helper with basic moderation tools"
   ```

2. Add permissions to roles:
   ```
   /permissions roles add permission admin furious.*
   /permissions roles add permission moderator furious.teleport.*
   /permissions roles add permission moderator furious.invsee
   /permissions roles add permission helper furious.teleport.force
   ```

3. Assign roles to players:
   ```
   /permissions roles add player admin Alex
   /permissions roles add player moderator Steve
   /permissions roles add player helper Sophia
   ```

### Managing Individual Player Permissions

1. Add a specific permission to a player:
   ```
   /permissions player add permission John furious.teleport.force
   ```

2. Check a player's permissions:
   ```
   /permissions player list permissions John
   ```

3. Remove a specific permission from a player:
   ```
   /permissions player remove permission John furious.teleport.force
   ```

### Using Command Aliases

The `/permissions` command supports aliases for faster typing:

- `/permissions r` instead of `/permissions roles`
- `/permissions p` instead of `/permissions player`
- `/permissions h` instead of `/permissions help`

Examples:
```
/permissions r list
/permissions p list permissions Steve
/permissions r add permission moderator furious.teleport.force
```

## Best Practices

### General Permission Management

1. **Role-Based Permissions**: It's generally better to assign permissions to roles and then assign roles to players, rather than assigning permissions directly to players.

2. **Role Hierarchy**: Create a clear hierarchy of roles with increasing levels of permissions.

3. **Descriptive Role Names**: Use clear, descriptive names for roles to make management easier.

4. **Regular Audits**: Periodically review roles and permissions to ensure they're still appropriate.

5. **Documentation**: Maintain documentation of your server's role structure and permission assignments.

6. **Minimal Direct Permissions**: Avoid assigning too many permissions directly to players, as this can make permission management more complex.

7. **Consistent Naming Convention**: Use a consistent naming convention for roles to make the hierarchy clear (e.g., "admin", "mod", "helper").

8. **Test New Roles**: After creating a new role or modifying permissions, test the role to ensure it has the intended access level.

9. **Backup Before Changes**: Consider backing up your permissions configuration before making significant changes.

### Leveraging the Permission Inheritance System

10. **Use Wildcard Permissions Strategically**: Wildcard permissions are powerful but should be used carefully:
    - Use `furious.*` only for server owners or highest-level administrators
    - Use feature-specific wildcards (e.g., `furious.teleport.*`) for moderators or specialized staff
    - Use sub-feature wildcards (e.g., `furious.bank.admin.*`) for staff with specific responsibilities

11. **Granular Permission Assignment**: Take advantage of the granular permission structure:
    - Assign specific `.others` permissions (e.g., `furious.homes.set.others`) to staff who need to help players
    - Use operation-specific permissions (e.g., `furious.bank.add` vs `furious.bank.set`) to limit capabilities
    - Consider permission-based feature limitations (cooldowns, costs, limits) when assigning permissions

12. **Permission Inheritance Planning**: Design your permission structure with inheritance in mind:
    - Map out permission hierarchies for each feature
    - Identify which permissions should be granted to which staff roles
    - Create a permission assignment matrix documenting which roles get which permissions

13. **Avoid Permission Conflicts**: Be aware of potential conflicts when using inheritance:
    - Granting a wildcard permission may override specific restrictions
    - Some plugins may handle permission negation differently
    - Test permission combinations to ensure they work as expected

14. **Feature Integration Awareness**: Understand how permissions interact across integrated features:
    - Economic permissions (wallet, bank, shops) work together in a unified structure
    - Teleportation permissions (teleport, homes, warps) share common restrictions and capabilities
    - Guild permissions interact with territory and protection systems

15. **Permission-Based Customization**: Use permissions to create customized experiences:
    - Set different home limits based on player rank using `furious.homes.limit.*` permissions
    - Configure teleport cooldowns and costs based on permission levels
    - Create VIP features using permission-based access controls