# Teleport Permissions Analysis

## Current Permissions

| Command | Current Permission | Issue |
|---------|-------------------|-------|
| RequestSubCommand | furious.teleport.request | None (follows consistent pattern) |
| AcceptSubCommand | furious.teleport.accept | None (follows consistent pattern) |
| DeclineSubCommand | furious.teleport.decline | None (follows consistent pattern) |
| ListSubCommand | furious.teleport.list | None (follows consistent pattern) |
| AbortSubCommand | furious.teleport.abort | None (follows consistent pattern) |
| DenySubCommand | furious.teleport.deny | None (follows consistent pattern) |
| WorldConfigSubCommand | furious.teleport.worldconfig | None (follows consistent pattern) |
| WorldsSubCommand | furious.teleport.worlds | None (follows consistent pattern) |
| CoordsSubCommand | furious.teleport.coords | None (follows consistent pattern) |
| WorldSpawnSubCommand | furious.teleport.worldspawn | None (follows consistent pattern) |
| SetWorldSpawnSubCommand | furious.teleport.setworldspawn | None (follows consistent pattern) |
| ForceToPlayerSubCommand | furious.teleport.force | None (follows consistent pattern) |
| TeleportCommand (bypass) | furious.teleport.admin | Used for bypassing teleport restrictions |

## Permission Structure Analysis

The teleport permission system follows a consistent pattern for all commands:

1. **Command-specific Permissions**: Each command uses the pattern `furious.teleport.[command]` (e.g., `furious.teleport.request`, `furious.teleport.accept`)
2. **Player Permissions**: Basic permissions like `furious.teleport.request` and `furious.teleport.accept` are granted to all players by default
3. **Admin Permissions**: Administrative permissions like `furious.teleport.force` and `furious.teleport.worldconfig` are restricted to operators by default
4. **Special Permissions**: The `furious.teleport.admin` permission allows bypassing teleport queue, effects, costs, and cooldowns

This structure is consistent with the bank, wallet, and warps permission systems, using specific permissions for each command.

## Permission Categories

The teleport permission system can be categorized as follows:

1. **Basic Player Permissions**:
   - `furious.teleport.request` - Allows requesting teleport to another player
   - `furious.teleport.accept` - Allows accepting teleport requests
   - `furious.teleport.decline` - Allows declining teleport requests
   - `furious.teleport.list` - Allows listing teleport requests
   - `furious.teleport.abort` - Allows aborting outgoing teleport requests
   - `furious.teleport.deny` - Allows toggling auto-deny of teleport requests

2. **Administrative Permissions**:
   - `furious.teleport.force` - Allows forcing teleport of players
   - `furious.teleport.worldconfig` - Allows configuring teleport settings for worlds
   - `furious.teleport.worlds` - Allows viewing world teleport settings
   - `furious.teleport.coords` - Allows teleporting to specific coordinates
   - `furious.teleport.worldspawn` - Allows teleporting to world spawn
   - `furious.teleport.setworldspawn` - Allows setting world spawn location

3. **Special Permissions**:
   - `furious.teleport.admin` - Allows bypassing teleport queue, effects, costs, and cooldowns

## Commands with Correct Permissions

| Command | Current Permission | Notes |
|---------|-------------------|-------|
| RequestSubCommand | furious.teleport.request | Follows consistent pattern, available to all players |
| AcceptSubCommand | furious.teleport.accept | Follows consistent pattern, available to all players |
| DeclineSubCommand | furious.teleport.decline | Follows consistent pattern, available to all players |
| ListSubCommand | furious.teleport.list | Follows consistent pattern, available to all players |
| AbortSubCommand | furious.teleport.abort | Follows consistent pattern, available to all players |
| DenySubCommand | furious.teleport.deny | Follows consistent pattern, available to all players |
| WorldConfigSubCommand | furious.teleport.worldconfig | Follows consistent pattern, restricted to operators |
| WorldsSubCommand | furious.teleport.worlds | Follows consistent pattern, restricted to operators |
| CoordsSubCommand | furious.teleport.coords | Follows consistent pattern, restricted to operators |
| WorldSpawnSubCommand | furious.teleport.worldspawn | Follows consistent pattern, restricted to operators |
| SetWorldSpawnSubCommand | furious.teleport.setworldspawn | Follows consistent pattern, restricted to operators |
| ForceToPlayerSubCommand | furious.teleport.force | Follows consistent pattern, restricted to operators |

## Implementation Status

The teleport permission system is already well-implemented with a consistent pattern:

1. All permissions follow the `furious.teleport.[command]` pattern ✓
2. Permissions are properly checked in all command classes ✓
3. The permission system provides a good balance between granularity and simplicity ✓

## Implemented Future Considerations

The following improvements have been implemented:

1. **Permission Inheritance**: ✓
   - Implemented a structured permission inheritance system:
     - `furious.teleport.*` grants all teleport permissions
     - `furious.teleport.admin.*` grants all administrative teleport permissions
     - Higher-level permissions automatically grant related lower-level permissions
     - For example, `furious.teleport.admin` grants bypass capabilities for all teleport restrictions

2. **Default Permission Configuration**: ✓
   - Adjusted default permission assignments:
     - Basic permissions (`furious.teleport.request`, `furious.teleport.accept`, etc.) granted to all players
     - World management permissions granted to trusted staff
     - Administrative permissions granted only to server operators
     - Added configuration options for teleport cooldowns and costs based on permission levels

3. **Permission Documentation**: ✓
   - Created comprehensive permission documentation in the ADMIN_TELEPORT_GUIDE.md file
   - Added detailed explanations of each permission and its intended use
   - Included examples of permission configurations for different server roles
   - Documented special permissions like `furious.teleport.admin` that bypass restrictions

4. **Integration with Other Systems**: ✓
   - Enhanced integration between teleport permissions and other teleportation systems:
     - Implemented unified teleport handling across homes, warps, and player teleports
     - Added permission-based integration with the warps system for seamless teleportation
     - Created consistent teleport restrictions (cooldowns, costs, etc.) across all teleport types
     - Added configuration options to customize how permissions affect teleport behavior across systems

## Permission Configuration

Server administrators should ensure:

1. All permission plugins are updated with the correct permissions
2. The wildcard permission `furious.teleport.*` includes all the specific teleport permissions
3. Players and staff are assigned appropriate permissions based on their roles
4. The `furious.teleport.admin` permission is granted carefully as it bypasses several security features