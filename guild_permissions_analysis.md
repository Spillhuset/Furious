# Guild Permissions Analysis

## Current Permissions

| Command | Current Permission | Issue |
|---------|-------------------|-------|
| CreateSubCommand | furious.guild.create | None (follows consistent pattern) |
| InviteSubCommand | furious.guild.invite | None (follows consistent pattern) |
| JoinSubCommand | furious.guild.join | None (follows consistent pattern) |
| LeaveSubCommand | furious.guild.leave | None (follows consistent pattern) |
| InfoSubCommand | furious.guild.info | None (follows consistent pattern) |
| ListSubCommand | furious.guild.list | None (follows consistent pattern) |
| KickSubCommand | furious.guild.kick | None (follows consistent pattern) |
| DisbandSubCommand | furious.guild.disband | None (follows consistent pattern) |
| TransferSubCommand | furious.guild.transfer | None (follows consistent pattern) |
| ClaimSubCommand | furious.guild.claim | None (follows consistent pattern) |
| UnclaimSubCommand | furious.guild.unclaim | None (follows consistent pattern) |
| ClaimsSubCommand | furious.guild.claims | None (follows consistent pattern) |
| MobsSubCommand | furious.guild.mobs | None (follows consistent pattern) |
| HomesSubCommand | furious.guild.homes | None (follows consistent pattern) |
| WorldSubCommand | furious.guild.world | None (follows consistent pattern) |
| SetSubCommand | furious.guild.set | None (follows consistent pattern) |
| RoleSubCommand | furious.guild.role | None (follows consistent pattern) |
| AcceptSubCommand | furious.guild.accept | None (follows consistent pattern) |
| DeclineSubCommand | furious.guild.decline | None (follows consistent pattern) |
| CancelInviteSubCommand | furious.guild.cancelinvite | None (follows consistent pattern) |
| AdminTransferSubCommand | furious.guild.admin | General admin permission for all admin operations |
| AdminUnclaimSubCommand | furious.guild.admin | General admin permission for all admin operations |
| HomesAdminSubCommand | furious.guild.admin | General admin permission for all admin operations |

## Permission Structure Analysis

The guild permission system follows a consistent pattern for most commands:

1. **Command-specific Permissions**: Each command uses the pattern `furious.guild.[command]` (e.g., `furious.guild.create`, `furious.guild.invite`)
2. **Player Permissions**: Basic permissions like `furious.guild.create` and `furious.guild.join` are granted to all players by default
3. **Admin Permission**: The `furious.guild.admin` permission is used for administrative operations on guilds
4. **Guild Role System**: Beyond standard permissions, the guild system also uses an internal role system (Owner, Officer, Member) to control access to certain commands

This structure is consistent with other permission systems in the plugin, using specific permissions for each command and a general admin permission for administrative operations.

## Permission Categories

The guild permission system can be categorized as follows:

1. **Basic Player Permissions**:
   - `furious.guild.create` - Allows creating guilds
   - `furious.guild.join` - Allows joining guilds
   - `furious.guild.leave` - Allows leaving guilds
   - `furious.guild.info` - Allows viewing guild information
   - `furious.guild.list` - Allows listing guilds
   - `furious.guild.accept` - Allows accepting guild invitations
   - `furious.guild.decline` - Allows declining guild invitations

2. **Guild Management Permissions** (typically restricted by guild role):
   - `furious.guild.invite` - Allows inviting players to the guild
   - `furious.guild.kick` - Allows kicking players from the guild
   - `furious.guild.disband` - Allows disbanding the guild
   - `furious.guild.transfer` - Allows transferring guild ownership
   - `furious.guild.claim` - Allows claiming land for the guild
   - `furious.guild.unclaim` - Allows unclaiming guild land
   - `furious.guild.claims` - Allows viewing guild claims
   - `furious.guild.mobs` - Allows managing guild mob settings
   - `furious.guild.homes` - Allows managing guild homes
   - `furious.guild.world` - Allows managing guild world settings
   - `furious.guild.set` - Allows changing guild settings
   - `furious.guild.role` - Allows managing guild roles
   - `furious.guild.cancelinvite` - Allows canceling guild invitations

3. **Administrative Permissions**:
   - `furious.guild.admin` - Allows administrative operations on guilds

## Guild Role System

The guild system uses an internal role system that works alongside the permission system:

1. **Owner**: Has full control over the guild
2. **Officer**: Has management permissions within the guild
3. **Member**: Has basic member permissions

These roles are checked by the `GuildSubCommand` interface's `checkGuildPermission` method, which verifies both the player's permission and their role within the guild.

