# Warps Permissions Analysis

## Current Permissions

| Command | Current Permission | Issue |
|---------|-------------------|-------|
| CreateSubCommand | furious.warps.create | None (follows lowercase pattern) |
| DeleteSubCommand | furious.warps.delete | None (follows lowercase pattern) |
| RelocateSubCommand | furious.warps.relocate | None (follows lowercase pattern) |
| RenameSubCommand | furious.warps.rename | None (follows lowercase pattern) |
| CostSubCommand | furious.warps.cost | Documentation refers to it as "furious.warps.setcost" |
| PasswdSubCommand | furious.warps.passwd | Documentation refers to it as "furious.warps.setpassword" |
| LinkSubCommand | furious.warps.link | None (follows lowercase pattern) |
| ListSubCommand | furious.warps.list | None (follows lowercase pattern) |
| WarpSubCommand | furious.warps.warp | Documentation also mentions "furious.warps.use" |
| VisibilitySubCommand | furious.warps.visibility | Updated from "furious.warps.admin" for consistency |
| Portal Removal | furious.warps.unlink | Implemented in WarpsManager.removePortalByPunch() |

## Permission Structure Analysis

The warps permission system follows a consistent pattern for most commands:

1. **Command-specific Permissions**: Most commands use the pattern `furious.warps.[command]` (e.g., `furious.warps.create`, `furious.warps.delete`)
2. **Player Permissions**: Basic permissions like `furious.warps.warp` and `furious.warps.list` are granted to all players by default
3. **Admin Permissions**: Administrative permissions like `furious.warps.create` and `furious.warps.delete` are restricted to operators by default
4. **Special Permissions**: The `furious.teleport.admin` permission allows bypassing teleport queue, effects, costs, and password requirements

This structure is now consistent with the bank and wallet permission systems, using specific permissions for each command rather than general admin permissions.

## Permission Categories

The warps permission system can be categorized as follows:

1. **Basic Player Permissions**:
   - `furious.warps.warp` - Allows teleporting to warps
   - `furious.warps.list` - Allows listing available warps

2. **Administrative Permissions**:
   - `furious.warps.create` - Allows creating warps
   - `furious.warps.delete` - Allows deleting warps
   - `furious.warps.relocate` - Allows relocating warps
   - `furious.warps.rename` - Allows renaming warps
   - `furious.warps.cost` - Allows setting warp costs
   - `furious.warps.passwd` - Allows setting warp passwords
   - `furious.warps.visibility` - Allows toggling warp visibility

3. **Portal Management Permissions**:
   - `furious.warps.link` - Allows creating warp portals
   - `furious.warps.unlink` - Allows removing warp portals

4. **Special Permissions**:
   - `furious.teleport.admin` - Allows bypassing teleport queue, effects, costs, and password requirements

## Implemented Permission Changes

The following changes have been implemented to improve the warps permission system:

| Command/Feature | Old Permission | New Permission | Reason |
|---------|-------------------|------------------------|--------|
| VisibilitySubCommand | furious.warps.admin | furious.warps.visibility | Match permission name to command name for consistency |
| Portal Removal | Not explicitly defined | furious.warps.unlink | Added explicit permission for removing portals |
| Documentation | furious.warps.setcost | furious.warps.cost | Updated documentation to match code |
| Documentation | furious.warps.setpassword | furious.warps.passwd | Updated documentation to match code |
| Documentation | furious.warps.use | furious.warps.warp | Clarified relationship between permissions |

## Commands with Correct Permissions

| Command/Feature | Current Permission | Notes |
|---------|-------------------|-------|
| CreateSubCommand | furious.warps.create | Follows lowercase pattern, restricted to operators |
| DeleteSubCommand | furious.warps.delete | Follows lowercase pattern, restricted to operators |
| RelocateSubCommand | furious.warps.relocate | Follows lowercase pattern, restricted to operators |
| RenameSubCommand | furious.warps.rename | Follows lowercase pattern, restricted to operators |
| CostSubCommand | furious.warps.cost | Follows lowercase pattern, restricted to operators |
| PasswdSubCommand | furious.warps.passwd | Follows lowercase pattern, restricted to operators |
| LinkSubCommand | furious.warps.link | Follows lowercase pattern, restricted to operators |
| ListSubCommand | furious.warps.list | Follows lowercase pattern, available to all players |
| WarpSubCommand | furious.warps.warp | Follows lowercase pattern, available to all players |
| VisibilitySubCommand | furious.warps.visibility | Follows command name pattern, restricted to operators |
| Portal Removal | furious.warps.unlink | Implemented in WarpsManager.removePortalByPunch(), restricted to operators |

## Implementation Status

The following implementation steps have been completed:

1. Updated VisibilitySubCommand to use the specific permission `furious.warps.visibility` instead of the general `furious.warps.admin` permission ✓
2. Aligned documentation with code by updating permission names in documentation: ✓
   - Changed `furious.warps.setcost` to `furious.warps.cost` in ADMIN_WARPS_GUIDE.md
   - Changed `furious.warps.setpassword` to `furious.warps.passwd` in ADMIN_WARPS_GUIDE.md
   - Clarified the relationship between `furious.warps.warp` and `furious.warps.use` in PLAYER_WARPS_GUIDE.md
3. Documented the `furious.teleport.admin` permission in plugin.yml and ADMIN_WARPS_GUIDE.md ✓
4. Added missing `furious.warps.unlink` permission to plugin.yml ✓
5. Updated all permission checks in code to use the correct permissions ✓
6. Ensured all permissions are properly documented in the plugin.yml file ✓

## Implemented Future Considerations

The following improvements have been implemented:

1. **Granular Permissions**: ✓
   - Added more granular permissions for operations on other players' warps:
     - `furious.warps.create.others` - For creating warps for other players
     - `furious.warps.delete.others` - For deleting other players' warps
     - `furious.warps.relocate.others` - For relocating other players' warps
     - `furious.warps.rename.others` - For renaming other players' warps
     - `furious.warps.cost.others` - For setting costs on other players' warps
     - `furious.warps.passwd.others` - For setting passwords on other players' warps

2. **Permission Inheritance**: ✓
   - Implemented a structured permission inheritance system:
     - `furious.warps.*` grants all warps permissions
     - `furious.warps.admin.*` grants all administrative warps permissions
     - Higher-level permissions automatically grant related lower-level permissions
     - For example, `furious.warps.admin.*` grants all `.others` permissions

3. **Default Permission Configuration**: ✓
   - Adjusted default permission assignments:
     - Basic permissions (`furious.warps.warp`, `furious.warps.list`) granted to all players
     - Creation and management permissions granted to trusted players
     - Administrative permissions granted only to server operators
     - Added configuration options for warp limits based on permission levels

4. **Permission Documentation**: ✓
   - Created comprehensive permission documentation in the ADMIN_WARPS_GUIDE.md file
   - Added detailed explanations of each permission and its intended use
   - Included examples of permission configurations for different server roles
   - Documented the relationship between warps permissions and teleport permissions

## Permission Configuration

Server administrators should ensure:

1. All permission plugins are updated with the correct permissions
2. The wildcard permission `furious.warps.*` includes all the specific warps permissions
3. Players and staff are assigned appropriate permissions based on their roles
4. The `furious.teleport.admin` permission is granted carefully as it bypasses several security features