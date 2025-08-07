# Player Guide: GUILD

This guide provides information on how to use the `/guild` command and its features as a player.

## Table of Contents
- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Permission Requirements](#permission-requirements)
- [Guild Management](#guild-management)
- [Member Management](#member-management)
- [Territory Management](#territory-management)
- [Guild Homes](#guild-homes)
- [Guild Settings](#guild-settings)
- [Examples](#examples)
- [Tips and Best Practices](#tips-and-best-practices)

## Overview

The guild system allows you to create and join player-run communities called guilds. Guilds provide various benefits including territory protection, shared homes, and organized player groups. Guild members can claim land, set guild homes, and work together to build and protect their territory.

## Basic Usage

- `/guild` - Shows help information
- `/guild info [guild]` - View information about a guild
- `/guild list` - List all guilds

## Permission Requirements

To use the guild system, you need these permissions:

### Basic Guild Permissions
- `furious.guild.create` - Allows creating guilds (default: true)
- `furious.guild.info` - Allows viewing guild information (default: true)
- `furious.guild.list` - Allows listing all guilds (default: true)

### Member Management Permissions
- `furious.guild.invite` - Allows inviting players to guilds (default: true)
- `furious.guild.join` - Allows joining guilds (default: true)
- `furious.guild.leave` - Allows leaving guilds (default: true)
- `furious.guild.kick` - Allows kicking players from guilds (default: true)
- `furious.guild.transfer` - Allows transferring guild ownership (default: true)

### Territory Management Permissions
- `furious.guild.claim` - Allows claiming chunks for a guild (default: true)
- `furious.guild.unclaim` - Allows unclaiming chunks from a guild (default: true)
- `furious.guild.claims` - Allows viewing claimed chunks of a guild (default: true)
- `furious.guild.mobs` - Allows controlling mob spawning in guild claimed chunks (default: true)

### Guild Homes Permissions
- `furious.guild.homes` - Allows managing guild homes (default: true)
- `furious.guild.homes.set` - Allows setting guild homes (default: true)
- `furious.guild.homes.teleport` - Allows teleporting to guild homes (default: true)

### Permission Inheritance
The server now uses a structured permission inheritance system:
- `furious.guild.*` grants all guild permissions
- `furious.guild.officer.*` grants all officer-level permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you have `furious.guild.officer.*`, you automatically have permissions like `furious.guild.invite`, `furious.guild.kick`, etc.

## Guild Management

These commands allow you to create, manage, and disband guilds.

### Creating and Disbanding Guilds
- `/guild create <n>` - Create a new guild
  - Example: `/guild create Defenders`

- `/guild disband` - Disband your guild (owner only)
  - Example: `/guild disband`

### Guild Information
- `/guild info [guild]` - View information about a guild
  - Example: `/guild info` (your guild)
  - Example: `/guild info Defenders` (specific guild)

- `/guild list` - List all guilds
  - Example: `/guild list`

## Member Management

These commands allow you to manage guild membership.

### Inviting and Joining
- `/guild invite <player>` - Invite a player to your guild
  - Example: `/guild invite Steve`

- `/guild join <guild>` - Join a guild (requires invitation or open guild)
  - Example: `/guild join Defenders`

- `/guild leave` - Leave your current guild
  - Example: `/guild leave`

### Managing Members
- `/guild kick <player>` - Kick a player from your guild (owner/officer only)
  - Example: `/guild kick Steve`

- `/guild transfer <player>` - Transfer guild ownership to another player (owner only)
  - Example: `/guild transfer Alex`

- `/guild role <player> <role>` - Set a player's role in the guild (owner only)
  - Example: `/guild role Steve OFFICER`

### Invitation Management
- `/guild accept <guild>` - Accept an invitation to join a guild
  - Example: `/guild accept Defenders`

- `/guild decline <guild>` - Decline an invitation to join a guild
  - Example: `/guild decline Defenders`

- `/guild cancelinvite <player>` - Cancel a pending invitation (owner/officer only)
  - Example: `/guild cancelinvite Steve`

## Territory Management

These commands allow you to manage guild territory through chunk claiming.

### Claiming and Unclaiming
- `/guild claim` - Claim the chunk you're standing in for your guild
  - Example: `/guild claim`

- `/guild unclaim` - Unclaim the chunk you're standing in
  - Example: `/guild unclaim`

- `/guild claims` - View all chunks claimed by your guild
  - Example: `/guild claims`

### Territory Settings
- `/guild mobs` - Toggle mob spawning in guild territory
  - Example: `/guild mobs`

## Guild Homes

These commands allow you to manage guild homes, which are teleport locations for guild members.

### Managing Guild Homes
- `/guild homes` - List all guild homes
  - Example: `/guild homes`

- `/guild homes set <n>` - Set a guild home at your current location
  - Example: `/guild homes set base`

- `/guild homes tp <n>` - Teleport to a guild home
  - Example: `/guild homes tp base`

## Guild Settings

These commands allow you to configure guild settings.

### Guild Configuration
- `/guild set description <text>` - Set your guild's description
  - Example: `/guild set description A friendly community of builders`

- `/guild set open <true/false>` - Set whether your guild is open for anyone to join
  - Example: `/guild set open true`

## Examples

### Creating and Setting Up a New Guild

1. Create a new guild:
   ```
   /guild create Defenders
   ```

2. Set a description for your guild:
   ```
   /guild set description A guild focused on protecting the server from threats
   ```

3. Invite some players:
   ```
   /guild invite Steve
   /guild invite Alex
   ```

4. Claim territory for your guild:
   ```
   /guild claim
   ```
   (Stand in different chunks and repeat to claim more territory)

5. Set a guild home:
   ```
   /guild homes set base
   ```

### Managing Guild Membership

1. Promote a member to officer:
   ```
   /guild role Steve OFFICER
   ```

2. Transfer guild ownership (if you're leaving):
   ```
   /guild transfer Alex
   ```

3. Remove an inactive member:
   ```
   /guild kick InactivePlayer
   ```

### Territory Management

1. View all your guild's claims:
   ```
   /guild claims
   ```

2. Toggle mob spawning in your territory:
   ```
   /guild mobs
   ```

3. Unclaim a chunk you no longer need:
   ```
   /guild unclaim
   ```
   (Stand in the chunk you want to unclaim)

## Tips and Best Practices

1. **Plan Your Territory**: Before claiming chunks, plan your territory layout to avoid scattered claims that are difficult to defend.

2. **Set Multiple Homes**: Create different guild homes for different purposes (e.g., farm, mine, base) to make navigation easier.

3. **Assign Roles Carefully**: Only promote trusted members to officer roles, as they will have significant control over the guild. Guild roles now have corresponding permission sets:
   - **Owner**: Has full control over the guild with all guild permissions
   - **Officer**: Has management permissions like inviting, kicking, and claiming territory
   - **Member**: Has basic member permissions like using guild homes

4. **Understand Role Permissions**: Each guild role now has specific permissions associated with it:
   - Guild permissions and roles work together - both are checked when you attempt an action
   - Server administrators can customize which permissions are granted to each guild role
   - Higher roles automatically include permissions from lower roles

5. **Regular Maintenance**: Periodically review your guild's claimed chunks and remove claims in areas you no longer use.

6. **Guild Description**: Create a clear, descriptive guild description to attract like-minded players.

7. **Coordinate with Members**: Communicate with guild members before making significant changes to territory or settings.

8. **Use Guild Homes Strategically**: Place guild homes in secure, convenient locations that benefit all members.

9. **Consider Mob Spawning**: Toggle mob spawning based on your guild's needs - enable for mob farms, disable for safe building areas.

10. **Permission Awareness**: Be aware that your ability to perform guild actions depends on both your guild role and your server permissions. If you're having trouble with a command, check with an administrator about your permission level.