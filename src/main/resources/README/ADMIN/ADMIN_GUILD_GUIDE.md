# Admin Guide: GUILD

This guide provides information on how to administer and configure the guild system.

## Table of Contents
- [Overview](#overview)
- [Administrative Permissions](#administrative-permissions)
- [Admin Commands](#admin-commands)
- [Advanced Territory Management](#advanced-territory-management)
- [Guild System Configuration](#guild-system-configuration)
- [Examples](#examples)
- [Best Practices](#best-practices)

## Overview

The guild system allows players to create and join player-run communities called guilds. As an administrator, you can configure guild settings, manage permissions, and handle advanced territory management for unmanned guilds.

## Administrative Permissions

To administer the guild system, you need these permissions:

- `furious.guild.claim.unmanned` - Allows claiming chunks for unmanned guilds (default: op)
- `furious.guild.unclaim.unmanned` - Allows unclaiming chunks from unmanned guilds (default: op)

### Granular Admin Permissions
The general `furious.guild.admin` permission has been replaced with more granular permissions:

- `furious.guild.admin.transfer` - Allows administrative guild ownership transfers (default: op)
- `furious.guild.admin.unclaim` - Allows administrative unclaiming of guild land (default: op)
- `furious.guild.admin.homes` - Allows administrative management of guild homes (default: op)
- `furious.guild.admin.disband` - Allows administrative disbanding of guilds (default: op)
- `furious.guild.admin.info` - Allows viewing detailed administrative information about guilds (default: op)

### Permission Inheritance
The server now uses a structured permission inheritance system:

- `furious.guild.*` - Grants all guild permissions
- `furious.guild.admin.*` - Grants all administrative guild permissions
- `furious.guild.officer.*` - Grants all officer-level permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you grant a player `furious.guild.admin.*`, they automatically have all administrative guild permissions like `furious.guild.admin.transfer`, `furious.guild.admin.unclaim`, etc.

### Player Permission Management

These are the permissions you can assign to players:

#### Basic Guild Permissions
- `furious.guild.create` - Allows creating guilds (default: true)
- `furious.guild.info` - Allows viewing guild information (default: true)
- `furious.guild.list` - Allows listing all guilds (default: true)

#### Member Management Permissions
- `furious.guild.invite` - Allows inviting players to guilds (default: true)
- `furious.guild.join` - Allows joining guilds (default: true)
- `furious.guild.leave` - Allows leaving guilds (default: true)
- `furious.guild.kick` - Allows kicking players from guilds (default: true)
- `furious.guild.transfer` - Allows transferring guild ownership (default: true)

#### Territory Management Permissions
- `furious.guild.claim` - Allows claiming chunks for a guild (default: true)
- `furious.guild.unclaim` - Allows unclaiming chunks from a guild (default: true)
- `furious.guild.claims` - Allows viewing claimed chunks of a guild (default: true)
- `furious.guild.mobs` - Allows controlling mob spawning in guild claimed chunks (default: true)

#### Guild Homes Permissions
- `furious.guild.homes` - Allows managing guild homes (default: true)
- `furious.guild.homes.set` - Allows setting guild homes (default: true)
- `furious.guild.homes.teleport` - Allows teleporting to guild homes (default: true)

### Role-Permission Integration

The guild system now features enhanced integration between guild roles and permissions:

#### Guild Roles
- **Owner**: Has full control over the guild with all guild permissions
- **Officer**: Has management permissions like inviting, kicking, and claiming territory
- **Member**: Has basic member permissions like using guild homes

#### How Role-Permission Integration Works
- Guild permissions and roles work together - both are checked when a player attempts an action
- Permission checks consider both the player's server permissions and their role within the guild
- Higher roles automatically include permissions from lower roles
- Server administrators can customize which permissions are granted to each guild role

#### Configuration Options
You can configure which permissions are granted to each guild role in the plugin's configuration file:
- Set which commands require Owner, Officer, or Member role
- Define custom permission requirements for specific guild actions
- Override default role requirements for specific guilds or players

## Admin Commands

These commands are available for server administrators:

### Advanced Territory Management
- `/guild claim unmanned <guild>` - Claim the chunk you're standing in for an unmanned guild
  - Example: `/guild claim unmanned Defenders`
  - Claims the chunk for the specified guild even if you're not a member
  - Requirements:
    - You must have the `furious.guild.claim.unmanned` permission

- `/guild claim SAFE` - Claim all chunks between your WorldEdit selection points for the SAFE guild
  - Example: `/guild claim SAFE`
  - Claims all chunks in the rectangular area defined by your WorldEdit selection
  - Requirements:
    - You must have the `furious.guild.claim.unmanned` permission
    - You must have a valid WorldEdit selection (use the WorldEdit wand to select two points)
    - Only works with the SAFE guild

- `/guild unclaim unmanned <guild>` - Unclaim the chunk you're standing in from an unmanned guild
  - Example: `/guild unclaim unmanned Defenders`
  - Unclaims the chunk from the specified guild even if you're not a member
  - Requirements:
    - You must have the `furious.guild.unclaim.unmanned` permission

### Guild System Management
- `/guild admin disband <guild>` - Forcibly disband a guild
  - Example: `/guild admin disband InactiveGuild`
  - Permanently removes the guild and all its claims
  - Requirements:
    - You must have the `furious.guild.admin.disband` permission

- `/guild admin transfer <guild> <player>` - Forcibly transfer guild ownership
  - Example: `/guild admin transfer Defenders NewOwner`
  - Transfers ownership of the specified guild to the specified player
  - Requirements:
    - You must have the `furious.guild.admin.transfer` permission

- `/guild admin homes <guild>` - Manage homes for a guild
  - Example: `/guild admin homes Defenders`
  - Allows administrative management of guild homes
  - Requirements:
    - You must have the `furious.guild.admin.homes` permission

- `/guild admin info <guild>` - View detailed information about a guild
  - Example: `/guild admin info Defenders`
  - Shows detailed administrative information about the specified guild
  - Requirements:
    - You must have the `furious.guild.admin.info` permission

## Guild System Configuration

The guild system can be configured in the plugin's configuration file. Some configurable options include:

- **Maximum Guild Size**: The maximum number of members allowed in a guild
- **Maximum Claims**: The maximum number of chunks a guild can claim
- **Maximum Guild Homes**: The maximum number of homes a guild can set
- **Claim Cost**: Whether claiming chunks costs money and how much
- **Guild Creation Cost**: Whether creating a guild costs money and how much
- **Mob Spawning Default**: The default setting for mob spawning in guild territories

Administrators can modify these settings to customize the guild system for their server.

## Examples

### Advanced Territory Management

1. Claim a chunk for an inactive guild:
   ```
   /guild claim unmanned InactiveGuild
   ```
   This claims the chunk you're standing in for the specified guild.

2. Claim multiple chunks using WorldEdit selection:
   ```
   /guild claim SAFE
   ```
   This claims all chunks in the rectangular area defined by your WorldEdit selection points for the SAFE guild. Make sure to select two points using the WorldEdit wand (wooden axe) before running this command.

3. Unclaim a chunk from a guild with no online members:
   ```
   /guild unclaim unmanned ProblemGuild
   ```
   This removes the claim on the chunk you're standing in from the specified guild.

### Guild System Management

1. Forcibly disband an inactive guild:
   ```
   /guild admin disband AbandonedGuild
   ```
   This permanently removes the guild and all its claims.

2. Transfer ownership of a guild to a new player:
   ```
   /guild admin transfer ProblemGuild NewOwner
   ```
   This transfers ownership of the guild to the specified player.

## Best Practices

1. **Granular Permission Management**: Take advantage of the new granular admin permissions to assign specific administrative capabilities to different staff roles:
   - Junior staff might receive only `furious.guild.admin.info` for monitoring
   - Senior moderators could get `furious.guild.admin.homes` and `furious.guild.admin.unclaim`
   - Only administrators should receive `furious.guild.admin.disband` and `furious.guild.admin.transfer`

2. **Permission Inheritance Setup**: Configure your permission system to take advantage of the new inheritance structure:
   - Set up permission groups that inherit from `furious.guild.*`, `furious.guild.admin.*`, or `furious.guild.officer.*`
   - Use the inheritance system to simplify permission management and ensure consistency
   - Remember that granting a wildcard permission like `furious.guild.admin.*` grants all related permissions

3. **Role-Permission Configuration**: Customize the role-permission integration to suit your server's needs:
   - Configure which commands require Owner, Officer, or Member role in the plugin's configuration
   - Create a balanced permission structure that respects guild hierarchy while allowing administrators to intervene when necessary
   - Consider creating custom permission configurations for special guilds or events

4. **Regular Audits**: Periodically review guilds for inactivity and consider disbanding guilds that have been inactive for extended periods.
   - Use the new `furious.guild.admin.info` permission to monitor guild activity
   - Set up automated systems to flag inactive guilds for review

5. **Territory Limits**: Configure appropriate claim limits to prevent guilds from claiming excessive territory.
   - Consider setting different claim limits based on guild size or activity
   - Use the granular admin permissions to manage territory disputes

6. **Guild Size Limits**: Set appropriate guild size limits to encourage multiple guilds rather than one massive guild.
   - Balance guild size limits with your server's population
   - Consider how guild size affects the role-permission system

7. **Cost Balance**: If using claim costs or guild creation costs, ensure they are balanced with your server's economy.
   - Adjust costs based on permission levels if appropriate
   - Consider how costs interact with the guild role system

8. **Conflict Resolution**: Establish procedures for handling disputes between guilds over territory or other issues.
   - Create clear guidelines for when administrators should intervene
   - Document which admin permissions should be used in different scenarios

9. **Documentation**: Maintain documentation of administrative actions taken regarding guilds for reference.
   - Log all uses of administrative guild permissions
   - Keep records of permission changes and their effects

10. **Player Communication**: Clearly communicate guild system rules, limitations, and the new permission structure to players.
    - Explain how the role-permission integration affects their guild experience
    - Update server documentation to reflect the new permission system

11. **Plugin Compatibility**: Ensure the guild system works well with other protection plugins you might be using.
    - Test interactions between guild permissions and other permission-based plugins
    - Resolve any conflicts between different permission systems

12. **Backup Before Changes**: Always back up your server before making significant changes to guild configuration or permissions.
    - Test permission changes in a controlled environment first
    - Have a rollback plan in case of unexpected issues