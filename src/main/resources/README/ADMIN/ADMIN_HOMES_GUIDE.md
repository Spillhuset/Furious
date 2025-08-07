# Admin Guide: HOMES

This guide provides information on how to administer and configure the homes system.

## Table of Contents
- [Overview](#overview)
- [Administrative Permissions](#administrative-permissions)
- [Admin Commands](#admin-commands)
- [World Settings](#world-settings)
- [Managing Player Homes](#managing-player-homes)
- [Examples](#examples)
- [Best Practices](#best-practices)

## Overview

The homes system allows players to set personal teleport locations called "homes." As an administrator, you can manage world settings for homes and help players by managing their homes if needed.

## Administrative Permissions

To administer the homes system, you need these permissions:

### Granular Admin Permissions
The general `furious.homes.admin` permission has been replaced with more granular permissions:

- `furious.homes.set.others` - Allows setting homes for other players (default: op)
- `furious.homes.delete.others` - Allows deleting other players' homes (default: op)
- `furious.homes.move.others` - Allows moving other players' homes (default: op)
- `furious.homes.rename.others` - Allows renaming other players' homes (default: op)
- `furious.homes.tp.others` - Allows teleporting to other players' homes (default: op)
- `furious.homes.list.others` - Allows listing other players' homes (default: op)

### World Management Permissions
- `furious.homes.world` - Allows managing world settings for homes (default: op)

### Permission Inheritance
The server now uses a structured permission inheritance system:

- `furious.homes.*` - Grants all homes permissions and unlimited homes
- `furious.homes.*.others` - Grants all administrative homes permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you grant a player `furious.homes.*.others`, they automatically have all administrative homes permissions like `furious.homes.set.others`, `furious.homes.delete.others`, etc.

### Legacy Support
For backward compatibility, the `furious.homes.admin` permission still works and grants all the granular admin permissions listed above.

## Admin Commands

These commands are available for server administrators:

### World Settings
- `/homes world list` - Lists all worlds and their homes settings
  - Example: `/homes world list`
  - Shows which worlds have homes enabled or disabled
  - Requirements:
    - You must have the `furious.homes.world` permission

- `/homes world enable <world>` - Enables homes in a specific world
  - Example: `/homes world enable world_nether`
  - Allows players to create and use homes in the specified world
  - Requirements:
    - You must have the `furious.homes.world` permission

- `/homes world disable <world>` - Disables homes in a specific world
  - Example: `/homes world disable world_the_end`
  - Prevents players from creating new homes in the specified world
  - Existing homes may still function depending on configuration
  - Requirements:
    - You must have the `furious.homes.world` permission

### Managing Player Homes
- `/homes set <player> <n>` - Set a home for another player
  - Example: `/homes set Steve base`
  - Sets a home for the specified player at your current location
  - Requirements:
    - You must have the `furious.homes.set.others` permission
    - Legacy: The `furious.homes.admin` permission also works

- `/homes move <player> <n>` - Move a home for another player
  - Example: `/homes move Steve base`
  - Moves the specified player's home to your current location
  - Requirements:
    - You must have the `furious.homes.move.others` permission
    - Legacy: The `furious.homes.admin` permission also works

- `/homes rename <player> <oldname> <newname>` - Rename a home for another player
  - Example: `/homes rename Steve base main_base`
  - Renames the specified player's home
  - Requirements:
    - You must have the `furious.homes.rename.others` permission
    - Legacy: The `furious.homes.admin` permission also works

- `/homes delete <player> <n>` - Delete a home for another player
  - Example: `/homes delete Steve base`
  - Deletes the specified player's home
  - Requirements:
    - You must have the `furious.homes.delete.others` permission
    - Legacy: The `furious.homes.admin` permission also works

- `/homes list <player>` - List another player's homes
  - Example: `/homes list Steve`
  - Shows all homes for the specified player
  - Requirements:
    - You must have the `furious.homes.list.others` permission
    - Legacy: The `furious.homes.admin` permission also works

- `/homes tp <player> <n>` - Teleport to another player's home
  - Example: `/homes tp Steve base`
  - Teleports you to the specified player's home
  - Requirements:
    - You must have the `furious.homes.tp.others` permission
    - Legacy: The `furious.homes.admin` permission also works

## Examples

### Managing World Settings

1. List all worlds and their homes settings:
   ```
   /homes world list
   ```
   ```
   [Server] Homes settings:
   [Server] world: ENABLED
   [Server] world_nether: ENABLED
   [Server] world_the_end: DISABLED
   ```

2. Disable homes in the nether world:
   ```
   /homes world disable world_nether
   ```
   ```
   [Server] Homes disabled in world_nether.
   ```

3. Enable homes in the end world:
   ```
   /homes world enable world_the_end
   ```
   ```
   [Server] Homes enabled in world_the_end.
   ```

### Managing Player Homes

1. Set a home for a player:
   ```
   /homes set Steve spawn
   ```
   This creates a home named "spawn" for Steve at your current location.

2. Move a player's home to a new location:
   ```
   /homes move Steve base
   ```
   This moves Steve's "base" home to your current location.

3. Rename a player's home:
   ```
   /homes rename Steve old_base new_base
   ```
   This renames Steve's home from "old_base" to "new_base".

4. Delete a player's home:
   ```
   /homes delete Steve unwanted_home
   ```
   This permanently removes Steve's "unwanted_home" home.

## Best Practices

1. **Granular Permission Management**: Take advantage of the new granular admin permissions to assign specific administrative capabilities to different staff roles:
   - Junior staff might receive only `furious.homes.list.others` for monitoring
   - Moderators could get `furious.homes.tp.others` to check problematic homes
   - Senior staff might receive `furious.homes.move.others` and `furious.homes.rename.others`
   - Only administrators should receive `furious.homes.delete.others` to prevent accidental data loss

2. **Permission Inheritance Setup**: Configure your permission system to take advantage of the new inheritance structure:
   - Set up permission groups that inherit from `furious.homes.*` or `furious.homes.*.others`
   - Use the inheritance system to simplify permission management and ensure consistency
   - Remember that granting a wildcard permission like `furious.homes.*.others` grants all related permissions

3. **Home Limit Configuration**: Use permission-based home limits to create a tiered system:
   - Configure different home limits for different player ranks using the `furious.homes.limit.*` permissions
   - Consider creating custom home limits for special player groups using `furious.homes.limit.custom`
   - Balance home limits with your server's gameplay style and economy

4. **World Restrictions**: Consider which worlds should allow homes. PvP or challenge worlds might be better without homes.
   - Use the world management commands to enforce these restrictions
   - Document your world settings for future reference

5. **Player Support**: Use the granular administrative home commands to help players who are having issues with their homes.
   - Choose the least invasive permission needed for each support task
   - Log all administrative actions on player homes

6. **Documentation**: Keep a record of world settings and permission configurations for homes.
   - Document which staff roles have which granular permissions
   - Maintain a changelog of permission changes

7. **Player Communication**: Clearly communicate to players which worlds allow homes, home limits based on permissions, and how to request assistance.
   - Update server documentation to reflect the new permission system
   - Explain how permission levels affect home limits

8. **Regular Audits**: Periodically review world settings and permission assignments to ensure they align with server policies.
   - Check for permission inconsistencies across staff ranks
   - Verify that home limits are appropriate for each player group

9. **Economy Integration**: If using the home buying feature, ensure the costs are balanced with your server's economy.
   - Configure costs based on permission levels if appropriate
   - Consider how home purchases interact with other economic systems

10. **Teleport System Integration**: The homes system now integrates seamlessly with other teleportation systems:
    - Configure consistent cooldowns and costs across homes, warps, and player teleports
    - Ensure permission-based restrictions are consistent across all teleport types
    - Use the unified teleport handling for a consistent player experience

11. **Conflict Resolution**: Establish procedures for handling disputes over homes between players.
    - Create clear guidelines for when administrators should intervene
    - Document which admin permissions should be used in different scenarios

12. **Plugin Compatibility**: Ensure the homes system works well with other teleportation and permission plugins.
    - Test interactions between homes permissions and other permission-based plugins
    - Resolve any conflicts between different permission systems