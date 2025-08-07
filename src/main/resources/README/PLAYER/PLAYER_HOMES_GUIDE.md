# Player Guide: HOMES

This guide provides information on how to use the `/homes` command and its features as a player.

## Table of Contents
- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Permission Requirements](#permission-requirements)
- [Home Management](#home-management)
- [Teleportation](#teleportation)
- [Home Limits](#home-limits)
- [Examples](#examples)
- [Tips and Best Practices](#tips-and-best-practices)

## Overview

The homes system allows you to set personal teleport locations called "homes." You can create multiple homes, each with a unique name, and teleport to them at any time. This provides a convenient way to navigate between important locations in the game world.

## Basic Usage

- `/homes` - Teleport to your default home
- `/homes <n>` - Teleport to a specific home
- `/homes tp <n>` - Teleport to a specific home (alternative syntax)
- `/homes list` - List all your homes
- `/homes set <n>` - Set a new home at your current location
- `/homes move <n>` - Move an existing home to your current location
- `/homes rename <oldname> <newname>` - Rename a home
- `/homes delete <n>` - Delete a home
- `/homes buy` - Purchase additional home slots (if economy is enabled)

## Permission Requirements

To use the homes system, you need these permissions:

### Basic Homes Permissions
- `furious.homes.set` - Allows setting homes (default: true)
- `furious.homes.move` - Allows moving homes (default: true)
- `furious.homes.rename` - Allows renaming homes (default: true)
- `furious.homes.delete` - Allows deleting homes (default: true)
- `furious.homes.list` - Allows listing homes (default: true)
- `furious.homes.tp` - Allows teleporting to homes (default: true)
- `furious.homes.buy` - Allows purchasing additional home slots (default: true)

### Home Limit Permissions
- `furious.homes.limit.1` - Sets home limit to 1 (default: false)
- `furious.homes.limit.2` - Sets home limit to 2 (default: false)
- `furious.homes.limit.3` - Sets home limit to 3 (default: true)
- `furious.homes.limit.5` - Sets home limit to 5 (default: false)
- `furious.homes.limit.10` - Sets home limit to 10 (default: false)
- `furious.homes.limit.custom` - Custom home limit set by server administrators

### Permission Inheritance
The server now uses a structured permission inheritance system:
- `furious.homes.*` grants all homes permissions and unlimited homes
- Higher-level permissions automatically grant related lower-level permissions
- For example, if you have `furious.homes.*`, you automatically have all basic homes permissions

## Home Management

These commands allow you to create and manage your homes.

### Creating and Modifying Homes
- `/homes set <n>` - Set a new home at your current location
  - Example: `/homes set base`
  - If you don't specify a name, it will create a home named "default"

- `/homes move <n>` - Move an existing home to your current location
  - Example: `/homes move base`
  - This updates the location of an existing home

- `/homes rename <oldname> <newname>` - Rename a home
  - Example: `/homes rename base main_base`
  - This changes the name of an existing home

- `/homes delete <n>` - Delete a home
  - Example: `/homes delete base`
  - This permanently removes the home

### Listing Homes
- `/homes list` - List all your homes
  - Example: `/homes list`
  - Shows all your homes with their coordinates and world

## Teleportation

These commands allow you to teleport to your homes.

### Teleporting to Homes
- `/homes` - Teleport to your default home
  - Example: `/homes`
  - If you have a home named "default," this will teleport you there

- `/homes <n>` - Teleport to a specific home
  - Example: `/homes base`
  - This is a shorthand for the tp command

- `/homes tp <n>` - Teleport to a specific home
  - Example: `/homes tp base`
  - This will teleport you to the specified home

## Home Limits

Players have a limit on how many homes they can create, determined by their permissions.

### Default Limits
- Regular players: 3 homes (with `furious.homes.limit.3`)
- Players can have different limits based on their permissions (1, 2, 3, 5, 10)
- Players with `furious.homes.*` have unlimited homes

### Purchasing Additional Slots
- `/homes buy` - Purchase additional home slots
  - Example: `/homes buy`
  - This may require in-game currency depending on server configuration

## Examples

### Setting Up Your Homes

1. Set your default home:
   ```
   /homes set default
   ```

2. Set additional homes for different purposes:
   ```
   /homes set mine
   /homes set farm
   /homes set nether
   ```

3. List all your homes:
   ```
   /homes list
   ```

### Managing Your Homes

1. Rename a home to something more descriptive:
   ```
   /homes rename mine diamond_mine
   ```

2. Move a home to a new location:
   ```
   /homes move farm
   ```
   (Execute this command at the new location)

3. Delete a home you no longer need:
   ```
   /homes delete old_base
   ```

### Teleporting Between Homes

1. Teleport to your default home:
   ```
   /homes
   ```

2. Teleport to a specific home:
   ```
   /homes farm
   ```
   or
   ```
   /homes tp farm
   ```

## Tips and Best Practices

1. **Name Homes Descriptively**: Use clear, descriptive names for your homes to make them easy to remember (e.g., "diamond_mine" instead of "mine1").

2. **Set a Default Home**: Always set a home named "default" as your main base, so you can quickly return using just `/homes`.

3. **Organize by Purpose**: Create homes for different purposes - one for your main base, one for mining, one for farming, etc.

4. **Consider World Limitations**: Some servers may disable homes in certain worlds. Check with an administrator if you're unsure.

5. **Manage Your Limit**: If you have a home limit, delete homes you no longer use before creating new ones. Your home limit is now directly tied to your permission level.

6. **Strategic Placement**: Place homes in strategic locations - near valuable resources, at important builds, or in dangerous areas for quick escapes.

7. **Buy Additional Slots**: If you need more homes than your permission allows, consider purchasing additional slots with `/homes buy`. The cost and maximum purchasable slots are configured by server administrators.

8. **Use Tab Completion**: When teleporting to homes, use tab completion to quickly select from your available homes.

9. **Permission Awareness**: Your ability to set, move, rename, and delete homes depends on your permissions. If you're having trouble with a command, check with an administrator about your permission level.

10. **Custom Home Limits**: Server administrators can now set custom home limits for different player groups. Ask about special home limit permissions if you need more homes for specific projects.

11. **Home Integration**: The homes system now integrates with other systems like teleport and wallet, allowing for seamless teleportation and home slot purchases.