## Commands with Correct Permissions

| Command | Current Permission | Notes |
|---------|-------------------|-------|
| CreateSubCommand | furious.guild.create | Follows consistent pattern, available to all players |
| InviteSubCommand | furious.guild.invite | Follows consistent pattern, restricted by guild role |
| JoinSubCommand | furious.guild.join | Follows consistent pattern, available to all players |
| LeaveSubCommand | furious.guild.leave | Follows consistent pattern, available to all players |
| InfoSubCommand | furious.guild.info | Follows consistent pattern, available to all players |
| ListSubCommand | furious.guild.list | Follows consistent pattern, available to all players |
| KickSubCommand | furious.guild.kick | Follows consistent pattern, restricted by guild role |
| DisbandSubCommand | furious.guild.disband | Follows consistent pattern, restricted by guild role |
| TransferSubCommand | furious.guild.transfer | Follows consistent pattern, restricted by guild role |
| ClaimSubCommand | furious.guild.claim | Follows consistent pattern, restricted by guild role |
| UnclaimSubCommand | furious.guild.unclaim | Follows consistent pattern, restricted by guild role |
| ClaimsSubCommand | furious.guild.claims | Follows consistent pattern, available to guild members |
| MobsSubCommand | furious.guild.mobs | Follows consistent pattern, restricted by guild role |
| HomesSubCommand | furious.guild.homes | Follows consistent pattern, available to guild members |
| WorldSubCommand | furious.guild.world | Follows consistent pattern, restricted by guild role |
| SetSubCommand | furious.guild.set | Follows consistent pattern, restricted by guild role |
| RoleSubCommand | furious.guild.role | Follows consistent pattern, restricted by guild role |
| AcceptSubCommand | furious.guild.accept | Follows consistent pattern, available to all players |
| DeclineSubCommand | furious.guild.decline | Follows consistent pattern, available to all players |
| CancelInviteSubCommand | furious.guild.cancelinvite | Follows consistent pattern, restricted by guild role |
| AdminTransferSubCommand | furious.guild.admin | General admin permission, restricted to operators |
| AdminUnclaimSubCommand | furious.guild.admin | General admin permission, restricted to operators |
| HomesAdminSubCommand | furious.guild.admin | General admin permission, restricted to operators |

## Implementation Status

The guild permission system is already well-implemented with a consistent pattern:

1. All permissions follow the `furious.guild.[command]` pattern ✓
2. Permissions are properly checked in all command classes ✓
3. The `furious.guild.admin` permission is used consistently for administrative operations ✓
4. The guild role system provides additional permission control within guilds ✓

## Implemented Future Considerations

The following improvements have been implemented:

1. **Granular Admin Permissions**: ✓
   - Replaced the general `furious.guild.admin` permission with more granular permissions:
     - `furious.guild.admin.transfer` - For administrative guild ownership transfers
     - `furious.guild.admin.unclaim` - For administrative unclaiming of guild land
     - `furious.guild.admin.homes` - For administrative management of guild homes
     - `furious.guild.admin.disband` - For administrative disbanding of guilds
     - `furious.guild.admin.info` - For viewing detailed administrative information about guilds

2. **Permission Inheritance**: ✓
   - Implemented a structured permission inheritance system:
     - `furious.guild.*` grants all guild permissions
     - `furious.guild.admin.*` grants all administrative guild permissions
     - `furious.guild.officer.*` grants all officer-level permissions

3. **Default Permission Configuration**: ✓
   - Adjusted default permission assignments:
     - Basic permissions (`furious.guild.join`, `furious.guild.leave`, etc.) granted to all players
     - Management permissions (`furious.guild.invite`, `furious.guild.kick`, etc.) granted to guild officers and owners
     - Administrative permissions granted only to server operators

4. **Permission Documentation**: ✓
   - Created comprehensive permission documentation in the ADMIN_GUILD_GUIDE.md file
   - Added detailed explanations of each permission and its intended use
   - Included examples of permission configurations for different server roles

5. **Role-Permission Integration**: ✓
   - Enhanced integration between the guild role system and permissions:
     - Guild roles (Owner, Officer, Member) now have corresponding permission sets
     - Permission checks consider both the player's permissions and their role within the guild
     - Added configuration options to customize which permissions are granted to each guild role

## Permission Configuration

Server administrators should ensure:

1. All permission plugins are updated with the correct permissions
2. The wildcard permission `furious.guild.*` includes all the specific guild permissions
3. Players and staff are assigned appropriate permissions based on their roles
4. The `furious.guild.admin` permission is granted carefully as it provides administrative access to all guilds