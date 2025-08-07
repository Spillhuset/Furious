# Player Guide: WARPS

This guide provides information on how to use the warps system as a player.

## Table of Contents
- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Permission Requirements](#permission-requirements)
- [Finding Warps](#finding-warps)
- [Using Warps](#using-warps)
- [Password-Protected Warps](#password-protected-warps)
- [Warp Costs](#warp-costs)
- [Using Warp Portals](#using-warp-portals)
- [Examples](#examples)
- [Tips and Best Practices](#tips-and-best-practices)

## Overview

The warps system allows you to teleport to predefined locations throughout the world. Warps are created by server administrators and provide a convenient way to travel between important locations. Some warps may require passwords or have costs associated with them.

## Basic Usage

- `/warp <name>` - Teleport to a warp
- `/warp <name> <password>` - Teleport to a password-protected warp
- `/warps` - List all available warps
- `/warps help` - Show help information

## Permission Requirements

To use the warps system, you need these permissions:

- `furious.warps.warp` - Allows using warps (default: true)
- `furious.warps.use` - Alternative permission for using warps (either permission works)
- `furious.warps.list` - Allows listing all warps (default: true)
- `furious.warps.unlink` - Allows removing warp portals (if you have permission to create them)

### Permission Inheritance
The server now uses a structured permission inheritance system:
- `furious.warps.*` grants all warps permissions
- `furious.warps.admin.*` grants all administrative warps permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you have `furious.warps.*`, you automatically have all basic warps permissions.

## Finding Warps

There are several ways to find warps in the game:

1. **Warp List**: Use `/warps` to see all available warps.
2. **Ask Administrators**: Server administrators can provide information about available warps.
3. **Explore**: Some warps may have physical markers or signs in the world.

## Using Warps

To use a warp:

1. Type `/warp <name>` to teleport to the warp location.
2. If the warp has a cost, you'll be informed and the amount will be deducted from your wallet if you have enough funds.
3. There may be a teleportation delay and effects (like nausea) during teleportation.
4. Operators and players with admin permissions can teleport instantly without delays or effects.

## Password-Protected Warps

Some warps may be password-protected for security reasons. To use these warps:

1. Type `/warp <name> <password>` with the correct password.
2. If the password is incorrect, you'll receive an error message.
3. Operators and players with the `furious.teleport.admin` permission can bypass password requirements.

## Warp Costs

Some warps may have costs associated with them:

1. When you use a warp with a cost, the amount will be deducted from your wallet.
2. If you don't have enough money, you won't be able to use the warp.
3. Operators and players with the `furious.teleport.admin` permission can bypass cost requirements.

## Using Warp Portals

Warp portals are physical gateways in the world:

1. Portals are identified by their gold block corners and special filling material.
2. Simply walk through a portal to be teleported to its destination.
3. Portals may have the same password and cost restrictions as command-based warps.

## Examples

### Basic Warp Usage

1. List all available warps:
   ```
   /warps
   ```

2. Teleport to a warp:
   ```
   /warp spawn
   ```

3. Teleport to a password-protected warp:
   ```
   /warp secretbase mypassword
   ```

## Tips and Best Practices

1. **Check Costs**: Some warps may have costs associated with them. Make sure you have enough money before attempting to use them. Costs are now permission-based, so different players may pay different amounts for the same warp.

2. **Password Security**: If you're using a password-protected warp, be careful about who might be watching when you type the password.

3. **Portal Awareness**: Be cautious when walking through unknown portals, as they might lead to dangerous locations. You can now remove warp portals with the appropriate permission.

4. **Teleport Cooldowns**: Cooldowns on warp teleportation are now permission-based. Higher permission levels may have shorter cooldowns or none at all. Wait for your cooldown to expire before attempting to warp again.

5. **Warp Naming**: Warp names are case-insensitive, so `/warp Spawn` and `/warp spawn` will take you to the same location.

6. **Movement During Teleportation**: Moving during the teleportation countdown will cancel the teleport. Stay still until the teleport completes.

7. **Combat Restrictions**: You may not be able to use warps while in combat to prevent abuse.

8. **World Restrictions**: Some worlds may have warps disabled for balance or gameplay reasons.

9. **Permission Awareness**: Your ability to use warps, see certain warps in the list, and interact with warp portals depends on your permissions. The new permission system provides more granular control over what you can do.

10. **Unified Teleportation System**: The warps system now integrates seamlessly with the teleport and homes systems, providing a consistent teleportation experience across all teleport types.

11. **Warp Visibility**: Some warps may be hidden from the warp list based on your permission level. Higher permission levels may see more available warps.

12. **Teleport Integration**: Warps now use the same teleportation handling as player teleports and homes, ensuring consistent behavior for cooldowns, costs, and effects.