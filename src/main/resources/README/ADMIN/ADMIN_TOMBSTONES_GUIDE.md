# Admin Guide: TOMBSTONES

This guide provides information on how to administer and configure the tombstone system.

## Table of Contents
- [Overview](#overview)
- [Administrative Permissions](#administrative-permissions)
- [Admin Commands](#admin-commands)
- [Configuration](#configuration)
- [Examples](#examples)
- [Best Practices](#best-practices)

## Overview

The tombstone system provides a secure way for players to recover their items after death. As an administrator, you can manage tombstones server-wide and configure how the system works.

## Administrative Permissions

To administer the tombstone system, you need these permissions:

### Basic Tombstone Management
- `furious.tombstones.purge` - Allows purging all tombstones (default: op)
- `furious.tombstones.locate` - Allows locating tombstones (default: op)
- `furious.tombstones.info` - Allows viewing detailed tombstone information (default: op)

### Granular Admin Permissions
- `furious.tombstones.purge.others` - Allows purging other players' tombstones (default: op)
- `furious.tombstones.access.others` - Allows accessing other players' tombstones (default: op)
- `furious.tombstones.extend.others` - Allows extending the expiration time of other players' tombstones (default: op)
- `furious.tombstones.teleport.others` - Allows teleporting to other players' tombstones (default: op)

### Legacy Permission
- `furious.tombstones.admin` - Allows all tombstone administration (default: op)

### Permission Inheritance
The server now uses a structured permission inheritance system:

- `furious.tombstones.*` - Grants all tombstone permissions
- `furious.tombstones.admin.*` - Grants all administrative tombstone permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you grant a player `furious.tombstones.admin.*`, they automatically have all administrative tombstone permissions like `furious.tombstones.purge.others`, `furious.tombstones.access.others`, etc.

### Player Experience Customization
Different permission levels can provide different tombstone experiences:

- Players with `furious.tombstones.extended` have longer tombstone expiration times
- Players with `furious.tombstones.secure` have tombstones that only they can access
- Players with `furious.tombstones.notify` receive notifications when their tombstones are about to expire

## Admin Commands

These commands are available for server administrators:

### Purging Tombstones
- `/tombstones purge` - Remove all tombstones from the server
  - Example: `/tombstones purge`
  - Removes all tombstones, including their armor stands and stored items
  - Useful for cleaning up the server or resolving issues with tombstones
  - Requirements:
    - You must have the `furious.tombstones.admin` permission

## Configuration

The tombstone system can be configured in the plugin's configuration file. Some configurable options include:

- **Expiration Time**: How long tombstones last before automatically disappearing
- **Safe Location Finding**: How the plugin finds a safe location for tombstones
- **Visual Appearance**: How tombstones appear in the world

Administrators can modify these settings to customize the tombstone system for their server.

## Examples

### Administrative Management

1. An administrator purges all tombstones:
   ```
   /tombstones purge
   ```
   ```
   [Server] Successfully purged 5 tombstones.
   ```

## Best Practices

1. **Regular Maintenance**: Consider periodically purging old tombstones to keep server performance optimal.

2. **Player Communication**: Inform players about the tombstone expiration time so they know how long they have to recover their items.

3. **Configuration Tuning**: Adjust the expiration time based on your server's needs - longer for casual servers, shorter for performance-focused servers.

4. **Administrative Purging**: Only use the purge command when necessary, as it permanently removes all tombstones and their contents.

5. **Backup Before Changes**: Always back up your server before making significant changes to the tombstone configuration.

6. **Monitor Performance**: If you have many players dying frequently, monitor server performance as too many tombstones could impact performance.

7. **World Restrictions**: Consider whether tombstones should be enabled in all worlds or restricted in certain areas.

8. **Plugin Compatibility**: Ensure the tombstone system works well with other death-related plugins you might be using.