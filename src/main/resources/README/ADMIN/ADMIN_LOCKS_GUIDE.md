# Admin Guide: LOCKS

This guide provides information on how to administer and configure the locks system.

## Table of Contents
- [Overview](#overview)
- [Administrative Permissions](#administrative-permissions)
- [Admin Commands](#admin-commands)
- [World Settings Management](#world-settings-management)
- [Configuration](#configuration)
- [Examples](#examples)
- [Best Practices](#best-practices)

## Overview

The locks system allows players to secure blocks against access by other players. As an administrator, you can manage world settings for locks and configure how the system works server-wide.

## Administrative Permissions

To administer the locks system, you need these permissions:

### World Management Permissions
- `furious.locks.world` - Allows managing locks world settings (default: op)

### Basic Lock Management Permissions
- `furious.locks.lock` - Allows creating lock items (default: true)
- `furious.locks.unlock` - Allows creating unlock items (default: true)
- `furious.locks.info` - Allows checking lock ownership (default: true)
- `furious.locks.key` - Allows creating key items (default: true)

### Granular Admin Permissions
- `furious.locks.lock.others` - Allows creating locks for other players (default: op)
- `furious.locks.unlock.others` - Allows unlocking other players' locks (default: op)
- `furious.locks.key.others` - Allows creating keys for other players' locks (default: op)
- `furious.locks.bypass` - Allows bypassing lock restrictions (default: op)

### Permission Inheritance
The server now uses a structured permission inheritance system:

- `furious.locks.*` - Grants all locks permissions
- `furious.locks.admin.*` - Grants all administrative locks permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you grant a player `furious.locks.admin.*`, they automatically have all administrative locks permissions like `furious.locks.lock.others`, `furious.locks.unlock.others`, etc.

### Economic Integration
The locks system integrates with the wallet permission system for economy features:

- Players with `furious.wallet.bypass.cost` can create locks without paying the cost
- Players with different permission levels may have different lock costs
- Lock costs can be configured based on permission groups

## Admin Commands

These commands are available for server administrators:

### World Settings Management

- `/locks world list` - Lists all worlds and their locks settings
  - Example: `/locks world list`
  - Shows which worlds have locks enabled or disabled
  - Requirements:
    - You must have the `furious.locks.world` permission

- `/locks world enable <world>` - Enables locks in a specific world
  - Example: `/locks world enable world_nether`
  - Allows players to create and use locks in the specified world
  - Requirements:
    - You must have the `furious.locks.world` permission

- `/locks world disable <world>` - Disables locks in a specific world
  - Example: `/locks world disable world_the_end`
  - Prevents players from creating new locks in the specified world
  - Existing locks may still function depending on configuration
  - Requirements:
    - You must have the `furious.locks.world` permission

## Configuration

The locks system can be configured in the plugin's configuration file. Some configurable options include:

- **Economy Integration**: Whether purchasing locks requires in-game currency
- **Lock Prices**: How much locks cost if economy is enabled
- **Allowed Block Types**: Which blocks can be locked
- **Default World Settings**: Whether locks are enabled by default in new worlds

Administrators can modify these settings to customize the locks system for their server.

## Examples

### Managing World Settings

1. List all worlds and their locks settings:
   ```
   /locks world list
   ```
   ```
   [Server] Locks settings:
   [Server] world: ENABLED
   [Server] world_nether: ENABLED
   [Server] world_the_end: DISABLED
   ```

2. Disable locks in the nether world:
   ```
   /locks world disable world_nether
   ```
   ```
   [Server] Locks disabled in world_nether.
   ```

3. Enable locks in the end world:
   ```
   /locks world enable world_the_end
   ```
   ```
   [Server] Locks enabled in world_the_end.
   ```

## Best Practices

1. **World Configuration**: Consider which worlds should have locks enabled. PvP or anarchy worlds might be better without locks.

2. **Economy Balance**: If using economy integration, set lock prices that balance accessibility with value.

3. **Player Education**: Ensure players understand how to use locks properly to avoid frustration.

4. **Block Type Restrictions**: Consider restricting locks to only certain block types to prevent abuse.

5. **Regular Audits**: Periodically check for abandoned or excessive locks that might need cleanup.

6. **Permission Management**: Carefully assign lock permissions to player groups based on your server's hierarchy.

7. **Conflict Resolution**: Establish procedures for handling disputes over locked blocks between players.

8. **Plugin Compatibility**: Ensure the locks system works well with other protection plugins you might be using.