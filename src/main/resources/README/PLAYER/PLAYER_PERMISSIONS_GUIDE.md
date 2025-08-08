# Player Guide: PERMISSIONS

This guide provides information about the permissions system from a player's perspective.

## Table of Contents
- [Overview](#overview)
- [How Permissions Affect Players](#how-permissions-affect-players)
- [Understanding Roles](#understanding-roles)
- [Permission Inheritance System](#permission-inheritance-system)
- [Permission Presets](#permission-presets)
- [Checking Your Permissions](#checking-your-permissions)
- [Common Permission Groups](#common-permission-groups)

## Overview

The permissions system in the Furious plugin is primarily an administrative tool that server administrators use to manage player access to various features. As a regular player, you won't directly use the `/permissions` command, but understanding how the system works can help you know what features you have access to and how your permission level affects your gameplay experience.

## How Permissions Affect Players

Permissions determine what commands and features you can use on the server. For example:
- Whether you can teleport to other players
- How many homes you can set
- Whether you can claim land for your guild
- What blocks you can protect with locks

Beyond basic access, permissions now also affect feature limitations and capabilities:
- **Cooldowns**: Higher permission levels may have shorter cooldowns or none at all
- **Costs**: Some features may cost less or be free for players with certain permissions
- **Limits**: The number of homes, guild claims, or other limited resources may be determined by your permission level
- **Special Features**: Some advanced features may only be available to players with specific permissions

Your permissions are typically assigned by server administrators based on your role or rank on the server.

## Permission Inheritance System

The server uses a structured permission inheritance system where higher-level permissions automatically grant related lower-level permissions. This means:

1. If you have a permission ending with `.*` (a wildcard permission), you automatically have all permissions that start with that prefix.
   - Example: If you have `furious.homes.*`, you have all home-related permissions.
   - Example: If you have `furious.*`, you have access to all features in the Furious plugin.

2. Wildcards can appear anywhere in the permission node, not just at the end:
   - Example: `furious.*.admin` would grant all admin permissions across different features.
   - Example: `furious.homes.*.others` would grant all permissions to manage other players' homes.

3. Your permission level may grant you additional capabilities within features:
   - Example: Players with `furious.teleport.admin` can bypass teleport cooldowns and costs.
   - Example: Players with `furious.homes.limit.10` can set up to 10 homes, while others might be limited to fewer.

The enhanced wildcard matching system now provides more flexibility in how permissions are assigned and inherited, allowing for more precise control over what features you can access.

## Permission Presets

The server now uses permission presets, which are predefined sets of permissions designed for specific player types or server roles. These presets help ensure consistent permission assignments across the server. Common presets include:

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

Server administrators can apply these presets to your account based on your role on the server. You may be assigned multiple presets if you have multiple roles (e.g., both a Builder and VIP).

## Understanding Roles

Most servers organize permissions using roles (sometimes called ranks or groups). A role is a collection of permissions that can be assigned to players. Common roles might include:

- **Default**: Basic permissions for all players
- **VIP**: Additional perks for donators or long-time players
- **Moderator**: Tools for helping manage the server
- **Admin**: Full access to server management

When you're assigned a role, you automatically receive all the permissions associated with that role.

## Checking Your Permissions

If you're curious about what permissions you have, you can:

1. Ask a server administrator to check your permissions using the new GUI-based permission management system:
   ```
   /permissions gui player YourUsername
   ```
   This will show your permissions in an easy-to-navigate interface.

2. Ask a server administrator to check your permissions using the command:
   ```
   /permissions player list permissions YourUsername
   ```

3. Ask a server administrator to check your roles using:
   ```
   /permissions player list roles YourUsername
   ```

4. Ask a server administrator which permission presets have been applied to your account:
   ```
   /permissions player list presets YourUsername
   ```

5. Try using a command - if you have permission, it will work; if not, you'll receive a message saying you don't have permission.

The new permission system includes caching for better performance, so changes to your permissions may take a moment to apply across all server systems.

## Common Permission Groups

Here are some common permission groups you might encounter in the Furious plugin, organized by category:

### Basic Feature Access
- `furious.teleport.request`: Allows requesting teleports to other players
- `furious.homes.set`: Allows setting homes
- `furious.guild.create`: Allows creating guilds
- `furious.wallet.pay`: Allows paying scraps to other players
- `furious.bank.deposit`: Allows depositing to bank

### Feature Limitations
- `furious.homes.limit.X`: Determines how many homes you can set (where X is a number like 1, 3, 5, 10)
- `furious.guild.claim.limit.X`: Determines how many chunks your guild can claim
- `furious.teleport.cooldown.X`: Determines your teleport cooldown time in seconds

### Permission Inheritance Examples
- `furious.teleport.*`: Grants all teleport permissions, including request, accept, decline, etc.
- `furious.homes.*`: Grants all homes permissions and typically unlimited homes
- `furious.guild.*`: Grants all guild permissions
- `furious.bank.*`: Grants all bank permissions
- `furious.wallet.*`: Grants all wallet permissions

### Special Capability Permissions
- `furious.teleport.bypass.cooldown`: Allows bypassing teleport cooldowns
- `furious.teleport.bypass.cost`: Allows teleporting without paying costs
- `furious.homes.set.others`: Allows setting homes for other players (typically staff only)
- `furious.bank.balance.others`: Allows checking other players' bank balances (typically staff only)

Remember that server administrators configure these permissions based on their server's specific needs, so the exact permissions you have may vary. Your permission level affects not just what commands you can use, but also cooldowns, costs, and limits for various features.