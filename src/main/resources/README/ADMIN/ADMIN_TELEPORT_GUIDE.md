# Admin Guide: TELEPORT

This guide provides information on how to administer and configure the teleport system.

## Table of Contents
- [Overview](#overview)
- [Administrative Permissions](#administrative-permissions)
- [Admin Commands](#admin-commands)
- [Direct Teleportation](#direct-teleportation)
- [Coordinate Teleportation](#coordinate-teleportation)
- [World Spawn Commands](#world-spawn-commands)
- [World Configuration](#world-configuration)
- [Examples](#examples)
- [Best Practices](#best-practices)

## Overview

The teleport system allows players to request teleportation to other players. As an administrator, you have additional commands for direct teleportation and world configuration. This guide covers the administrative aspects of the teleport system.

## Administrative Permissions

To administer the teleport system, you need these permissions:

### Basic Administrative Permissions
- `furious.teleport.worldconfig` - Allows configuring world teleport settings (default: op)
- `furious.teleport.coords` - Allows using coordinate teleport commands (default: op)
- `furious.teleport.force` - Allows using force teleport commands (default: op)
- `furious.teleport.worlds` - Allows viewing world teleport settings (default: op)
- `furious.teleport.worldspawn` - Allows teleporting to world spawn (default: op)
- `furious.teleport.setworldspawn` - Allows setting world spawn location (default: op)

### Special Permissions
- `furious.teleport.admin` - Allows bypassing teleport queue, effects, costs, and cooldowns (default: op)

### Permission Inheritance
The server now uses a structured permission inheritance system:

- `furious.teleport.*` - Grants all teleport permissions
- `furious.teleport.admin.*` - Grants all administrative teleport permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you grant a player `furious.teleport.admin.*`, they automatically have all administrative teleport permissions like `furious.teleport.worldconfig`, `furious.teleport.coords`, etc.

### Teleport System Integration
The teleport system now integrates seamlessly with other teleportation systems:

- Unified teleport handling across homes, warps, and player teleports
- Consistent permission-based restrictions (cooldowns, costs, etc.) across all teleport types
- Configurable teleport behavior based on permission levels

## Admin Commands

These commands are available for server administrators:

### Direct Teleportation
- `/teleport <playerA> [playerB]` - Teleport yourself to playerA or teleport playerA to playerB
  - Example: `/teleport Steve` - Teleport yourself to Steve
  - Example: `/teleport Steve Alex` - Teleport Steve to Alex
  - Requirements:
    - You must have the `furious.teleport.force` permission

### Coordinate Teleportation
- `/teleport coords [player] <x> <y> <z> [world]` - Teleport to coordinates
  - Example: `/teleport coords 100 64 200` - Teleport yourself to the specified coordinates
  - Example: `/teleport coords Steve 100 64 200 world_nether` - Teleport Steve to the specified coordinates in the nether
  - Requirements:
    - You must have the `furious.teleport.coords` permission

### World Spawn Commands
- `/teleport worldspawn <player> [world]` - Teleport player to world's spawn location
  - Example: `/teleport worldspawn Steve` - Teleport Steve to the spawn of their current world
  - Example: `/teleport worldspawn Steve world_nether` - Teleport Steve to the nether's spawn point
  - Requirements:
    - You must have the `furious.teleport.force` permission

- `/teleport setworldspawn [world]` - Set the spawn location of a world
  - Example: `/teleport setworldspawn` - Set the spawn of your current world to your location
  - Example: `/teleport setworldspawn world_nether` - Set the spawn of the nether to your location
  - Requirements:
    - You must have the `furious.teleport.worldconfig` permission

## World Configuration

These commands allow administrators to configure which worlds allow teleportation.

### Managing World Settings
- `/teleport world enable <world>` - Enable teleportation to a world
  - Example: `/teleport world enable world_nether`
  - Allows players to teleport to the specified world
  - Requirements:
    - You must have the `furious.teleport.worldconfig` permission

- `/teleport world disable <world>` - Disable teleportation to a world
  - Example: `/teleport world disable world_the_end`
  - Prevents players from teleporting to the specified world
  - Requirements:
    - You must have the `furious.teleport.worldconfig` permission

- `/teleport worlds` - Show list of worlds and their teleportation status
  - Example: `/teleport worlds`
  - Displays which worlds allow teleportation and which don't
  - Requirements:
    - You must have the `furious.teleport.worldconfig` permission

## Examples

### Administrative Teleportation

1. Teleport yourself to a player:
   ```
   /teleport Steve
   ```

2. Teleport one player to another:
   ```
   /teleport Steve Alex
   ```

3. Teleport to specific coordinates:
   ```
   /teleport coords 100 64 200
   ```

4. Teleport a player to specific coordinates:
   ```
   /teleport coords Steve 100 64 200 world_nether
   ```

5. Teleport a player to a world's spawn point:
   ```
   /teleport worldspawn Steve world_nether
   ```

6. Set a world's spawn point:
   ```
   /teleport setworldspawn
   ```

### World Configuration

1. Enable teleportation to a world:
   ```
   /teleport world enable world_nether
   ```

2. Disable teleportation to a world:
   ```
   /teleport world disable world_the_end
   ```

3. View world teleportation settings:
   ```
   /teleport worlds
   ```

## Best Practices

1. **Permission Inheritance Setup**: Configure your permission system to take advantage of the new inheritance structure:
   - Set up permission groups that inherit from `furious.teleport.*` or `furious.teleport.admin.*`
   - Use the inheritance system to simplify permission management and ensure consistency
   - Remember that granting a wildcard permission like `furious.teleport.admin.*` grants all related permissions

2. **Teleport System Integration**: Configure the teleport system to work seamlessly with other teleportation systems:
   - Set consistent cooldowns and costs across teleport, homes, and warps systems
   - Configure permission-based restrictions that apply uniformly across all teleport types
   - Use the unified teleport handling for a consistent player experience

3. **Permission-Based Configuration**: Take advantage of permission-based teleport features:
   - Configure different cooldown times for different permission levels
   - Set up cost variations based on player permissions
   - Create permission-based exceptions to teleport restrictions

4. **World Restrictions**: Consider which worlds should allow teleportation. You might want to disable teleportation to challenge worlds or special event areas.
   - Document which worlds have teleportation enabled or disabled
   - Consider permission-based exceptions for staff or VIP players

5. **Safe Spawn Points**: When setting world spawn points, ensure they are in safe, accessible locations.
   - Test spawn points with different player states (flying, riding, etc.)
   - Consider the spawn experience for new players versus returning players

6. **Permission Management**: Carefully assign teleport permissions to staff based on their role and responsibilities.
   - Junior staff might receive only basic teleport permissions
   - Senior staff could get administrative teleport permissions
   - Only trusted administrators should receive bypass permissions like `furious.teleport.admin`

7. **Player Support**: Use administrative teleport commands to help players who are stuck or experiencing issues.
   - Choose the least invasive teleport method needed for each support task
   - Log all administrative teleport actions for accountability

8. **Coordinate Teleportation**: Use coordinate teleportation carefully to avoid placing players in unsafe or restricted areas.
   - Verify coordinates before teleporting players
   - Consider creating a database of safe teleport locations

9. **Documentation**: Maintain comprehensive documentation of teleport settings and permissions.
   - Document which staff roles have which teleport permissions
   - Keep records of world teleport settings and any changes

10. **Administrative Responsibility**: Use force teleport commands responsibly and respect player privacy.
    - Establish clear guidelines for when administrators should use force teleport
    - Consider notifying players when they are being teleported administratively

11. **World Spawn Management**: Set world spawns in safe, accessible locations for the best player experience.
    - Consider different spawn points for different player groups based on permissions
    - Test spawn points regularly to ensure they remain safe and accessible

12. **Regular Audits**: Periodically review teleport settings and permission assignments.
    - Check for permission inconsistencies across staff ranks
    - Verify that teleport restrictions are working as intended
    - Monitor teleport usage patterns to identify potential issues

13. **Player Communication**: Clearly communicate teleport rules and limitations to players.
    - Update server documentation to reflect the new permission system
    - Explain how permission levels affect teleport cooldowns, costs, and restrictions
    - Provide information about the integration between different teleport systems