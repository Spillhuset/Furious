# Player Guide: LOCKS

This guide provides information on how to use the `/locks` command and its features as a player.

## Table of Contents
- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Permission Requirements](#permission-requirements)
- [Player Commands](#player-commands)
- [Lock Management](#lock-management)
- [Key Management](#key-management)
- [Examples](#examples)
- [Tips and Best Practices](#tips-and-best-practices)

## Overview

The locks system allows you to secure blocks (such as chests, doors, and other interactive blocks) against access by other players. You can create lock items, unlock items, and keys to manage access to your secured blocks. The system provides a way to protect your valuables and control who can access your builds.

## Basic Usage

- `/locks` - Shows help information
- `/locks lock` - Creates a lock item
- `/locks unlock` - Creates an unlock item
- `/locks info` - Checks lock ownership
- `/locks key` - Creates a key item
- `/locks buy` - Purchases lock items

## Permission Requirements

To use the `/locks` command and its subcommands, you need these permissions:

- `furious.locks.lock` - Allows creating lock items (default: true)
- `furious.locks.unlock` - Allows creating unlock items (default: true)
- `furious.locks.info` - Allows checking lock ownership (default: true)
- `furious.locks.key` - Allows creating key items (default: true)

## Player Commands

These commands are available for regular players:

### Lock Management

- `/locks lock` - Creates a lock item
  - Example: `/locks lock`
  - Usage: Right-click a block with the lock item to lock it

- `/locks unlock` - Creates an unlock item
  - Example: `/locks unlock`
  - Usage: Right-click a locked block with the unlock item to unlock it

- `/locks info` - Checks lock ownership
  - Example: `/locks info`
  - Usage: Right-click a locked block to see who owns the lock

### Purchasing Locks

- `/locks buy` - Purchases lock items (if economy is enabled)
  - Example: `/locks buy`
  - This may require in-game currency depending on server configuration

### Key Management

- `/locks key` - Creates a key item for your locks
  - Example: `/locks key`
  - Usage: Right-click a locked block with the key to temporarily access it without unlocking

## Examples

### Securing a Chest

1. Create a lock item:
   ```
   /locks lock
   ```

2. Right-click on a chest with the lock item to secure it.

3. Now only you can access the chest.

4. To check who owns a locked block:
   ```
   /locks info
   ```
   Then right-click on the locked block.

### Creating Keys for Friends

1. Create a key for your locks:
   ```
   /locks key
   ```

2. Give the key to a friend (drop it or use a trade plugin).

3. Your friend can now access your locked blocks with the key.

### Removing Locks

1. Create an unlock item:
   ```
   /locks unlock
   ```

2. Right-click on a locked block with the unlock item to remove the lock.

3. The block is now accessible to everyone.

## Tips and Best Practices

1. **Lock Important Containers**: Always lock chests, barrels, and other containers that hold valuable items.

2. **Secure Entry Points**: Lock doors, trapdoors, and gates to prevent unauthorized access to your builds.

3. **Share Keys Carefully**: Only give keys to players you trust, as they will have access to all your locked blocks.

4. **Use Info Command**: If you forget which blocks you've locked, use the info command to check.

5. **Unlock Before Breaking**: Always unlock blocks before breaking them, or you might lose the ability to access the lock.

6. **Organize Your Keys**: If you have keys from multiple players, use item frames or renamed chests to keep them organized.

7. **Lock Redstone Components**: Consider locking important redstone components like hoppers and dispensers to prevent tampering.

8. **Check World Settings**: Some worlds might have locks disabled by server administrators.