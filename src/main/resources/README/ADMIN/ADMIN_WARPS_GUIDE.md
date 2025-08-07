# Admin Guide: WARPS

This guide provides information on how to administer and configure the warps system.

## Table of Contents
- [Overview](#overview)
- [Administrative Permissions](#administrative-permissions)
- [Admin Commands](#admin-commands)
- [Warp Management](#warp-management)
- [Password Management](#password-management)
- [Cost Management](#cost-management)
- [Portal Management](#portal-management)
- [Warp Visibility](#warp-visibility)
- [Examples](#examples)
- [Best Practices](#best-practices)
- [Technical Implementation](#technical-implementation)

## Overview

The warps system allows players to teleport to predefined locations throughout the world. As an administrator, you can create and manage warps, set passwords and costs, create warp portals, and control warp visibility. The system includes features for both command-based teleportation and physical portal teleportation.

## Administrative Permissions

To administer the warps system, you need these permissions:

### Basic Warp Management Permissions
- `furious.warps.create` - Allows creating warps (default: op)
- `furious.warps.delete` - Allows deleting warps (default: op)
- `furious.warps.relocate` - Allows relocating warps (default: op)
- `furious.warps.rename` - Allows renaming warps (default: op)
- `furious.warps.cost` - Allows setting warp costs (default: op)
- `furious.warps.passwd` - Allows setting warp passwords (default: op)
- `furious.warps.link` - Allows creating warp portals (default: op)
- `furious.warps.unlink` - Allows removing warp portals (default: op)
- `furious.warps.visibility` - Allows toggling warp visibility (default: op)

### Granular Admin Permissions
The system now includes more granular permissions for operations on other players' warps:

- `furious.warps.create.others` - Allows creating warps for other players (default: op)
- `furious.warps.delete.others` - Allows deleting other players' warps (default: op)
- `furious.warps.relocate.others` - Allows relocating other players' warps (default: op)
- `furious.warps.rename.others` - Allows renaming other players' warps (default: op)
- `furious.warps.cost.others` - Allows setting costs on other players' warps (default: op)
- `furious.warps.passwd.others` - Allows setting passwords on other players' warps (default: op)

### Special Permissions
- `furious.teleport.admin` - Allows bypassing teleport queue, effects, costs, and password requirements (default: op)

### Permission Inheritance
The server now uses a structured permission inheritance system:

- `furious.warps.*` - Grants all warps permissions
- `furious.warps.admin.*` - Grants all administrative warps permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you grant a player `furious.warps.admin.*`, they automatically have all administrative warps permissions like `furious.warps.create.others`, `furious.warps.delete.others`, etc.

## Admin Commands

These commands are available for server administrators:

### Warp Management
- `/warps create <name> [cost] [password]` - Create a new warp
  - Example: `/warps create mining 10 secretpass`
  - Creates a new warp at your current location
  - Requirements:
    - You must have the `furious.warps.create` permission
    - You cannot be in a disabled world

- `/warps create <player> <n> [cost] [password]` - Create a warp for another player
  - Example: `/warps create Steve mining 10 secretpass`
  - Creates a new warp for the specified player at your current location
  - Requirements:
    - You must have the `furious.warps.create.others` permission
    - You cannot be in a disabled world

- `/warps delete <name>` - Delete a warp
  - Example: `/warps delete oldwarp`
  - Permanently removes the warp and any associated portals
  - Requirements:
    - You must have the `furious.warps.delete` permission

- `/warps relocate <name>` - Relocate a warp
  - Example: `/warps relocate spawn`
  - Moves the warp to your current location
  - Requirements:
    - You must have the `furious.warps.relocate` permission
    - You cannot be in a disabled world

- `/warps rename <old> <new>` - Rename a warp
  - Example: `/warps rename oldname newname`
  - Changes the name of an existing warp
  - Requirements:
    - You must have the `furious.warps.rename` permission

### Password Management
- `/warps passwd <name> [password]` - Set or remove a warp's password
  - Example: `/warps passwd vip secretpass`
  - Sets a password for the warp (or removes it if no password is provided)
  - Requirements:
    - You must have the `furious.warps.passwd` permission

### Cost Management
- `/warps cost <name> <amount>` - Set a warp's cost
  - Example: `/warps cost mining 25`
  - Sets the cost to use this warp
  - Requirements:
    - You must have the `furious.warps.cost` permission

### Portal Management
- `/warps link <name> [filling]` - Create a portal to a warp
  - Example: `/warps link spawn water`
  - Creates a portal between two gold blocks that teleports to the warp
  - Valid filling materials: air, water, nether_portal
  - Requirements:
    - You must have the `furious.warps.link` permission
    - You must be looking at a gold block

- `/warps unlink` - Remove a portal
  - Example: `/warps unlink`
  - Removes the portal you're looking at or standing in
  - Requirements:
    - You must have the `furious.warps.unlink` permission

### Warp Visibility
- `/warps visibility` - Toggle warp visibility
  - Example: `/warps visibility`
  - Toggles whether warp armor stands are visible to you
  - Requirements:
    - You must have the `furious.warps.visibility` permission

### Managing Other Players' Warps
These commands allow administrators to manage warps owned by other players:

- `/warps create <player> <n> [cost] [password]` - Create a warp for another player
  - Example: `/warps create Steve mining 10 secretpass`
  - Creates a new warp for the specified player at your current location
  - Requirements:
    - You must have the `furious.warps.create.others` permission
    - You cannot be in a disabled world

- `/warps delete <player> <n>` - Delete another player's warp
  - Example: `/warps delete Steve oldwarp`
  - Permanently removes the specified player's warp and any associated portals
  - Requirements:
    - You must have the `furious.warps.delete.others` permission

- `/warps relocate <player> <n>` - Relocate another player's warp
  - Example: `/warps relocate Steve spawn`
  - Moves the specified player's warp to your current location
  - Requirements:
    - You must have the `furious.warps.relocate.others` permission
    - You cannot be in a disabled world

- `/warps rename <player> <old> <new>` - Rename another player's warp
  - Example: `/warps rename Steve oldname newname`
  - Changes the name of the specified player's warp
  - Requirements:
    - You must have the `furious.warps.rename.others` permission

- `/warps passwd <player> <n> [password]` - Set or remove another player's warp password
  - Example: `/warps passwd Steve vip secretpass`
  - Sets a password for the specified player's warp (or removes it if no password is provided)
  - Requirements:
    - You must have the `furious.warps.passwd.others` permission

- `/warps cost <player> <n> <amount>` - Set another player's warp cost
  - Example: `/warps cost Steve mining 25`
  - Sets the cost to use the specified player's warp
  - Requirements:
    - You must have the `furious.warps.cost.others` permission

## Examples

### Setting Up a New Warp

1. Create a new warp:
   ```
   /warps create spawn
   ```

2. Create a warp with a cost:
   ```
   /warps create mining 25
   ```

3. Create a password-protected warp:
   ```
   /warps create vip secretpass
   ```

4. Create a warp with both cost and password:
   ```
   /warps create special 50 topsecret
   ```

### Managing Existing Warps

1. Relocate a warp:
   ```
   /warps relocate spawn
   ```

2. Change a warp's password:
   ```
   /warps passwd vip newpass
   ```

3. Remove a warp's password:
   ```
   /warps passwd vip
   ```

4. Change a warp's cost:
   ```
   /warps cost mining 50
   ```

5. Rename a warp:
   ```
   /warps rename oldname newname
   ```

### Creating and Managing Portals

1. Create a portal with air filling:
   ```
   /warps link spawn air
   ```
   (Look at a gold block when executing this command, then place another gold block to define the portal frame)

2. Create a portal with water filling:
   ```
   /warps link beach water
   ```

3. Create a portal with nether portal filling:
   ```
   /warps link nether nether_portal
   ```

4. Remove a portal:
   ```
   /warps unlink
   ```
   (Look at or stand in the portal you want to remove)

### Managing Warp Visibility

1. Toggle warp visibility:
   ```
   /warps visibility
   ```
   (This makes warp armor stands visible or invisible to you)

## Best Practices

1. **Granular Permission Management**: Take advantage of the new granular admin permissions to assign specific administrative capabilities to different staff roles:
   - Junior staff might receive only basic warp permissions for server maintenance
   - Moderators could get `furious.warps.create` and `furious.warps.relocate` for managing public warps
   - Senior staff might receive `.others` permissions for helping players with their warps
   - Only administrators should receive full access to all warp permissions

2. **Permission Inheritance Setup**: Configure your permission system to take advantage of the new inheritance structure:
   - Set up permission groups that inherit from `furious.warps.*` or `furious.warps.admin.*`
   - Use the inheritance system to simplify permission management and ensure consistency
   - Remember that granting a wildcard permission like `furious.warps.admin.*` grants all related permissions

3. **Strategic Warp Placement**: Place warps in strategic locations like spawn, resource areas, and community hubs.
   - Consider creating different types of warps for different purposes (public, VIP, staff)
   - Use permission-based access to control who can use which warps

4. **Naming Conventions**: Use clear, consistent naming for warps to make them easy to find and remember.
   - Consider prefixing warps with categories (e.g., "public_", "vip_", "staff_")
   - Document your naming conventions for staff reference

5. **Cost Balancing**: Set appropriate costs for warps based on their value and distance.
   - More valuable or distant warps should cost more
   - Consider permission-based cost variations for different player ranks

6. **Password Security**: Use strong passwords for sensitive warps and change them periodically.
   - Implement a password rotation schedule for high-security warps
   - Use the granular permissions to control who can set or change passwords

7. **Portal Design**: Design portals with clear visual indicators so players know where they lead.
   - Consider using signs or other indicators to show destination and cost
   - Use consistent designs for different types of portals

8. **World Restrictions**: Be aware that warps may be disabled in certain worlds for balance reasons.
   - Document which worlds have warp restrictions
   - Consider permission-based exceptions for staff or VIP players

9. **Documentation**: Keep track of all warps, their locations, costs, passwords, and permission requirements.
   - Maintain a central registry of all warps and their properties
   - Document which staff roles have which permissions for warps

10. **Regular Maintenance**: Periodically review and update warps to ensure they're still relevant and properly located.
    - Schedule regular audits of the warp system
    - Check for permission inconsistencies across staff ranks

11. **Visibility Control**: Use the visibility toggle to check warp locations without affecting regular players.
    - Train staff on how to use visibility controls for warp management
    - Document the visibility system for new staff members

12. **Portal Placement**: Place portals in logical locations that make sense for their destinations.
    - Consider traffic flow and server performance when placing portals
    - Use the new granular permissions to control who can create and manage portals

13. **Teleport System Integration**: The warps system now integrates seamlessly with other teleportation systems:
    - Configure consistent cooldowns and costs across warps, homes, and player teleports
    - Ensure permission-based restrictions are consistent across all teleport types
    - Use the unified teleport handling for a consistent player experience

## Technical Implementation

The warps system uses invisible armor stands to mark warp locations. These armor stands have the following properties:

- They are invisible to regular players
- They have custom names set to "Warp: [warp_name]"
- They are only visible to administrators who have enabled warp visibility with the `furious.warps.visibility` permission
- They have the WAYPOINT_TRANSMIT_RANGE attribute set to 500.0
- They are small-sized, invulnerable, and have no gravity

For more detailed technical information, refer to the WAYPOINT_IMPLEMENTATION.md document.