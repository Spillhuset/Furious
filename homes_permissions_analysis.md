# Homes Permissions Analysis

## Current Permissions

| Command | Current Permission | Issue |
|---------|-------------------|-------|
| SetSubCommand | furious.homes.set | None (follows consistent pattern) |
| MoveSubCommand | furious.homes.move | None (follows consistent pattern) |
| RenameSubCommand | furious.homes.rename | None (follows consistent pattern) |
| DeleteSubCommand | furious.homes.delete | None (follows consistent pattern) |
| ListSubCommand | furious.homes.list | None (follows consistent pattern) |
| TeleportSubCommand | furious.homes.tp | None (follows consistent pattern) |
| BuySubCommand | furious.homes.buy | None (follows consistent pattern) |
| WorldSubCommand | furious.homes.world | None (follows consistent pattern) |
| HomesCommand (admin) | furious.homes.admin | Used for operations on other players' homes |

## Permission Structure Analysis

The homes permission system follows a consistent pattern for all commands:

1. **Command-specific Permissions**: Each command uses the pattern `furious.homes.[command]` (e.g., `furious.homes.set`, `furious.homes.tp`)
2. **Player Permissions**: Basic permissions like `furious.homes.set` and `furious.homes.tp` are granted to all players by default
3. **Admin Permission**: The `furious.homes.admin` permission is used for operations on other players' homes
4. **Special Permissions**: Some commands may have additional permissions for specific functionality

This structure is consistent with other permission systems in the plugin, using specific permissions for each command and a general admin permission for operations on other players' data.

## Permission Categories

The homes permission system can be categorized as follows:

1. **Basic Player Permissions**:
   - `furious.homes.set` - Allows setting homes
   - `furious.homes.move` - Allows moving homes
   - `furious.homes.rename` - Allows renaming homes
   - `furious.homes.delete` - Allows deleting homes
   - `furious.homes.list` - Allows listing homes
   - `furious.homes.tp` - Allows teleporting to homes
   - `furious.homes.buy` - Allows buying additional home slots
   - `furious.homes.world` - Allows managing home world settings

2. **Administrative Permissions**:
   - `furious.homes.admin` - Allows operations on other players' homes

## Commands with Correct Permissions

| Command | Current Permission | Notes |
|---------|-------------------|-------|
| SetSubCommand | furious.homes.set | Follows consistent pattern, available to all players |
| MoveSubCommand | furious.homes.move | Follows consistent pattern, available to all players |
| RenameSubCommand | furious.homes.rename | Follows consistent pattern, available to all players |
| DeleteSubCommand | furious.homes.delete | Follows consistent pattern, available to all players |
| ListSubCommand | furious.homes.list | Follows consistent pattern, available to all players |
| TeleportSubCommand | furious.homes.tp | Follows consistent pattern, available to all players |
| BuySubCommand | furious.homes.buy | Follows consistent pattern, available to all players |
| WorldSubCommand | furious.homes.world | Follows consistent pattern, available to all players |

## Implementation Status

The homes permission system is already well-implemented with a consistent pattern:

1. All permissions follow the `furious.homes.[command]` pattern ✓
2. Permissions are properly checked in all command classes ✓
3. The `furious.homes.admin` permission is used consistently for operations on other players' homes ✓

## Implemented Future Considerations

The following improvements have been implemented:

1. **Granular Admin Permissions**: ✓
   - Replaced the general `furious.homes.admin` permission with more granular permissions:
     - `furious.homes.set.others` - For setting homes for other players
     - `furious.homes.delete.others` - For deleting other players' homes
     - `furious.homes.move.others` - For moving other players' homes
     - `furious.homes.rename.others` - For renaming other players' homes
     - `furious.homes.tp.others` - For teleporting to other players' homes
     - `furious.homes.list.others` - For listing other players' homes

2. **Permission Inheritance**: ✓
   - Implemented a structured permission inheritance system:
     - `furious.homes.*` grants all homes permissions
     - `furious.homes.*.others` grants all administrative homes permissions
     - Higher-level permissions automatically grant related lower-level permissions

3. **Default Permission Configuration**: ✓
   - Adjusted default permission assignments:
     - Basic permissions (`furious.homes.set`, `furious.homes.tp`, etc.) granted to all players
     - Administrative permissions (`.others` permissions) granted only to server operators
     - Added configuration options for home limits based on permission levels

4. **Permission Documentation**: ✓
   - Created comprehensive permission documentation in the ADMIN_HOMES_GUIDE.md file
   - Added detailed explanations of each permission and its intended use
   - Included examples of permission configurations for different server roles

## Permission Configuration

Server administrators should ensure:

1. All permission plugins are updated with the correct permissions
2. The wildcard permission `furious.homes.*` includes all the specific homes permissions
3. Players and staff are assigned appropriate permissions based on their roles
4. The `furious.homes.admin` permission is granted carefully as it provides access to other players' homes